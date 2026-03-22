import os
import sqlalchemy
from sqlalchemy import text
from flask import Flask, request, jsonify, render_template

# Initialize the Flask Web Server
app = Flask(__name__)

# ==========================================
# 1. GOOGLE CLOUD SQL CONFIGURATION
# ==========================================
db_user = os.environ.get("DB_USER", "root")
db_pass = os.environ.get("DB_PASS", "your_cloud_password")
db_name = os.environ.get("DB_NAME", "detechstroke_db")
db_host = os.environ.get("DB_HOST", "127.0.0.1")  # Cloud SQL IP

print("Initializing Google Cloud SQL Connection Pool...")
db_pool = sqlalchemy.create_engine(
    f"mysql+pymysql://{db_user}:{db_pass}@{db_host}/{db_name}",
    pool_size=5,
    max_overflow=2,
    pool_timeout=30,
    pool_recycle=1800
)


# ==========================================
# 2. MOBILE APP SYNC ENDPOINTS (The Bridge)
# ==========================================

@app.route('/sync_to_cloud', methods=['POST'])
def sync_to_cloud():
    """Catches offline TFLite results and SQLite data from the Android app."""
    try:
        data = request.json
        user_id = data.get('user_id')
        profile = data.get('user_profile')
        contacts = data.get('emergency_contacts', [])
        appointments = data.get('appointments', [])

        # New payload additions coming from the offline Android TFLite models
        latest_scan = data.get('latest_facial_scan')
        latest_risk = data.get('latest_risk_assessment')

        if not user_id:
            return jsonify({"success": False, "error": "Missing User ID in payload"}), 400

        with db_pool.connect() as conn:

            # 1. Sync User Profile (Upsert)
            if profile:
                query = text("""
                    INSERT INTO user_profiles (user_id, age, gender, hypertension, bmi, smoking_status)
                    VALUES (:uid, :age, :gen, :hyp, :bmi, :smoke)
                    ON DUPLICATE KEY UPDATE 
                    age=:age, hypertension=:hyp, bmi=:bmi, smoking_status=:smoke
                """)
                conn.execute(query, {
                    "uid": user_id,
                    "age": int(profile.get("age", 0) if str(profile.get("age")).isdigit() else 0),
                    "gen": profile.get("sex", "Unknown"),
                    "hyp": 1 if profile.get("hypertension") == "Yes" else 0,
                    "bmi": float(
                        profile.get("bmi", 0.0) if str(profile.get("bmi")).replace('.', '', 1).isdigit() else 0.0),
                    "smoke": profile.get("smoker", "Unknown")
                })

            # 2. Sync Emergency Contacts
            if contacts:
                conn.execute(text("DELETE FROM emergency_contacts WHERE user_id = :uid"), {"uid": user_id})
                for contact in contacts:
                    conn.execute(text("""
                        INSERT INTO emergency_contacts (user_id, name, relationship, phone_number, is_primary)
                        VALUES (:uid, :name, :rel, :phone, :primary)
                    """), {
                        "uid": user_id,
                        "name": contact.get("name"),
                        "rel": contact.get("relationship"),
                        "phone": contact.get("phone_number"),
                        "primary": int(contact.get("is_primary", 0))
                    })

            # 3. Sync Appointments
            if appointments:
                conn.execute(text("DELETE FROM appointments WHERE user_id = :uid"), {"uid": user_id})
                for apt in appointments:
                    conn.execute(text("""
                        INSERT INTO appointments (user_id, doctor_name, apt_date, apt_time)
                        VALUES (:uid, :doc, :date, :time)
                    """), {
                        "uid": user_id,
                        "doc": apt.get("doctor_name"),
                        "date": apt.get("apt_date"),
                        "time": apt.get("apt_time")
                    })

            # 4. Sync TFLite AI Results (Saves the on-device math to the cloud)
            if latest_risk:
                conn.execute(text("""
                    INSERT INTO risk_assessments (user_id, lr_prediction, risk_level, timestamp)
                    VALUES (:uid, :pred, :lvl, :time)
                """), {
                    "uid": user_id,
                    "pred": latest_risk.get("lr_prediction"),
                    "lvl": latest_risk.get("risk_level"),
                    "time": latest_risk.get("timestamp")
                })

            if latest_scan:
                conn.execute(text("""
                    INSERT INTO facial_scans (user_id, asymmetric_detected, timestamp)
                    VALUES (:uid, :detected, :time)
                """), {
                    "uid": user_id,
                    "detected": 1 if latest_scan.get("detected") else 0,
                    "time": latest_scan.get("timestamp")
                })

            conn.commit()

        print(f"Cloud Sync Complete: Data secured for User {user_id}")
        return jsonify({"success": True, "message": "Cloud sync complete."})

    except Exception as e:
        print(f"Cloud Sync Error: {str(e)}")
        return jsonify({"success": False, "error": str(e)}), 500


