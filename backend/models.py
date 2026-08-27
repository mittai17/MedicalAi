from sqlalchemy import Column, String, Integer, Float, Text, ForeignKey, JSON
from .database import Base

class User(Base):
    __tablename__ = "users"
    
    id = Column(Integer, primary_key=True, index=True)
    username = Column(String, unique=True, index=True, nullable=False)
    password = Column(String, nullable=False)
    email = Column(String, unique=True, index=True)
    role = Column(String, default="worker")  # e.g., citizen, health_worker

class Patient(Base):
    __tablename__ = "patients"
    
    id = Column(String, primary_key=True, index=True)
    name = Column(String, index=True)
    age = Column(Integer)
    gender = Column(String)
    contact = Column(String)
    data = Column(JSON, nullable=True)  # captures dynamic metadata fields

class Screening(Base):
    __tablename__ = "screenings"
    
    id = Column(String, primary_key=True, index=True)
    patient_id = Column(String, index=True)
    type = Column(String)  # image, voice, etc.
    result = Column(String)
    confidence = Column(Float)
    details = Column(JSON, nullable=True)

class Vitals(Base):
    __tablename__ = "vitals"
    
    id = Column(String, primary_key=True, index=True)
    patient_id = Column(String, index=True)
    systolic = Column(Integer)
    diastolic = Column(Integer)
    heart_rate = Column(Integer)
    temperature = Column(Float)
    spo2 = Column(Integer)
    data = Column(JSON, nullable=True)

class Symptom(Base):
    __tablename__ = "symptoms"
    
    id = Column(String, primary_key=True, index=True)
    patient_id = Column(String, index=True)
    symptoms = Column(JSON, nullable=True)
    notes = Column(Text, nullable=True)

class Report(Base):
    __tablename__ = "reports"
    
    id = Column(String, primary_key=True, index=True)
    patient_id = Column(String, index=True)
    title = Column(String)
    summary = Column(Text)
    created_at = Column(String)
    data = Column(JSON, nullable=True)

class Referral(Base):
    __tablename__ = "referrals"
    
    id = Column(String, primary_key=True, index=True)
    patient_id = Column(String, index=True)
    status = Column(String, default="pending")  # pending, completed
    reason = Column(Text)
    facility = Column(String)
    data = Column(JSON, nullable=True)
