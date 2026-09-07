# pyrefly: ignore [missing-import]
from fastapi import FastAPI, Depends, HTTPException, status, APIRouter, Request
# pyrefly: ignore [missing-import]
from fastapi.middleware.cors import CORSMiddleware
# pyrefly: ignore [missing-import]
from sqlalchemy.orm import Session
# pyrefly: ignore [missing-import]
from sqlalchemy import text
from typing import Dict, Any

from .database import engine, Base, get_db, SessionLocal
from . import models, schemas
import json
import os
import urllib.request

# Initialize database tables
Base.metadata.create_all(bind=engine)


def _migrate_schema(db: Session) -> None:
    """Idempotently backfill columns added after an initial migration was run
    (e.g. User.is_active) without dropping user data. create_all() only adds
    brand-new tables, so existing tables must be patched by hand."""
    existing = {row[1] for row in db.execute(text("PRAGMA table_info(users)"))}
    if "is_active" not in existing:
        db.execute(text("ALTER TABLE users ADD COLUMN is_active BOOLEAN DEFAULT 1"))
        db.commit()

SEEDED_FLAG = "swasthai_seeded_v1"


def _seed_demo_data(db: Session) -> None:
    """Seed representative demo records so the govt admin console has data on
    first launch. Runs once per database (idempotent via the users table)."""
    if db.query(models.User).first():
        return

    workers = [
        models.User(username="asha", password="pass", email="asha@example.org", role="health_worker"),
        models.User(username="pragya", password="pass", email="pragya@example.org", role="health_worker"),
        models.User(username="vijay", password="pass", email="vijay@example.org", role="health_worker"),
    ]
    citizens = [
        models.User(username="meera", password="pass", email="meera@example.org", role="citizen"),
        models.User(username="raman", password="pass", email="raman@example.org", role="citizen"),
        models.User(username="lakshmi", password="pass", email="lakshmi@example.org", role="citizen"),
    ]
    db.add_all(workers + citizens)
    db.commit()
    for u in workers + citizens:
        db.refresh(u)

    patients = [
        models.Patient(id="P-1001", name="Meera Nair", age=42, gender="female",
                       contact="98xxxxx01", data={"workerId": str(workers[0].id)}),
        models.Patient(id="P-1002", name="Raman Iyer", age=58, gender="male",
                       contact="98xxxxx02", data={"workerId": str(workers[1].id)}),
        models.Patient(id="P-1003", name="Lakshmi Devi", age=6, gender="female",
                       contact="98xxxxx03", data={"workerId": str(workers[0].id)}),
    ]
    db.add_all(patients)
    db.commit()

    screenings = [
        models.Screening(id="S-2001", patient_id="P-1001", type="SYMPTOM_CHECK",
                         result="Influenza (Flu)", confidence=0.62,
                         details={"result": "Influenza (Flu)", "confidence": 0.62,
                                  "symptoms": ["Fever", "Cough", "Headache"],
                                  "workerId": str(workers[0].id)}),
        models.Screening(id="S-2002", patient_id="P-1002", type="SYMPTOM_CHECK",
                         result="Pneumonia", confidence=0.74,
                         details={"result": "Pneumonia", "confidence": 0.74,
                                  "symptoms": ["Fever", "Cough", "Difficulty Breathing"],
                                  "workerId": str(workers[1].id)}),
        models.Screening(id="S-2003", patient_id="P-1003", type="VOICE",
                         result="Dengue", confidence=0.53,
                         details={"result": "Dengue", "confidence": 0.53,
                                  "voiceTranscript": "fever, body ache, rash",
                                  "workerId": str(workers[0].id)}),
    ]
    db.add_all(screenings)
    db.commit()

    referrals = [
        models.Referral(id="R-3001", patient_id="P-1002", status="pending",
                        reason="High-risk screening: Pneumonia", facility="District Hospital"),
        models.Referral(id="R-3002", patient_id="P-1001", status="completed",
                        reason="Persistent fever > 5 days", facility="Community Health Centre"),
    ]
    db.add_all(referrals)
    db.commit()

    vitals = [
        models.Vitals(id="V-4001", patient_id="P-1001", systolic=126, diastolic=82,
                      heart_rate=88, temperature=38.2, spo2=96,
                      data={"temperature": 38.2, "spo2": 96, "heartRate": 88, "pulse": 88}),
        models.Vitals(id="V-4002", patient_id="P-1002", systolic=142, diastolic=92,
                      heart_rate=104, temperature=39.1, spo2=92,
                      data={"temperature": 39.1, "spo2": 92, "heartRate": 104, "pulse": 104}),
        models.Vitals(id="V-4003", patient_id="P-1003", systolic=98, diastolic=62,
                      heart_rate=118, temperature=38.8, spo2=95,
                      data={"temperature": 38.8, "spo2": 95, "heartRate": 118, "pulse": 118}),
    ]
    db.add_all(vitals)
    db.commit()

    reports = [
        models.Report(id="RP-5001", patient_id="P-1002", title="Chest X-ray — Pneumonia",
                      summary="Bilateral infiltrates consistent with pneumonia. Referred for follow-up.",
                      created_at="2026-08-14"),
    ]
    db.add_all(reports)
    db.commit()


