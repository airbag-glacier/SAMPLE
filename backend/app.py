import os
import sqlalchemy
import base64
import tempfile
from sqlalchemy import text
from flask import Flask, request, jsonify, render_template, make_response
from fpdf import FPDF
from datetime import datetime

app = Flask(__name__)

# ==========================================
# 1. DATABASE CONFIGURATION
# ==========================================
db_user = os.environ.get("DB_USER", "root")
db_pass = os.environ.get("DB_PASS", "Gabagool#1970")
db_name = os.environ.get("DB_NAME", "detechstroke_db")
db_host = os.environ.get("DB_HOST", "127.0.0.1")
db_port = os.environ.get("DB_PORT", "3307")

db_pool = sqlalchemy.create_engine(
    f"mysql+pymysql://{db_user}:{db_pass}@{db_host}:{db_port}/{db_name}",
    pool_size=5,
    max_overflow=2,
    pool_timeout=30,
    pool_recycle=1800,
    pool_pre_ping=True
)

def safe_int(value, default=0):
    try:
        return int(float(value)) if value and value != 'N/A' else default
    except: return default

def safe_float(value, default=0.0):
    try:
        return float(value) if value and value != 'N/A' else default
    except: return default


# ==========================================
# 2. SYNC ENDPOINT (YOLOv10 + Logistic Regression)
# ==========================================

@app.route('/sync_to_cloud', methods=['POST'])
def sync_to_cloud():
    try:
        data = request.json
        user_name = data.get('user_name', 'App User')
        user_email = data.get('user_email', 'synced@user.local')
        profile = data.get('user_profile')
        latest_scan = data.get('latest_facial_scan')
        latest_risk = data.get('latest_risk_assessment')

        with db_pool.connect() as conn:
            # --- THE IDENTITY FIX: Find user by email instead of Android ID ---
            user_record = conn.execute(text("SELECT id FROM users WHERE email = :email"), {"email": user_email}).fetchone()

            if user_record:
                cloud_uid = user_record[0]
                # User exists, just update their name in case it changed
                conn.execute(text("UPDATE users SET name = :name WHERE id = :uid"), {"name": user_name, "uid": cloud_uid})
            else:
                # Brand new user! Generate a new unique Cloud ID safely
                max_id_record = conn.execute(text("SELECT MAX(id) FROM users")).fetchone()
                next_id = (max_id_record[0] or 0) + 1 if max_id_record else 1

                conn.execute(text("INSERT INTO users (id, name, email) VALUES (:uid, :name, :email)"),
                             {"uid": next_id, "name": user_name, "email": user_email})
                cloud_uid = next_id

            # --- USE the safe `cloud_uid` for ALL subsequent tables ---

            # 1. Sync Health Profile
            if profile:
                conn.execute(text("""
                    INSERT INTO user_profiles (
                        user_id, age, gender, hypertension, bmi, smoking_status,
                        cholesterol, hdl, ldl, triglycerides, fbs,
                        diabetes, stroke_history, cardiac_disease
                    )
                    VALUES (
                        :uid, :age, :gen, :hyp, :bmi, :smoke,
                        :chol, :hdl, :ldl, :tri, :fbs,
                        :diab, :stroke, :cardiac
                    )
                    ON DUPLICATE KEY UPDATE 
                    age=:age, hypertension=:hyp, bmi=:bmi, smoking_status=:smoke,
                    cholesterol=:chol, hdl=:hdl, ldl=:ldl, triglycerides=:tri, fbs=:fbs,
                    diabetes=:diab, stroke_history=:stroke, cardiac_disease=:cardiac
                """), {
                    "uid": cloud_uid, # <--- Uses the safe ID
                    "age": safe_int(profile.get("age")),
                    "gen": profile.get("sex", "Unknown"),
                    "hyp": 1 if profile.get("hypertension") == "Yes" else 0,
                    "bmi": safe_float(profile.get("bmi")),
                    "smoke": profile.get("smoker", "Unknown"),
                    "chol": safe_float(profile.get("cholesterol")),
                    "hdl": safe_float(profile.get("hdl")),
                    "ldl": safe_float(profile.get("ldl")),
                    "tri": safe_float(profile.get("tri")),
                    "fbs": safe_float(profile.get("fbs")),
                    "diab": 1 if profile.get("diabetes") == "Yes" else 0,
                    "stroke": 1 if profile.get("stroke_history") == "Yes" else 0,
                    "cardiac": 1 if profile.get("cardiac_disease") == "Yes" else 0
                })

            # 2. Sync Risk Results (With Duplicate Blocker)
            if latest_risk:
                risk_exists = conn.execute(text("SELECT 1 FROM risk_assessments WHERE user_id = :uid AND timestamp = :t"),
                                           {"uid": cloud_uid, "t": latest_risk.get("timestamp")}).fetchone()
                if not risk_exists:
                    conn.execute(text("INSERT INTO risk_assessments (user_id, lr_prediction, risk_level, timestamp) VALUES (:uid, :pred, :lvl, :t)"),
                                 {"uid": cloud_uid, "pred": latest_risk.get("lr_prediction"), "lvl": latest_risk.get("risk_level"), "t": latest_risk.get("timestamp")})

            # 3. Sync YOLOv10 Facial Scan results (With Duplicate Blocker)
            if latest_scan:
                scan_exists = conn.execute(text("SELECT 1 FROM facial_scans WHERE user_id = :uid AND timestamp = :t"),
                                           {"uid": cloud_uid, "t": latest_scan.get("timestamp")}).fetchone()
                if not scan_exists:
                    conn.execute(text("""
                        INSERT INTO facial_scans (user_id, asymmetric_detected, timestamp, scan_image)
                        VALUES (:uid, :detected, :time, :img)
                    """), {
                        "uid": cloud_uid,
                        "detected": 1 if latest_scan.get("detected") else 0,
                        "time": latest_scan.get("timestamp"),
                        "img": latest_scan.get("image_base64")
                    })

            conn.commit()
        return jsonify({"success": True})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500
