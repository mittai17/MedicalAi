# pyrefly: ignore [missing-import]
from pydantic import BaseModel, EmailStr
from typing import Optional, Dict, Any, List

class UserRegister(BaseModel):
    username: str
    password: str
    email: Optional[EmailStr] = None
    role: Optional[str] = "worker"

class UserLogin(BaseModel):
    username: str
    password: str

class TokenResponse(BaseModel):
    access_token: str
    token_type: str
    user: Dict[str, Any]

class PatientCreate(BaseModel):
    id: str
    name: str
    age: int
    gender: str
    contact: str
    data: Optional[Dict[str, Any]] = None

class ScreeningCreate(BaseModel):
    id: str
    patient_id: str
    type: str
    result: str
    confidence: float
    details: Optional[Dict[str, Any]] = None

class VitalsCreate(BaseModel):
    id: str
    patient_id: str
    systolic: int
    diastolic: int
    heart_rate: int
    temperature: float
    spo2: int
    data: Optional[Dict[str, Any]] = None

class SymptomCreate(BaseModel):
    id: str
    patient_id: str
    symptoms: Optional[Dict[str, Any]] = None
    notes: Optional[str] = None

class ReportCreate(BaseModel):
    id: str
    patient_id: str
    title: str
    summary: str
    created_at: str
    data: Optional[Dict[str, Any]] = None

class ReferralCreate(BaseModel):
    id: str
    patient_id: str
    status: Optional[str] = "pending"
    reason: str
    facility: str
    data: Optional[Dict[str, Any]] = None

class SyncBatchItem(BaseModel):
    type: str  # e.g., "patient", "screening", "vitals", "symptom", "report", "referral"
    action: str  # e.g., "create", "update"
    payload: Dict[str, Any]
