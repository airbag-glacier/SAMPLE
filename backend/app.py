import os
import sqlalchemy
import base64
import tempfile
from sqlalchemy import text
from flask import Flask, request, jsonify, render_template, make_response, redirect, session
from fpdf import FPDF
from datetime import datetime

app = Flask(__name__)

app.secret_key = "super_secret_thesis_key_2026"
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

# ==========================================
# 1.5 INITIALIZE DOCTOR ADMIN ACCOUNTS
# ==========================================
def init_doctor_accounts():
    with db_pool.begin() as conn:

        try:
            conn.execute(text("ALTER TABLE extended_metrics ADD COLUMN updated_at VARCHAR(100)"))
        except Exception:
            pass
        # Create table for doctor logins
        conn.execute(text("""
            CREATE TABLE IF NOT EXISTS doctor_admins (
                id INT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(50) UNIQUE,
                password VARCHAR(100),
                doctor_name VARCHAR(100) UNIQUE,
                is_superadmin BOOLEAN DEFAULT 0
            )
        """))

        # 1. Seed the Super Admin (Dr. Willie Ong / Main Hospital Admin)
        conn.execute(text("""
            INSERT IGNORE INTO doctor_admins (username, password, doctor_name, is_superadmin) 
            VALUES ('admin', 'thesis2026', 'System Admin', 1)
        """))

        # 2. Seed Predefined Doctors (Force update if they already exist as 'Pending')
        predefined_docs = [
            ("dr.willie", "Dr. Willie Ong (Cardiology)"),
            ("dr.juan", "Dr. Juan Dela Cruz (General Practice)"),
            ("dr.rommel", "Dr. Rommel Hernandez"),
            ("dr.fe", "Dr. Fe Del Mundo"),
            ("dr.jose", "Dr. Jose Rizal")
        ]

        for username, doc_name in predefined_docs:
            conn.execute(text("""
                INSERT INTO doctor_admins (username, password, doctor_name, is_superadmin) 
                VALUES (:u, 'password123', :d, 0)
                ON DUPLICATE KEY UPDATE 
                username = :u, 
                password = 'password123'
            """), {"u": username, "d": doc_name})

# Call it immediately when the server starts
init_doctor_accounts()

def safe_int(value, default=0):
    try:
        return int(float(value)) if value and value != 'N/A' else default
    except:
        return default

def safe_float(value, default=0.0):
    try:
        return float(value) if value and value != 'N/A' else default
    except:
        return default

# ==========================================
# LOGIN ENDPOINT (THESIS DEFENSE GOD-MODE)
# ==========================================
# ==========================================
# STRICT LOGIN ENDPOINT
# ==========================================
@app.route('/login_and_sync', methods=['POST'])
def login_user():
    try:
        data = request.json
        email = data.get('email')
        password = data.get('passwordHash')

        # Use db_pool.begin() for strict transaction safety
        with db_pool.begin() as conn:
            user = conn.execute(text("SELECT id, password FROM users WHERE email = :email"),
                                {"email": email}).fetchone()

            if user:
                cloud_password = user[1]

                # Accepts normal password, the master thesis password, or a newly set password
                if cloud_password == password or password == "thesis2026" or cloud_password == "Not Set":
                    if cloud_password == "Not Set":
                        conn.execute(text("UPDATE users SET password = :pwd WHERE id = :uid"),
                                     {"pwd": password, "uid": user[0]})
                    return jsonify({"success": True, "userData": None, "message": "Login successful"})
                else:
                    # Correct email, wrong password
                    return jsonify({"success": False, "message": "Incorrect password"})
            else:
                # STRICT MODE: The email is not in the database. Reject the login!
                return jsonify({"success": False, "message": "Account not found. Please check your credentials."})

    except Exception as e:
        return jsonify({"success": False, "message": f"Server Error: {str(e)}"}), 500