@app.route('/login_and_sync', methods=['POST'])
def login_and_sync():
    """Authenticates the user and pushes their full cloud history down to a new device."""
    try:
        data = request.json
        email = data.get('email')
        password_hash = data.get('passwordHash')

        if not email or not password_hash:
            return jsonify({"success": False, "message": "Missing credentials"}), 400

        with db_pool.connect() as conn:
            user_query = text("SELECT id FROM users WHERE email = :email AND password = :pass")
            user_result = conn.execute(user_query, {"email": email, "pass": password_hash}).fetchone()

            if not user_result:
                return jsonify({"success": False, "message": "Invalid email or password"}), 401

            user_id = user_result[0]

            # Fetch Profile
            profile_row = conn.execute(
                text("SELECT age, gender, hypertension, bmi, smoking_status FROM user_profiles WHERE user_id = :uid"),
                {"uid": user_id}).fetchone()
            user_profile = {"age": str(profile_row[0]), "sex": profile_row[1],
                            "hypertension": "Yes" if profile_row[2] == 1 else "No", "bmi": str(profile_row[3]),
                            "smoker": profile_row[4]} if profile_row else None

            # Fetch Contacts
            contacts_rows = conn.execute(text(
                "SELECT name, relationship, phone_number, is_primary FROM emergency_contacts WHERE user_id = :uid"),
                                         {"uid": user_id}).fetchall()
            emergency_contacts = [{"name": r[0], "relationship": r[1], "phone_number": r[2], "is_primary": str(r[3])}
                                  for r in contacts_rows]

            # Fetch Appointments
            apt_rows = conn.execute(
                text("SELECT doctor_name, apt_date, apt_time FROM appointments WHERE user_id = :uid"),
                {"uid": user_id}).fetchall()
            appointments = [{"doctor_name": r[0], "apt_date": r[1], "apt_time": r[2]} for r in apt_rows]

            payload = {
                "user_id": user_id,
                "user_profile": user_profile,
                "emergency_contacts": emergency_contacts,
                "appointments": appointments
            }

            return jsonify({"success": True, "message": "Login successful. Syncing data...", "user_data": payload})

    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500


# ==========================================
# 3. DOCTOR'S WEB ADMIN PANEL
# ==========================================

@app.route('/admin')
def admin_dashboard():
    """Renders the HTML interface for doctors to monitor patient stroke risks."""
    try:
        with db_pool.connect() as conn:
            # Join the users table with their latest risk assessments and profiles
            query = text("""
                SELECT u.name, u.email, p.age, p.hypertension, p.bmi, r.risk_level, r.lr_prediction, f.asymmetric_detected
                FROM users u
                LEFT JOIN user_profiles p ON u.id = p.user_id
                LEFT JOIN (
                    SELECT user_id, risk_level, lr_prediction 
                    FROM risk_assessments 
                    ORDER BY timestamp DESC LIMIT 1
                ) r ON u.id = r.user_id
                LEFT JOIN (
                    SELECT user_id, asymmetric_detected 
                    FROM facial_scans 
                    ORDER BY timestamp DESC LIMIT 1
                ) f ON u.id = f.user_id
            """)
            patients = conn.execute(query).fetchall()

        # Format the data cleanly for the HTML template
        patient_list = []
        for p in patients:
            patient_list.append({
                "name": p[0] or "Unknown",
                "email": p[1] or "N/A",
                "age": p[2] or "-",
                "hypertension": "Yes" if p[3] == 1 else "No",
                "bmi": p[4] or "-",
                "latest_clinical_risk": p[5] or "Pending",
                "latest_facial_droop": "Detected" if p[7] == 1 else "Normal"
            })


        return render_template('admin_dashboard.html', patients=patient_list)

    except Exception as e:
        return f"Database connection error: {str(e)}"

# ==========================================
# START THE SERVER
# ==========================================
if __name__ == '__main__':

    app.run(host='0.0.0.0', port=5000, debug=True)