# ==========================================
# 3. CLINICAL DASHBOARD & PDF REPORTING
# ==========================================

# Shared query to ensure consistency between Dashboard and PDF
LATEST_PATIENT_QUERY = text("""
    WITH RankedRisk AS (
        SELECT user_id, risk_level, 
               ROW_NUMBER() OVER(PARTITION BY user_id ORDER BY timestamp DESC) as rn
        FROM risk_assessments
    ),
    RankedScans AS (
        SELECT user_id, asymmetric_detected, 
               ROW_NUMBER() OVER(PARTITION BY user_id ORDER BY timestamp DESC) as rn
        FROM facial_scans
    )
    SELECT u.id, u.name, u.email, p.age, p.hypertension, p.bmi, 
           r.risk_level, f.asymmetric_detected
    FROM users u
    LEFT JOIN user_profiles p ON u.id = p.user_id
    LEFT JOIN RankedRisk r ON u.id = r.user_id AND r.rn = 1
    LEFT JOIN RankedScans f ON u.id = f.user_id AND f.rn = 1
""")

@app.route('/admin')
def admin_dashboard():
    try:
        with db_pool.connect() as conn:
            patients = conn.execute(LATEST_PATIENT_QUERY).fetchall()

        patient_list = [{
            "id": p[0], # WE ADDED THIS SO HTML CAN USE IT
            "name": p[1] or "Unknown",
            "email": p[2] or "N/A",
            "age": p[3] or "-",
            "hypertension": "Yes" if p[4] == 1 else "No",
            "bmi": p[5] or "-",
            "latest_clinical_risk": p[6] or "Pending",
            "latest_facial_droop": "Detected" if p[7] == 1 else "Normal"
        } for p in patients]

        return render_template('admin_dashboard.html', patients=patient_list, db_connected=True)
    except Exception as e: return f"Database Error: {str(e)}"

