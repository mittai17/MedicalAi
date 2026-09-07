export type Role = "admin";

export interface Session {
  role: Role;
  username: string;
  token: string;
}

/** Payload shapes returned by the SwasthAI FastAPI backend (/api/v1). */

export interface AuthUser {
  id?: number;
  username?: string;
  role?: string;
}

export interface LoginResponse {
  access_token: string;
  token_type: string;
  user: AuthUser;
}

export interface RegisterResponse {
  status: string;
  user: AuthUser;
}

/** A registered user account managed by the govt / SaaS admin. */
export interface ManagedUser {
  id: number;
  username: string;
  email?: string | null;
  role: "citizen" | "health_worker" | string;
  is_active: boolean;
}

export interface Stats {
  users: { citizen: number; health_worker: number };
  total_users: number;
  patients: number;
  screenings: number;
  high_risk_screenings: number;
  vitals: number;
  referrals: number;
  pending_referrals: number;
  reports: number;
}

export interface Patient {
  id: string;
  name?: string;
  age?: number;
  gender?: string;
  contact?: string;
  [key: string]: unknown;
}

export interface VitalsRecord {
  id: string;
  patientId?: string;
  patient_id?: string;
  systolic?: number;
  diastolic?: number;
  heartRate?: number;
  temperature?: number;
  spo2?: number;
  [key: string]: unknown;
}

export interface SymptomNote {
  id: string;
  symptoms?: Record<string, unknown> | null;
  notes?: string | null;
}

export interface PatientRecord {
  patient: Patient;
  screenings: Screening[];
  vitals: VitalsRecord[];
  reports: Report[];
  referrals: Referral[];
  symptoms: SymptomNote[];
}

export interface Screening {
  id: string;
  patientId?: string;
  patient_id?: string;
  type?: string;
  result?: string;
  confidence?: number;
  details?: Record<string, unknown> | null;
  [key: string]: unknown;
}

export interface Report {
  id: string;
  patientId?: string;
  patient_id?: string;
  title?: string;
  summary?: string;
  createdAt?: string;
  created_at?: string;
  [key: string]: unknown;
}

export interface Referral {
  id: string;
  patientId?: string;
  patient_id?: string;
  status?: string;
  reason?: string;
  facility?: string;
  [key: string]: unknown;
}

export interface HealthTip {
  id: string;
  title: string;
  content: string;
  category: string;
}