_migrate_schema(SessionLocal())
_seed_demo_data(SessionLocal())

app = FastAPI(
    title="SwasthAI Backend",
    description="Python FastAPI backend for offline-first AI-powered medical diagnostics platform",
    version="1.0.0"
)

# CORS configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

router = APIRouter(prefix="/api/v1")

# ── Auth Endpoints ──

@router.post("/auth/register")
def register(user: schemas.UserRegister, db: Session = Depends(get_db)):
    db_user = db.query(models.User).filter(models.User.username == user.username).first()
    if db_user:
        raise HTTPException(status_code=400, detail="Username already registered")
    new_user = models.User(
        username=user.username,
        password=user.password,  # Simple plaintext/mock storage for local test
        email=user.email,
        role=user.role
    )
    db.add(new_user)
    db.commit()
    db.refresh(new_user)
    return {
        "status": "success",
        "user": {"id": new_user.id, "username": new_user.username, "role": new_user.role}
    }

@router.post("/auth/login")
def login(credentials: schemas.UserLogin, db: Session = Depends(get_db)):
    # Accept any credential and bypass validation for testing convenience.
    db_user = db.query(models.User).filter(models.User.username == credentials.username).first()
    if not db_user:
        db_user = models.User(
            username=credentials.username,
            password=credentials.password,  # Simple plaintext/mock storage for local test
            role="health_worker"
        )
        db.add(db_user)
        db.commit()
        db.refresh(db_user)
    return {
        "access_token": "mock-jwt-token-for-swasthai",
        "token_type": "bearer",
        "user": {"id": db_user.id, "username": db_user.username, "role": db_user.role}
    }

# ── User Management (govt / SaaS admin console) ──

def _user_out(u: models.User) -> Dict[str, Any]:
    return {
        "id": u.id,
        "username": u.username,
        "email": u.email,
        "role": u.role,
        "is_active": u.is_active is not False,
    }

@router.get("/users")
def list_users(role: str | None = None, db: Session = Depends(get_db)):
    query = db.query(models.User)
    if role:
        query = query.filter(models.User.role == role)
    return [_user_out(u) for u in query.order_by(models.User.role, models.User.username).all()]