@app.route('/download_report')
def download_report():
    try:
        with db_pool.connect() as conn:
            data = conn.execute(LATEST_PATIENT_QUERY).fetchall()

        pdf = FPDF()
        pdf.add_page()

        # --- NEW: Added Timestamp Footer ---
        current_time = datetime.now().strftime("%B %d, %Y - %I:%M %p")
        pdf.set_font("helvetica", "I", 10)
        pdf.set_text_color(100, 100, 100) # Gray color
        pdf.cell(0, 10, f"Generated on: {current_time}", ln=True, align="R")
        pdf.set_text_color(0, 0, 0) # Reset to black
        # -----------------------------------

        pdf.set_font("helvetica", "B", 16)
        pdf.cell(0, 10, "DeTechStroke Clinical Master Report", ln=True, align="C")
        pdf.ln(10)

        # Table Header
        pdf.set_font("helvetica", "B", 10)
        pdf.cell(55, 10, "Patient Name", 1)
        pdf.cell(20, 10, "Age", 1)
        pdf.cell(20, 10, "BMI", 1)
        pdf.cell(45, 10, "Stroke Risk (LR)", 1)
        pdf.cell(45, 10, "Facial Droop (YOLO)", 1)
        pdf.ln()

        # Table Content
        pdf.set_font("helvetica", "", 10)
        for row in data:
            # row[0] is u.id
            pdf.cell(55, 10, str(row[1] or "Unknown"), 1)  # row[1] is u.name
            pdf.cell(20, 10, str(row[3] or "-"), 1)        # row[3] is p.age
            pdf.cell(20, 10, str(row[5] or "-"), 1)        # row[5] is p.bmi
            pdf.cell(45, 10, str(row[6] or "Pending"), 1)  # row[6] is r.risk_level

            # row[7] is f.asymmetric_detected
            pdf.cell(45, 10, "⚠️ Detected" if row[7] == 1 else "Normal", 1)
            pdf.ln()


        pdf_bytes = bytes(pdf.output())

        response = make_response(pdf_bytes)
        response.headers['Content-Type'] = 'application/pdf'
        response.headers['Content-Disposition'] = 'attachment; filename=DeTechStroke_Report.pdf'
        return response

    except Exception as e:
        return f"PDF Generation Error: {str(e)}"