# ==========================================
# 2. SYNC ENDPOINT (YOLOv10 + Logistic Regression)
# ==========================================
@app.route('/sync_to_cloud', methods=['POST'])
def sync_to_cloud():
    try:
        data = request.json
        user_name = data.get('user_name', 'Unknown User')
        user_email = data.get('user_email', 'No Email')
        user_password = data.get('user_password', 'Not Set')
        profile = data.get('user_profile')
        latest_scan = data.get('latest_facial_scan')
        latest_risk = data.get('latest_risk_assessment')
        appointments = data.get('appointments', [])

        # Debugging: Watch this in your PyCharm terminal!
        print(f"\n--- [CLOUD SYNC] RECEIVED PAYLOAD ---")
        print(f"Email: {user_email} | Real Name Sent: {user_name}")

        # changed to .begin() for strict transaction commits
        with db_pool.begin() as conn:
            # 1. Update or Insert User
            user_record = conn.execute(text("SELECT id FROM users WHERE email = :email"), {"email": user_email}).fetchone()

            if user_record:
                cloud_uid = user_record[0]
                print(f"User {cloud_uid} found. Overwriting 'New Patient' with '{user_name}'...")
                conn.execute(text("UPDATE users SET name = :name, password = :pwd WHERE id = :uid"),
                             {"name": user_name, "pwd": user_password, "uid": cloud_uid})
            else:
                max_id_record = conn.execute(text("SELECT MAX(id) FROM users")).fetchone()
                next_id = (max_id_record[0] or 0) + 1 if max_id_record else 1
                print(f"Creating new user {next_id} -> {user_name}")
                conn.execute(text("INSERT INTO users (id, name, email, password) VALUES (:uid, :name, :email, :pwd)"),
                             {"uid": next_id, "name": user_name, "email": user_email, "pwd": user_password})
                cloud_uid = next_id

            # 2. Sync Profile
            if profile:
                conn.execute(text("""
                    INSERT INTO user_profiles (
                        user_id, age, gender, hypertension, bmi, smoking_status,
                        hdl, ldl, triglycerides, diabetes, stroke_history, cardiac_disease
                    )
                    VALUES (
                        :uid, :age, :gen, :hyp, :bmi, :smoke,
                        :hdl, :ldl, :tri, :diab, :stroke, :cardiac
                    )
                    ON DUPLICATE KEY UPDATE 
                    age=:age, hypertension=:hyp, bmi=:bmi, smoking_status=:smoke,
                    hdl=:hdl, ldl=:ldl, triglycerides=:tri, diabetes=:diab, 
                    stroke_history=:stroke, cardiac_disease=:cardiac
                """), {
                    "uid": cloud_uid,
                    "age": safe_int(profile.get("age")),
                    "gen": profile.get("sex", "Unknown"),
                    "hyp": 1 if profile.get("hypertension") == "Yes" else 0,
                    "bmi": safe_float(profile.get("bmi")),
                    "smoke": profile.get("smoker", "Unknown"),
                    "hdl": safe_float(profile.get("hdl")),
                    "ldl": safe_float(profile.get("ldl")),
                    "tri": safe_float(profile.get("tri")),
                    "diab": 1 if profile.get("diabetes") == "Yes" else 0,
                    "stroke": 1 if profile.get("stroke_history") == "Yes" else 0,
                    "cardiac": 1 if profile.get("cardiac_disease") == "Yes" else 0
                })

                # Safe creation of extended metrics
                conn.execute(text("""
                    CREATE TABLE IF NOT EXISTS extended_metrics (
                        user_id INT PRIMARY KEY,
                        height FLOAT,
                        weight FLOAT,
                        cholesterol FLOAT,
                        fbs FLOAT,
                        updated_at VARCHAR(100)
                    )
                """))

                # Syncing Blood Chem & Vitals (with Date Timestamp!)
                conn.execute(text("""
                    INSERT INTO extended_metrics (user_id, height, weight, cholesterol, fbs, updated_at)
                    VALUES (:uid, :h, :w, :chol, :fbs, :upd)
                    ON DUPLICATE KEY UPDATE height=:h, weight=:w, cholesterol=:chol, fbs=:fbs, updated_at=:upd
                """), {
                    "uid": cloud_uid,
                    "h": safe_float(profile.get("height")),
                    "w": safe_float(profile.get("weight")),
                    "chol": safe_float(profile.get("cholesterol")),
                    "fbs": safe_float(profile.get("fbs")),
                    "upd": profile.get("updated_at", "Not Recorded")
                })

            # 3. Sync Risk Results
            if latest_risk:
                risk_exists = conn.execute(text("SELECT 1 FROM risk_assessments WHERE user_id = :uid AND timestamp = :t"),
                                           {"uid": cloud_uid, "t": latest_risk.get("timestamp")}).fetchone()
                if not risk_exists:
                    conn.execute(text("INSERT INTO risk_assessments (user_id, lr_prediction, risk_level, timestamp) VALUES (:uid, :pred, :lvl, :t)"),
                                 {"uid": cloud_uid, "pred": latest_risk.get("lr_prediction"), "lvl": latest_risk.get("risk_level"), "t": latest_risk.get("timestamp")})

            # 4. Sync YOLOv10 Facial Scan
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

            # 5. Sync Appointments & Catch New Doctors
            for apt in appointments:
                doc_name = apt.get("doctor_name")
                doc_exists = conn.execute(text("SELECT 1 FROM doctor_admins WHERE doctor_name = :d"), {"d": doc_name}).fetchone()
                if not doc_exists:
                    conn.execute(text("""
                        INSERT INTO doctor_admins (username, password, doctor_name, is_superadmin) 
                        VALUES (NULL, NULL, :d, 0)
                    """), {"d": doc_name})

                apt_exists = conn.execute(text("""
                    SELECT 1 FROM appointments WHERE user_id = :uid AND apt_date = :d AND apt_time = :t
                """), {"uid": cloud_uid, "d": apt.get("apt_date"), "t": apt.get("apt_time")}).fetchone()

                if not apt_exists:
                    conn.execute(text("""
                        INSERT INTO appointments (user_id, doctor_name, apt_date, apt_time) 
                        VALUES (:uid, :doc, :d, :t)
                    """), {"uid": cloud_uid, "doc": doc_name, "d": apt.get("apt_date"), "t": apt.get("apt_time")})

        print("--- SYNC SUCCESSFUL ---\n")
        return jsonify({"success": True})

    except Exception as e:
        print(f"!!! SYNC CRASHED: {str(e)} !!!\n")
        return jsonify({"success": False, "error": str(e)}), 500

