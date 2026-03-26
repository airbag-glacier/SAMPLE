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
db_pass = os.environ.get("DB_PASS", "Gabagool#1970")
db_name = os.environ.get("DB_NAME", "detechstroke_db")
db_host = os.environ.get("DB_HOST", "127.0.0.1")
db_port = os.environ.get("DB_PORT", "3307")

print("Initializing Google Cloud SQL Connection Pool...")
db_pool = sqlalchemy.create_engine(
    f"mysql+pymysql://{db_user}:{db_pass}@{db_host}:{db_port}/{db_name}",
    pool_size=5,
    max_overflow=2,
    pool_timeout=30,
    pool_recycle=1800
)

# ==========================================
# 2. MOBILE APP SYNC ENDPOINTS
# ==========================================

@app.route('/sync_to_cloud', methods=['POST'])
def sync_to_cloud():
    """Catches data from the Android app and saves it to Cloud SQL."""
    try:
        data = request.json
        user_id = data.get('userId') # Updated to match Kotlin 'userId'
        profile = data.get('userProfile')
        contacts = data.get('emergencyContacts', [])
        appointments = data.get('appointments', [])

        # Fix: Matching Kotlin CamelCase names
        latest_scan = data.get('latestFacialScan')
        latest_risk = data.get('latestRiskAssessment')

        if not user_id:
            return jsonify({"success": False, "error": "Missing User ID"}), 400

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
                    "age": int(profile.get("age", 0)),
                    "gen": profile.get("sex", "Unknown"),
                    "hyp": 1 if profile.get("hypertension") == "Yes" else 0,
                    "bmi": float(profile.get("bmi", 0.0)),
                    "smoke": profile.get("smoker", "Unknown")
                })

            # 2. Sync Risk Assessments (Logistic Regression results)
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

            # 3. Sync Facial Scans (YOLOv10 results)
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

# ==========================================
# 3. DOCTOR'S WEB ADMIN PANEL
# ==========================================

@app.route('/admin')
def admin_dashboard():
    """Renders dashboard with the LATEST results for EACH patient."""
    try:
        with db_pool.connect() as conn:
            # Fix: Using subqueries to get the latest entry PER user
            query = text("""
                SELECT u.name, u.email, p.age, p.hypertension, p.bmi, r.risk_level, r.lr_prediction, f.asymmetric_detected
                FROM users u
                LEFT JOIN user_profiles p ON u.id = p.user_id
                LEFT JOIN risk_assessments r ON r.user_id = u.id AND r.timestamp = (
                    SELECT MAX(timestamp) FROM risk_assessments WHERE user_id = u.id
                )
                LEFT JOIN facial_scans f ON f.user_id = u.id AND f.timestamp = (
                    SELECT MAX(timestamp) FROM facial_scans WHERE user_id = u.id
                )
            """)
            patients = conn.execute(query).fetchall()

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

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)