@router.get("/users/{user_id}")
def get_user(user_id: int, db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    return _user_out(user)

@router.patch("/users/{user_id}")
def update_user(user_id: int, body: schemas.UserUpdate, db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    if body.role is not None:
        user.role = body.role
    if body.is_active is not None:
        user.is_active = body.is_active
    db.commit()
    db.refresh(user)
    return _user_out(user)

@router.get("/users/{user_id}/patients")
def get_worker_patients(user_id: int, db: Session = Depends(get_db)):
    """Patients managed by a health worker.

    Patients are matched by their dynamic metadata (workerId / workerUsername /
    assignedTo). If no worker owns any patients yet, all clinic patients are
    returned so the admin console can still drill down (shared clinic model).
    """
    user = db.query(models.User).filter(models.User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    all_patients = db.query(models.Patient).all()
    owned = []
    own_id = str(user.id)
    own_username = user.username
    for p in all_patients:
        data = p.data or {}
        owner = str(data.get("workerId") or data.get("assignedToId") or "") == own_id or \
                str(data.get("workerUsername") or data.get("assignedTo") or "") == own_username
        if owner:
            owned.append(p)
    selected = owned if owned else all_patients
    return [{**(p.data or {}),
             "id": p.id, "name": p.name, "age": p.age, "gender": p.gender, "contact": p.contact}
            for p in selected]

# ── Overview Stats ──

@router.get("/stats")
def get_stats(db: Session = Depends(get_db)):
    users = db.query(models.User).all()
    role_counts: Dict[str, int] = {"citizen": 0, "health_worker": 0}
    for u in users:
        if u.role == "citizen":
            role_counts["citizen"] += 1
        elif u.role == "health_worker":
            role_counts["health_worker"] += 1
    screenings = db.query(models.Screening).all()
    high_risk = [s for s in screenings if (s.confidence or 0) >= 0.7]
    pending = db.query(models.Referral).filter(models.Referral.status == "pending").count()
    return {
        "users": role_counts,
        "total_users": len(users),
        "patients": db.query(models.Patient).count(),
        "screenings": len(screenings),
        "high_risk_screenings": len(high_risk),
        "vitals": db.query(models.Vitals).count(),
        "referrals": db.query(models.Referral).count(),
        "pending_referrals": pending,
        "reports": db.query(models.Report).count(),
    }

# ── Patients Endpoints ──

@router.post("/patients")
def upload_patient(body: Dict[str, Any], db: Session = Depends(get_db)):
    patient_id = body.get("id")
    if not patient_id:
        raise HTTPException(status_code=400, detail="Patient ID is required")
        
    db_patient = db.query(models.Patient).filter(models.Patient.id == patient_id).first()
    if db_patient:
        # Update existing
        db_patient.name = body.get("name", db_patient.name)
        db_patient.age = body.get("age", db_patient.age)
        db_patient.gender = body.get("gender", db_patient.gender)
        db_patient.contact = body.get("contact", db_patient.contact)
        db_patient.data = body
    else:
        # Create new
        new_patient = models.Patient(
            id=patient_id,
            name=body.get("name"),
            age=body.get("age"),
            gender=body.get("gender"),
            contact=body.get("contact"),
            data=body
        )
        db.add(new_patient)
    db.commit()
    return {"status": "success"}

@router.put("/patients/{id}")
def update_patient(id: str, body: Dict[str, Any], db: Session = Depends(get_db)):
    db_patient = db.query(models.Patient).filter(models.Patient.id == id).first()
    if not db_patient:
        raise HTTPException(status_code=404, detail="Patient not found")
    db_patient.name = body.get("name", db_patient.name)
    db_patient.age = body.get("age", db_patient.age)
    db_patient.gender = body.get("gender", db_patient.gender)
    db_patient.contact = body.get("contact", db_patient.contact)
    db_patient.data = body
    db.commit()
    return {"status": "success"}

@router.get("/patients")
def get_patients(db: Session = Depends(get_db)):
    patients = db.query(models.Patient).all()
    return [{**(p.data or {}),
             "id": p.id, "name": p.name, "age": p.age, "gender": p.gender, "contact": p.contact}
            for p in patients]

@router.get("/patients/{patient_id}")
def get_patient(patient_id: str, db: Session = Depends(get_db)):
    patient = db.query(models.Patient).filter(models.Patient.id == patient_id).first()
    if not patient:
        raise HTTPException(status_code=404, detail="Patient not found")
    return {**(patient.data or {}),
            "id": patient.id, "name": patient.name, "age": patient.age,
            "gender": patient.gender, "contact": patient.contact}

@router.get("/patients/{patient_id}/records")
def get_patient_records(patient_id: str, db: Session = Depends(get_db)):
    """Aggregated clinical record for one patient: screenings, vitals, reports,
    referrals and symptom notes."""
    patient = db.query(models.Patient).filter(models.Patient.id == patient_id).first()
    if not patient:
        raise HTTPException(status_code=404, detail="Patient not found")

    def _screenings():
        rows = db.query(models.Screening).filter(models.Screening.patient_id == patient_id).all()
        return [{**(s.details or {}), "id": s.id, "type": s.type,
                 "result": s.result, "confidence": s.confidence} for s in rows]

    def _vitals():
        rows = db.query(models.Vitals).filter(models.Vitals.patient_id == patient_id).all()
        return [{**(v.data or {}), "id": v.id, "systolic": v.systolic, "diastolic": v.diastolic,
                 "heartRate": v.heart_rate, "temperature": v.temperature,
                 "spo2": v.spo2} for v in rows]

    def _reports():
        rows = db.query(models.Report).filter(models.Report.patient_id == patient_id).all()
        return [r.data or {"id": r.id, "title": r.title, "summary": r.summary,
                           "createdAt": r.created_at} for r in rows]

    def _referrals():
        rows = db.query(models.Referral).filter(models.Referral.patient_id == patient_id).all()
        return [{**(r.data or {}), "id": r.id, "status": r.status,
                 "reason": r.reason, "facility": r.facility} for r in rows]

    def _symptoms():
        rows = db.query(models.Symptom).filter(models.Symptom.patient_id == patient_id).all()
        return [{"id": s.id, "symptoms": s.symptoms, "notes": s.notes} for s in rows]

    return {
        "patient": {**(patient.data or {}),
                    "id": patient.id, "name": patient.name, "age": patient.age,
                    "gender": patient.gender, "contact": patient.contact},
        "screenings": _screenings(),
        "vitals": _vitals(),
        "reports": _reports(),
        "referrals": _referrals(),
        "symptoms": _symptoms(),
    }

# ── Screenings Endpoints ──

@router.post("/screenings")
def upload_screening(body: Dict[str, Any], db: Session = Depends(get_db)):
    id_ = body.get("id")
    if not id_:
        raise HTTPException(status_code=400, detail="Screening ID is required")
        
    db_screening = db.query(models.Screening).filter(models.Screening.id == id_).first()
    if db_screening:
        db_screening.result = body.get("result", db_screening.result)
        db_screening.confidence = body.get("confidence", db_screening.confidence)
        db_screening.details = body
    else:
        new_screening = models.Screening(
            id=id_,
            patient_id=body.get("patientId") or body.get("patient_id"),
            type=body.get("type"),
            result=body.get("result"),
            confidence=body.get("confidence"),
            details=body
        )
        db.add(new_screening)
    db.commit()
    return {"status": "success"}

@router.put("/screenings/{id}")
def update_screening(id: str, body: Dict[str, Any], db: Session = Depends(get_db)):
    db_screening = db.query(models.Screening).filter(models.Screening.id == id).first()
    if not db_screening:
        raise HTTPException(status_code=404, detail="Screening not found")
    db_screening.result = body.get("result", db_screening.result)
    db_screening.confidence = body.get("confidence", db_screening.confidence)
    db_screening.details = body
    db.commit()
    return {"status": "success"}

@router.get("/screenings")
def get_screenings(db: Session = Depends(get_db)):
    screenings = db.query(models.Screening).all()
    return [{**(s.details or {}),
             "id": s.id, "patientId": s.patient_id, "type": s.type,
             "result": s.result, "confidence": s.confidence} for s in screenings]

# ── Vitals Endpoints ──

@router.post("/vitals")
def upload_vitals(body: Dict[str, Any], db: Session = Depends(get_db)):
    id_ = body.get("id")
    if not id_:
        raise HTTPException(status_code=400, detail="Vitals ID is required")
    db_vitals = db.query(models.Vitals).filter(models.Vitals.id == id_).first()
    if db_vitals:
        db_vitals.data = body
    else:
        new_v = models.Vitals(
            id=id_,
            patient_id=body.get("patientId") or body.get("patient_id"),
            systolic=body.get("systolic", 120),
            diastolic=body.get("diastolic", 80),
            heart_rate=body.get("heartRate") or body.get("heart_rate", 72),
            temperature=body.get("temperature", 36.6),
            spo2=body.get("spo2", 98),
            data=body
        )
        db.add(new_v)
    db.commit()
    return {"status": "success"}

@router.get("/vitals")
def get_vitals(patient_id: str | None = None, db: Session = Depends(get_db)):
    query = db.query(models.Vitals)
    if patient_id:
        query = query.filter(models.Vitals.patient_id == patient_id)
    rows = query.all()
    return [{**(v.data or {}), "id": v.id, "patientId": v.patient_id,
            "systolic": v.systolic, "diastolic": v.diastolic, "heartRate": v.heart_rate,
            "temperature": v.temperature, "spo2": v.spo2} for v in rows]

# ── Symptoms Endpoints ──

@router.post("/symptoms")
def upload_symptom(body: Dict[str, Any], db: Session = Depends(get_db)):
    id_ = body.get("id")
    if not id_:
        raise HTTPException(status_code=400, detail="Symptom ID is required")
    db_symptom = db.query(models.Symptom).filter(models.Symptom.id == id_).first()
    if db_symptom:
        db_symptom.symptoms = body
    else:
        new_s = models.Symptom(
            id=id_,
            patient_id=body.get("patientId") or body.get("patient_id"),
            symptoms=body,
            notes=body.get("notes")
        )
        db.add(new_s)
    db.commit()
    return {"status": "success"}

# ── Reports Endpoints ──

@router.post("/reports")
def upload_report(body: Dict[str, Any], db: Session = Depends(get_db)):
    id_ = body.get("id")
    if not id_:
        raise HTTPException(status_code=400, detail="Report ID is required")
    db_report = db.query(models.Report).filter(models.Report.id == id_).first()
    if db_report:
        db_report.data = body
    else:
        new_r = models.Report(
            id=id_,
            patient_id=body.get("patientId") or body.get("patient_id"),
            title=body.get("title", "Clinical Report"),
            summary=body.get("summary", ""),
            created_at=body.get("createdAt") or body.get("created_at"),
            data=body
        )
        db.add(new_r)
    db.commit()
    return {"status": "success"}

@router.get("/reports")
def get_reports(db: Session = Depends(get_db)):
    reports = db.query(models.Report).all()
    return [r.data or {
        "id": r.id, "patientId": r.patient_id, "title": r.title, "summary": r.summary, "createdAt": r.created_at
    } for r in reports]

# ── Referrals Endpoints ──

@router.post("/referrals")
def upload_referral(body: Dict[str, Any], db: Session = Depends(get_db)):
    id_ = body.get("id")
    if not id_:
        raise HTTPException(status_code=400, detail="Referral ID is required")
    db_ref = db.query(models.Referral).filter(models.Referral.id == id_).first()
    if db_ref:
        db_ref.data = body
    else:
        new_ref = models.Referral(
            id=id_,
            patient_id=body.get("patientId") or body.get("patient_id"),
            status=body.get("status", "pending"),
            reason=body.get("reason", ""),
            facility=body.get("facility", ""),
            data=body
        )
        db.add(new_ref)
    db.commit()
    return {"status": "success"}

@router.get("/referrals")
def get_referrals(db: Session = Depends(get_db)):
    referrals = db.query(models.Referral).all()
    return [r.data or {
        "id": r.id, "patientId": r.patient_id, "status": r.status, "reason": r.reason, "facility": r.facility
    } for r in referrals]

@router.patch("/referrals/{id}")
def update_referral(id: str, body: Dict[str, Any], db: Session = Depends(get_db)):
    referral = db.query(models.Referral).filter(models.Referral.id == id).first()
    if not referral:
        raise HTTPException(status_code=404, detail="Referral not found")
    if body.get("status") is not None:
        referral.status = body.get("status")
    if body.get("reason") is not None:
        referral.reason = body.get("reason")
    if body.get("facility") is not None:
        referral.facility = body.get("facility")
    if body.get("data") is not None:
        db_data = dict(referral.data or {})
        db_data.update(body.get("data"))
        referral.data = db_data
    db.commit()
    db.refresh(referral)
    return {**(referral.data or {}), "id": referral.id, "patientId": referral.patient_id,
            "status": referral.status, "reason": referral.reason, "facility": referral.facility}

@router.get("/referrals/pending")
def get_pending_referrals(db: Session = Depends(get_db)):
    pending = db.query(models.Referral).filter(models.Referral.status == "pending").all()
    return [r.data or {
        "id": r.id, "patientId": r.patient_id, "status": r.status, "reason": r.reason, "facility": r.facility
    } for r in pending]

# ── Health Tips Endpoints ──

@router.get("/health-tips")
def get_health_tips():
    return [
        {
            "id": "tip1",
            "title": "Stay Hydrated",
            "content": "Drink at least 8-10 glasses of clean water daily to remain hydrated and support organ functionality.",
            "category": "General Health"
        },
        {
            "id": "tip2",
            "title": "Hypertension Care",
            "content": "Reduce daily salt intake to under 5g (about 1 teaspoon) to help manage high blood pressure.",
            "category": "Cardiovascular"
        },
        {
            "id": "tip3",
            "title": "Maternal Nutrition",
            "content": "Ensure pregnant mothers receive adequate iron, folic acid, and protein-rich local foods.",
            "category": "Maternal Care"
        }
    ]

# ── Batch Sync Endpoint ──

@router.post("/sync/batch")
async def sync_batch(request: Request, db: Session = Depends(get_db)):
    processed_count = 0
    errors = []
    
    body = await request.json()
    # Accept either a flat list of items or a wrapper object with an "items" key.
    if isinstance(body, dict):
        items = body.get("items") or []
    else:
        items = body or []
    
    # Processes dynamic queue batch items.
    for item in items:
        try:
            item_type = item.get("type")
            action = item.get("action", "create")
            payload = item.get("payload", item)  # tolerate inlined payloads
            payload = payload if isinstance(payload, dict) else item
            
            if item_type == "patient":
                upload_patient(payload, db)
            elif item_type == "screening":
                upload_screening(payload, db)
            elif item_type == "vitals":
                upload_vitals(payload, db)
            elif item_type == "symptom":
                upload_symptom(payload, db)
            elif item_type == "report":
                upload_report(payload, db)
            elif item_type == "referral":
                upload_referral(payload, db)
            else:
                errors.append(f"Unknown type: {item_type}")
                continue
            processed_count += 1
        except Exception as e:
            errors.append(str(e))
            
    return {
        "status": "success" if not errors else "partial_success",
        "processed_count": processed_count,
        "errors": errors
    }

# ── AI Fallback (Low-end friendly) ──
#
# The SwasthAI app is offline-first: inference runs on-device (TFLite + the
# reasoning engine) so it works on low-end phones with no big model download.
# When the on-device engine is uncertain, the app may optionally consult this
# endpoint for an LLM-assisted second opinion.
#
# If GEMMA_API_KEY is set, this calls a hosted Gemma (Gemini-compatible)
# endpoint. Otherwise (or on failure) it returns a safe, structured fallback
# derived from rule-based reasoning so the app always receives a valid reply.
#
# Configure via env vars:
#   GEMMA_API_KEY     - API key for the hosted Gemma model
#   GEMMA_BASE_URL    - endpoint base (default: Gemini API)
#   GEMMA_MODEL       - model id (default: Gemma 4 E2B-IT, the light on-device
#                       multimodal Gemma used as the app's LLM fallback)

GEMMA_API_KEY = os.environ.get("GEMMA_API_KEY", "")
GEMMA_BASE_URL = os.environ.get("GEMMA_BASE_URL", "https://generativelanguage.googleapis.com")
GEMMA_MODEL = os.environ.get("GEMMA_MODEL", "gemma-4-e2b-it")

@router.post("/ai/fallback")
def ai_fallback(body: Dict[str, Any]):
    symptoms = body.get("symptoms", [])
    vitals = body.get("vitals") or {}
    transcript = body.get("voiceTranscript") or ""
    scan_type = body.get("scanType", "SYMPTOM_CHECK")

    symptom_names = ", ".join(symptoms) if symptoms else (transcript or "General symptoms")
    temperature = vitals.get("temperature")
    spo2 = vitals.get("spo2")
    pulse = vitals.get("pulse")

    # 1) Try the hosted Gemma model when configured.
    llm_result = None
    if GEMMA_API_KEY:
        try:
            llm_result = _call_gemma(symptom_names, scan_type, temperature, spo2, pulse)
        except Exception:
            llm_result = None

    # 2) Graceful structured fallback.
    if llm_result:
        return {**llm_result, "provider": "gemma-remote"}
    return {
        "predictedDisease": _fallback_disease(symptoms, transcript),
        "advice": _fallback_advice(temperature, spo2, pulse),
        "note": "Using built-in fallback reasoning (no Gemma API key configured).",
        "provider": "fallback-rules",
        "confidence": 0.5
    }


def _call_gemma(symptom_text: str, scan_type: str, temperature, spo2, pulse) -> Dict[str, Any] | None:
    url = f"{GEMMA_BASE_URL}/v1beta/models/{GEMMA_MODEL}:generateContent"
    payload = {
        "contents": [{
            "parts": [{
                "text": (
                    "You are a medical triage screener for SwasthAI, a community health "
                    "app. Keep answers brief, non-diagnostic, and in plain language. "
                    f"Symptoms: {symptom_text}. Scan type: {scan_type}. "
                    f"Vitals: temp={temperature}, spo2={spo2}%, pulse={pulse}. "
                    "Reply as JSON with keys predictedDisease (string), "
                    "advice (string), risk (low|moderate|high)."
                )
            }]
        }],
        "generationConfig": {"temperature": 0.2, "maxOutputTokens": 300}
    }
    req = urllib.request.Request(
        url,
        data=json.dumps(payload).encode(),
        headers={"Content-Type": "application/json",
                 "x-goog-api-key": GEMMA_API_KEY},
        method="POST"
    )
    with urllib.request.urlopen(req, timeout=20) as resp:
        data = json.loads(resp.read().decode())
    text = (data.get("candidates") or [{}])[0].get("content", {}).get("parts", [{}])[0].get("text", "")
    return _parse_llm_json(text)


def _parse_llm_json(text: str) -> Dict[str, Any] | None:
    try:
        start = text.find("{")
        end = text.rfind("}")
        if start != -1 and end != -1 and end > start:
            return json.loads(text[start:end + 1])
    except Exception:
        pass
    return None


def _fallback_disease(symptoms: list, transcript: str) -> str:
    for s in symptoms:
        low = str(s).lower()
        if "fever" in low or "temperature" in low:
            return "Possible Fever / Viral Infection"
        if "cough" in low or "breath" in low:
            return "Possible Respiratory Infection"
        if "rash" in low:
            return "Possible Skin Condition"
    if transcript:
        return "Symptoms noted — see advice below"
    return "General health concern"


def _fallback_advice(temperature, spo2, pulse) -> str:
    notes = []
    if temperature is not None and temperature >= 38.0:
        notes.append("You may have a fever; keep hydrated and rest.")
    if spo2 is not None and spo2 < 94:
        notes.append("Your oxygen level is low — seek help soon.")
    if pulse is not None and (pulse > 120 or pulse < 50):
        notes.append("Your heart rate is outside the normal range.")
    return " ".join(notes) or "Monitor symptoms and visit a health centre if they worsen."

app.include_router(router)