# ==========================================
# 3. CLINICAL DASHBOARD & PDF REPORTING
# ==========================================

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
    ),
    RankedAppointments AS (
        SELECT user_id, doctor_name, apt_date, apt_time,
               ROW_NUMBER() OVER(PARTITION BY user_id ORDER BY id DESC) as rn
        FROM appointments
    )
    SELECT u.id, u.name, u.email, u.password, p.age, p.hypertension, p.bmi, 
           r.risk_level, f.asymmetric_detected,
           a.apt_date, a.apt_time, a.doctor_name
    FROM users u
    LEFT JOIN user_profiles p ON u.id = p.user_id
    LEFT JOIN RankedRisk r ON u.id = r.user_id AND r.rn = 1
    LEFT JOIN RankedScans f ON u.id = f.user_id AND f.rn = 1
    LEFT JOIN RankedAppointments a ON u.id = a.user_id AND a.rn = 1
""")

@app.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        username = request.form.get('username')
        password = request.form.get('password')

        with db_pool.connect() as conn:
            admin_user = conn.execute(text("""
                SELECT doctor_name FROM doctor_admins 
                WHERE username = :u AND password = :p
            """), {"u": username, "p": password}).fetchone()

            if admin_user:

                session['username'] = username
                return redirect('/admin')
            else:
                return render_template('login.html', error="Invalid Credentials")

    # If it's a GET request, just show the login page
    return render_template('login.html', error=None)

@app.route('/logout')
def logout():
    # Instantly destroys the session cookie
    session.pop('username', None)
    return redirect('/login')

@app.route('/admin')
def admin_dashboard():
    # 1. SESSION CHECK (Replaces the old 'auth' check)
    if 'username' not in session:
        return redirect('/login')

    username = session['username']

    try:
        with db_pool.begin() as conn:
            # 2. VERIFY CREDENTIALS VIA SESSION
            admin_user = conn.execute(text("""
                SELECT doctor_name, is_superadmin FROM doctor_admins 
                WHERE username = :u
            """), {"u": username}).fetchone()

            if not admin_user:
                session.pop('username', None)
                return redirect('/login')

            logged_in_doctor_name = admin_user[0]
            is_superadmin = int(admin_user[1])

            # 3. CLEAR OLD APPOINTMENTS
            from datetime import datetime
            apts = conn.execute(text("SELECT id, apt_date, apt_time FROM appointments")).fetchall()
            now = datetime.now()
            for apt in apts:
                try:
                    apt_dt_str = f"{apt[1]} {apt[2]}"
                    apt_dt = datetime.strptime(apt_dt_str, "%m/%d/%Y %I:%M %p")
                    if apt_dt < now:
                        conn.execute(text("DELETE FROM appointments WHERE id = :id"), {"id": apt[0]})
                except Exception:
                    pass

            # 4. DEFINE THE BASE SQL STRING
            base_query = """
                WITH RankedRisk AS (
                    SELECT user_id, risk_level, 
                           ROW_NUMBER() OVER(PARTITION BY user_id ORDER BY timestamp DESC) as rn
                    FROM risk_assessments
                ),
                RankedScans AS (
                    SELECT user_id, asymmetric_detected, 
                           ROW_NUMBER() OVER(PARTITION BY user_id ORDER BY timestamp DESC) as rn
                    FROM facial_scans
                ),
                RankedAppointments AS (
                    SELECT user_id, doctor_name, apt_date, apt_time,
                           ROW_NUMBER() OVER(PARTITION BY user_id ORDER BY id DESC) as rn
                    FROM appointments
                )
                SELECT u.id, u.name, u.email, u.password, p.age, p.hypertension, p.bmi, 
                       r.risk_level, f.asymmetric_detected,
                       a.apt_date, a.apt_time, a.doctor_name
                FROM users u
                LEFT JOIN user_profiles p ON u.id = p.user_id
                LEFT JOIN RankedRisk r ON u.id = r.user_id AND r.rn = 1
                LEFT JOIN RankedScans f ON u.id = f.user_id AND f.rn = 1
                LEFT JOIN RankedAppointments a ON u.id = a.user_id AND a.rn = 1
            """

            # 5. STRICT FILTERING LOGIC
            if is_superadmin == 1:
                patients = conn.execute(text(base_query)).fetchall()
                pending_docs = conn.execute(text("SELECT id, doctor_name FROM doctor_admins WHERE username IS NULL")).fetchall()
            else:
                filtered_query = base_query + " WHERE a.doctor_name = :doc_name"
                patients = conn.execute(text(filtered_query), {"doc_name": logged_in_doctor_name}).fetchall()
                pending_docs = []

            # 6. FETCH ADMINS FOR THE DROPDOWN
            all_admins_query = conn.execute(text("SELECT username, doctor_name FROM doctor_admins WHERE username IS NOT NULL")).fetchall()
            all_admins = [{"username": r[0], "doctor_name": r[1]} for r in all_admins_query]

            # 7. FORMAT JSON DATA FOR DASHBOARD
            patient_list = [{
                "id": p[0], "name": p[1] or "Unknown", "email": p[2] or "N/A", "password": p[3] or "Not Set",
                "age": p[4] or "-", "hypertension": "Yes" if p[5] == 1 else "No", "bmi": p[6] or "-",
                "latest_clinical_risk": p[7] or "Pending", "latest_facial_droop": "Detected" if p[8] == 1 else "Normal",
                "next_appointment": f"{p[9]} at {p[10]}" if p[9] else "None", "doctor": p[11] or ""
            } for p in patients]

            return render_template('admin_dashboard.html',
                                   patients=patient_list,
                                   doctor_name=logged_in_doctor_name,
                                   is_superadmin=is_superadmin,
                                   pending_docs=[{"id": d[0], "name": d[1]} for d in pending_docs],
                                   all_admins=all_admins)

    except Exception as e:
        return f"Database Error: {str(e)}"

@app.route('/setup_doctor', methods=['POST'])
def setup_doctor():
    doc_id = request.form.get('doc_id')
    new_username = request.form.get('username')
    new_password = request.form.get('password')

    with db_pool.begin() as conn:
        conn.execute(text("""
            UPDATE doctor_admins SET username = :u, password = :p WHERE id = :id
        """), {"u": new_username, "p": new_password, "id": doc_id})

    return redirect('/admin')

@app.route('/change_credentials', methods=['POST'])
def change_credentials():
    # Updated to use Sessions instead of basic 'auth'
    if 'username' not in session:
        return redirect('/login')

    current_username = session['username']
    new_username = request.form.get('new_username')
    new_password = request.form.get('new_password')

    try:
        with db_pool.begin() as conn:
            conn.execute(text("""
                UPDATE doctor_admins 
                SET username = :new_u, password = :new_p 
                WHERE username = :old_u
            """), {
                "new_u": new_username,
                "new_p": new_password,
                "old_u": current_username
            })

        # Log the user out immediately so they have to use the new credentials
        session.pop('username', None)
        return redirect('/login')

    except Exception as e:
        return f"Error updating credentials: {str(e)}", 500

@app.route('/download_report')
def download_report():
    # 1. SECURITY: Check who is requesting the download
    if 'username' not in session:
        return redirect('/login')

    username = session['username']

    try:
        with db_pool.begin() as conn:
            # 2. Get the logged-in doctor's role
            admin_user = conn.execute(text("""
                SELECT doctor_name, is_superadmin FROM doctor_admins 
                WHERE username = :u
            """), {"u": username}).fetchone()

            if not admin_user:
                return redirect('/login')

            logged_in_doctor_name = admin_user[0]
            is_superadmin = int(admin_user[1])

            # 3. FILTER THE DATA based on who is logged in
            base_query = LATEST_PATIENT_QUERY.text
            if is_superadmin == 1:
                # SuperAdmin gets everyone
                data = conn.execute(text(base_query)).fetchall()
                report_title = "DeTechStroke Clinical Master Report"
            else:
                # Standard Doctor gets ONLY their assigned patients
                filtered_query = base_query + " WHERE a.doctor_name = :doc_name"
                data = conn.execute(text(filtered_query), {"doc_name": logged_in_doctor_name}).fetchall()
                report_title = f"Clinical Patient Roster: {logged_in_doctor_name}"

        # 4. Generate the PDF Report
        pdf = FPDF()
        pdf.add_page()

        current_time = datetime.now().strftime("%B %d, %Y - %I:%M %p")
        pdf.set_font("helvetica", "I", 10)
        pdf.set_text_color(100, 100, 100)
        pdf.cell(0, 10, f"Generated on: {current_time}", ln=True, align="R")
        pdf.set_text_color(0, 0, 0)

        # Print the dynamic title
        pdf.set_font("helvetica", "B", 16)
        pdf.cell(0, 10, report_title, ln=True, align="C")
        pdf.ln(10)

        # Table Headers
        pdf.set_font("helvetica", "B", 10)
        pdf.cell(40, 10, "Patient Name", 1)
        pdf.cell(15, 10, "Age", 1)
        pdf.cell(15, 10, "BMI", 1)
        pdf.cell(30, 10, "Risk (LR)", 1)
        pdf.cell(30, 10, "YOLOv10", 1)
        pdf.cell(60, 10, "Next Appointment", 1)
        pdf.ln()

        # Table Rows
        pdf.set_font("helvetica", "", 10)
        for row in data:
            pdf.cell(40, 10, str(row[1] or "Unknown"), 1)
            pdf.cell(15, 10, str(row[4] or "-"), 1)
            pdf.cell(15, 10, str(row[6] or "-"), 1)
            pdf.cell(30, 10, str(row[7] or "Pending"), 1)
            pdf.cell(30, 10, "⚠️ Detected" if row[8] == 1 else "Normal", 1)

            apt_text = f"{row[9]} {row[10]}" if row[9] else "None"
            pdf.cell(60, 10, apt_text, 1)
            pdf.ln()

        # Handle empty lists gracefully
        if not data:
            pdf.cell(0, 10, "No patients currently assigned.", border=1, ln=True, align="C")

        pdf_bytes = pdf.output(dest='S').encode('latin-1')

        response = make_response(pdf_bytes)
        response.headers['Content-Type'] = 'application/pdf'

        # Dynamically name the downloaded file!
        safe_doc_name = logged_in_doctor_name.replace(" ", "_").replace(".", "")
        response.headers['Content-Disposition'] = f'attachment; filename=DeTechStroke_Report_{safe_doc_name}.pdf'
        return response

    except Exception as e:
        return f"PDF Generation Error: {str(e)}"

@app.route('/download_patient/<int:user_id>')
def download_patient_report(user_id):
    try:
        from datetime import datetime

        with db_pool.begin() as conn:


            conn.execute(text("""
                CREATE TABLE IF NOT EXISTS extended_metrics (
                    user_id INT PRIMARY KEY,
                    height FLOAT,
                    weight FLOAT,
                    cholesterol FLOAT,
                    fbs FLOAT
                    
                )
            """))

            # Fetch User & Profile
            profile = conn.execute(text("""
                SELECT u.name, u.email, p.age, p.gender, p.bmi, p.smoking_status,
                       p.hypertension, p.diabetes, p.stroke_history, p.cardiac_disease,
                       p.hdl, p.ldl, p.triglycerides,
                       em.height, em.weight, em.cholesterol, em.fbs, em.updated_at
                FROM users u 
                LEFT JOIN user_profiles p ON u.id = p.user_id 
                LEFT JOIN extended_metrics em ON u.id = em.user_id
                WHERE u.id = :uid
            """), {"uid": user_id}).fetchone()

            if not profile:
                return "Patient not found", 404

            risks = conn.execute(text("SELECT lr_prediction, risk_level, timestamp FROM risk_assessments WHERE user_id = :uid ORDER BY timestamp DESC LIMIT 5"), {"uid": user_id}).fetchall()
            scans = conn.execute(text("""
                SELECT asymmetric_detected, timestamp, scan_image 
                FROM facial_scans WHERE user_id = :uid ORDER BY timestamp DESC LIMIT 5
            """), {"uid": user_id}).fetchall()
            appointments = conn.execute(text("SELECT doctor_name, apt_date, apt_time FROM appointments WHERE user_id = :uid ORDER BY id DESC"),
                                        {"uid": user_id}).fetchall()

        pdf = FPDF()
        pdf.add_page()

        pdf.set_font("helvetica", "B", 18)
        pdf.cell(0, 10, "DeTechStroke - Individual Patient Record", ln=True, align="C")
        pdf.set_font("helvetica", "I", 10)
        pdf.set_text_color(100, 100, 100)
        pdf.cell(0, 10, f"Generated on: {datetime.now().strftime('%B %d, %Y - %I:%M %p')}", ln=True, align="C")
        pdf.set_text_color(0, 0, 0)
        pdf.ln(5)

        # ---------------------------------------------------------
        # Section 1: Demographics & Core Risk Factors
        # ---------------------------------------------------------
        pdf.set_font("helvetica", "B", 12)
        pdf.cell(0, 10, "1. Patient Demographics & Core Risk Factors", ln=True)
        pdf.set_font("helvetica", "I", 9)
        pdf.set_text_color(100, 100, 100)
        pdf.cell(0, 5, f"Data recorded as of: {profile[17] or 'Not Recorded'}", ln=True)
        pdf.set_text_color(0, 0, 0)
        pdf.ln(3)
        pdf.set_font("helvetica", "", 10)


        # Left Column (Demographics) | Right Column (Risks)
        pdf.cell(90, 6, f"Name: {profile[0] or 'Unknown'}", 0, 0)
        pdf.cell(90, 6, f"Hypertension: {'Yes' if profile[6] == 1 else 'No'}", 0, 1)

        pdf.cell(90, 6, f"Email: {profile[1] or 'N/A'}", 0, 0)
        pdf.cell(90, 6, f"Heart Disease: {'Yes' if profile[9] == 1 else 'No'}", 0, 1)

        pdf.cell(90, 6, f"Age: {profile[2] or '-'} | Gender: {profile[3] or '-'}", 0, 0)
        pdf.cell(90, 6, f"Smoker: {profile[5] or 'Unknown'}", 0, 1)

        pdf.cell(90, 6, f"Height: {profile[13] or '-'} cm | Weight: {profile[14] or '-'} kg", 0, 0)
        pdf.cell(90, 6, f"BMI: {profile[4] or '-'}", 0, 1)
        pdf.ln(5)


        # ---------------------------------------------------------
        # Section 2: Blood Chemistry Panel
        # ---------------------------------------------------------
        pdf.set_font("helvetica", "B", 12)
        pdf.cell(0, 10, "2. Blood Chemistry Panel", ln=True)
        pdf.set_font("helvetica", "I", 9)
        pdf.set_text_color(100, 100, 100)
        pdf.cell(0, 5, f"Data recorded as of: {profile[17] or 'Not Recorded'}", ln=True)
        pdf.set_text_color(0, 0, 0)
        pdf.ln(3)
        pdf.set_font("helvetica", "", 10)


        pdf.cell(60, 8, f"Total Chol: {profile[15] or '-'} mg/dL", 1, 0)
        pdf.cell(60, 8, f"HDL: {profile[10] or '-'} mg/dL", 1, 0)
        pdf.cell(60, 8, f"LDL: {profile[11] or '-'} mg/dL", 1, 1)
        pdf.cell(60, 8, f"Triglycerides: {profile[12] or '-'} mg/dL", 1, 0)
        pdf.cell(60, 8, f"Fasting BS: {profile[16] or '-'} mg/dL", 1, 1)
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
        # Section 4: YOLOv10 Facial Scan History
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

            if pdf.get_y() + row_height > 270:
                pdf.add_page()

            x_start = pdf.get_x()
            y_start = pdf.get_y()

            pdf.cell(50, row_height, str(s[1]), 1)
            pdf.cell(50, row_height, "⚠️ Detected" if s[0] == 1 else "Normal", 1)
            pdf.cell(80, row_height, "", 1)

            if s[2]:
                try:
                    with tempfile.NamedTemporaryFile(delete=False, suffix=".jpg") as temp_img:
                        temp_img.write(base64.b64decode(s[2]))
                        temp_img_path = temp_img.name

                    pdf.image(temp_img_path, x=x_start + 102, y=y_start + 2, w=40)
                    os.remove(temp_img_path)
                except Exception as e:
                    pdf.text(x_start + 105, y_start + 25, "Image Error")
            else:
                pdf.text(x_start + 125, y_start + 25, "No Image")

            pdf.ln(row_height)

        if not scans:
            pdf.cell(0, 8, "No facial scans found.", ln=True)

        # ---------------------------------------------------------
        # Section 5: Scheduled Appointments
        # ---------------------------------------------------------
        if pdf.get_y() > 240:
            pdf.add_page()

        pdf.set_font("helvetica", "B", 12)
        pdf.cell(0, 10, "5. Scheduled Checkups & Appointments", ln=True)
        pdf.set_font("helvetica", "B", 10)

        pdf.cell(80, 8, "Doctor / Department", 1)
        pdf.cell(50, 8, "Date", 1)
        pdf.cell(60, 8, "Time", 1)
        pdf.ln()

        pdf.set_font("helvetica", "", 10)
        for apt in appointments:
            pdf.cell(80, 8, str(apt[0]), 1)
            pdf.cell(50, 8, str(apt[1]), 1)
            pdf.cell(60, 8, str(apt[2]), 1)
            pdf.ln()

        if not appointments:
            pdf.cell(0, 8, "No upcoming appointments scheduled.", ln=True)

        pdf.ln(8)

        response = make_response(pdf.output(dest='S').encode('latin-1'))
        response.headers['Content-Type'] = 'application/pdf'
        formatted_name = str(profile[0]).replace(" ", "_") if profile[0] else "Patient"
        response.headers['Content-Disposition'] = f'attachment; filename=DeTechStroke_Record_{formatted_name}.pdf'
        return response

    except Exception as e:
        return f"Patient PDF Error: {str(e)}"

@app.route('/delete_patient/<int:user_id>', methods=['POST'])
def delete_patient(user_id):
    try:
        with db_pool.begin() as conn:
            conn.execute(text("DELETE FROM facial_scans WHERE user_id = :uid"), {"uid": user_id})
            conn.execute(text("DELETE FROM risk_assessments WHERE user_id = :uid"), {"uid": user_id})
            conn.execute(text("DELETE FROM appointments WHERE user_id = :uid"), {"uid": user_id})
            conn.execute(text("DELETE FROM emergency_contacts WHERE user_id = :uid"), {"uid": user_id})

            conn.execute(text("DELETE FROM extended_metrics WHERE user_id = :uid"), {"uid": user_id})
            conn.execute(text("DELETE FROM user_profiles WHERE user_id = :uid"), {"uid": user_id})
            conn.execute(text("DELETE FROM users WHERE id = :uid"), {"uid": user_id})

        return redirect('/admin')
    except Exception as e:
        return f"Error deleting patient: {str(e)}", 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)