@app.route('/download_patient/<int:user_id>')
def download_patient_report(user_id):
    try:
        from datetime import datetime
        with db_pool.connect() as conn:
            # Fetch User & Profile
            profile = conn.execute(text("""
                SELECT u.name, u.email, p.age, p.gender, p.bmi, p.smoking_status,
                       p.hypertension, p.diabetes, p.stroke_history, p.cardiac_disease,
                       p.cholesterol, p.hdl, p.ldl, p.triglycerides, p.fbs
                FROM users u LEFT JOIN user_profiles p ON u.id = p.user_id WHERE u.id = :uid
            """), {"uid": user_id}).fetchone()

            if not profile:
                return "Patient not found", 404

            # Fetch AI Scan History (Up to 5 latest)
            risks = conn.execute(text("SELECT lr_prediction, risk_level, timestamp FROM risk_assessments WHERE user_id = :uid ORDER BY timestamp DESC LIMIT 5"), {"uid": user_id}).fetchall()
            scans = conn.execute(text("""
                SELECT asymmetric_detected, timestamp, scan_image 
                FROM facial_scans WHERE user_id = :uid ORDER BY timestamp DESC LIMIT 5
            """), {"uid": user_id}).fetchall()

        # Build the PDF
        pdf = FPDF()
        pdf.add_page()

        # Header
        pdf.set_font("helvetica", "B", 18)
        pdf.cell(0, 10, "DeTechStroke - Individual Patient Record", ln=True, align="C")
        pdf.set_font("helvetica", "I", 10)
        pdf.set_text_color(100, 100, 100)
        pdf.cell(0, 10, f"Generated on: {datetime.now().strftime('%B %d, %Y - %I:%M %p')}", ln=True, align="C")
        pdf.set_text_color(0, 0, 0)
        pdf.ln(5)

        # ---------------------------------------------------------
        # Section 1: Demographics & Risk Factors (Fixed Layout)
        # ---------------------------------------------------------
        pdf.set_font("helvetica", "B", 12)
        pdf.cell(0, 10, "1. Patient Demographics & Core Risk Factors", ln=True)
        pdf.set_font("helvetica", "", 10)

        # Left Column (Demographics) | Right Column (Risks)
        pdf.cell(90, 6, f"Name: {profile[0] or 'Unknown'}", 0, 0)
        pdf.cell(90, 6, f"Hypertension: {'Yes' if profile[6] == 1 else 'No'}", 0, 1)

        pdf.cell(90, 6, f"Email: {profile[1] or 'N/A'}", 0, 0)
        pdf.cell(90, 6, f"Diabetes: {'Yes' if profile[7] == 1 else 'No'}", 0, 1)

        pdf.cell(90, 6, f"Age: {profile[2] or '-'} | Gender: {profile[3] or '-'}", 0, 0)
        pdf.cell(90, 6, f"Stroke History: {'Yes' if profile[8] == 1 else 'No'}", 0, 1)

        pdf.cell(90, 6, f"BMI: {profile[4] or '-'} | Smoker: {profile[5] or 'Unknown'}", 0, 0)
        pdf.cell(90, 6, f"Cardiac Disease: {'Yes' if profile[9] == 1 else 'No'}", 0, 1)
        pdf.ln(5)

        # ---------------------------------------------------------
        # Section 2: Blood Chemistry Panel
        # ---------------------------------------------------------
        pdf.set_font("helvetica", "B", 12)
        pdf.cell(0, 10, "2. Blood Chemistry Panel", ln=True)
        pdf.set_font("helvetica", "", 10)

        pdf.cell(60, 8, f"Total Chol: {profile[10] or '-'} mg/dL", 1, 0)
        pdf.cell(60, 8, f"HDL: {profile[11] or '-'} mg/dL", 1, 0)
        pdf.cell(60, 8, f"LDL: {profile[12] or '-'} mg/dL", 1, 1)
        pdf.cell(60, 8, f"Triglycerides: {profile[13] or '-'} mg/dL", 1, 0)
        pdf.cell(60, 8, f"Fasting BS: {profile[14] or '-'} mg/dL", 1, 1)
        pdf.ln(8)

        # ---------------------------------------------------------
        # Section 3: Logistic Regression Risk History
        # ---------------------------------------------------------
        pdf.set_font("helvetica", "B", 12)
        pdf.cell(0, 10, "3. Clinical Risk Assessment History (Logistic Regression)", ln=True)
        pdf.set_font("helvetica", "B", 10)
        pdf.cell(60, 8, "Timestamp", 1)
        pdf.cell(60, 8, "Risk Level", 1)
        pdf.cell(60, 8, "Probability Score", 1)
        pdf.ln()

        pdf.set_font("helvetica", "", 10)
        for r in risks:
            pdf.cell(60, 8, str(r[2]), 1)
            pdf.cell(60, 8, str(r[1]), 1)
            pdf.cell(60, 8, f"{float(r[0]):.2f}%" if r[0] else "N/A", 1)
            pdf.ln()
        if not risks:
            pdf.cell(0, 8, "No risk assessments found.", ln=True)
        pdf.ln(8)

        # ---------------------------------------------------------
        # Section 4: YOLOv10 Facial Scan History (Deleted the duplicate)
        # ---------------------------------------------------------
        pdf.set_font("helvetica", "B", 12)
        pdf.cell(0, 10, "4. Facial Droop Scan History (YOLOv10)", ln=True)
        pdf.set_font("helvetica", "B", 10)

        pdf.cell(50, 8, "Timestamp", 1)
        pdf.cell(50, 8, "Asymmetry Detected", 1)
        pdf.cell(80, 8, "Scan Evidence", 1)
        pdf.ln()

        pdf.set_font("helvetica", "", 10)
        for s in scans:
            row_height = 45

            # --- NEW FIX: Force a page break if the image will get cut off ---
            if pdf.get_y() + row_height > 270:
                pdf.add_page()

            x_start = pdf.get_x()
            y_start = pdf.get_y()

            pdf.cell(50, row_height, str(s[1]), 1)
            pdf.cell(50, row_height, "⚠️ Detected" if s[0] == 1 else "Normal", 1)
            pdf.cell(80, row_height, "", 1) # Empty cell for the image border

            if s[2]: # If scan_image exists
                try:
                    with tempfile.NamedTemporaryFile(delete=False, suffix=".jpg") as temp_img:
                        temp_img.write(base64.b64decode(s[2]))
                        temp_img_path = temp_img.name

                    pdf.image(temp_img_path, x=x_start + 102, y=y_start + 2, w=40)
                    os.remove(temp_img_path)
                except Exception as e:
                    pdf.text(x_start + 105, y_start + 25, "Image Error")
            else:
                # NEW FIX: Adjusted the Y coordinate so the text sits perfectly in the middle of the box
                pdf.text(x_start + 125, y_start + 25, "No Image")

            pdf.ln(row_height)

        if not scans:
            pdf.cell(0, 8, "No facial scans found.", ln=True)

        # Return the final PDF
        response = make_response(bytes(pdf.output()))
        response.headers['Content-Type'] = 'application/pdf'
        formatted_name = str(profile[0]).replace(" ", "_") if profile[0] else "Patient"
        response.headers['Content-Disposition'] = f'attachment; filename=DeTechStroke_Record_{formatted_name}.pdf'
        return response

    except Exception as e:
        return f"Patient PDF Error: {str(e)}"

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)