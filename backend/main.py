# pyrefly: ignore [missing-import]
from fastapi import FastAPI, Depends, HTTPException, status, APIRouter
# pyrefly: ignore [missing-import]
from fastapi.middleware.cors import CORSMiddleware
# pyrefly: ignore [missing-import]
from sqlalchemy.orm import Session
from typing import List, Dict, Any

from .database import engine, Base, get_db
from . import models, schemas
import json
import os
import urllib.request

# Initialize database tables
Base.metadata.create_all(bind=engine)

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
    # Accept any credential and bypass validation for testing convenience
    return {
        "access_token": "mock-jwt-token-for-swasthai",
        "token_type": "bearer",
        "user": {"id": 1, "username": credentials.username, "role": "health_worker"}
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
    return [p.data or {
        "id": p.id, "name": p.name, "age": p.age, "gender": p.gender, "contact": p.contact
    } for p in patients]

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
def sync_batch(body: List[Dict[str, Any]], db: Session = Depends(get_db)):
    processed_count = 0
    errors = []
    
    # Processes dynamic queue batch items.
    for item in body:
        try:
            item_type = item.get("type")
            action = item.get("action", "create")
            payload = item.get("payload", {})
            
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
