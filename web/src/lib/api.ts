import type {
  HealthTip,
  LoginResponse,
  ManagedUser,
  Patient,
  PatientRecord,
  Referral,
  RegisterResponse,
  Report,
  Screening,
  Stats,
  SyncBatchResult,
  VitalsRecord,
} from "./types";

const BASE =
  process.env.NEXT_PUBLIC_API_URL ?? "http://127.0.0.1:8000/api/v1";

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    ...init,
    cache: "no-store",
    headers: {
      "Content-Type": "application/json",
      ...init.headers,
    },
  });
  if (!res.ok) {
    let message = `Request failed with ${res.status}`;
    try {
      const body = (await res.json()) as { detail?: string; message?: string };
      message = body.detail ?? body.message ?? message;
    } catch {
      // non-JSON error body
    }
    throw new ApiError(res.status, message);
  }
  return res.json() as Promise<T>;
}

export const api = {
  BASE,

  // ── Auth ──
  login(username: string, password: string): Promise<LoginResponse> {
    return request("/auth/login", {
      method: "POST",
      body: JSON.stringify({ username, password }),
    });
  },

  register(
    username: string,
    password: string,
    role: string,
    email?: string,
  ): Promise<RegisterResponse> {
    return request("/auth/register", {
      method: "POST",
      body: JSON.stringify({ username, password, email, role }),
    });
  },

  // ── User management (govt / SaaS admin) ──
  getUsers(token: string, role?: "citizen" | "health_worker"): Promise<ManagedUser[]> {
    return request(`/users${role ? `?role=${role}` : ""}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  patchUser(
    token: string,
    id: number,
    update: { role?: string; is_active?: boolean },
  ): Promise<ManagedUser> {
    return request(`/users/${id}`, {
      method: "PATCH",
      headers: { Authorization: `Bearer ${token}` },
      body: JSON.stringify(update),
    });
  },

  getWorkerPatients(token: string, userId: number): Promise<Patient[]> {
    return request(`/users/${userId}/patients`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  getStats(token: string): Promise<Stats> {
    return request("/stats", { headers: { Authorization: `Bearer ${token}` } });
  },

  // ── Clinical data ──
  getPatients(token: string): Promise<Patient[]> {
    return request("/patients", {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  getPatient(token: string, id: string): Promise<Patient> {
    return request(`/patients/${id}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  getPatientRecords(token: string, id: string): Promise<PatientRecord> {
    return request(`/patients/${id}/records`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  addPatient(token: string, input: Record<string, unknown>): Promise<{ status: string }> {
    return request("/patients", {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
      body: JSON.stringify(input),
    });
  },

  getScreenings(token: string): Promise<Screening[]> {
    return request("/screenings", {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  getVitals(token: string): Promise<VitalsRecord[]> {
    return request("/vitals", {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  getReports(token: string): Promise<Report[]> {
    return request("/reports", {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  getReferrals(token: string): Promise<Referral[]> {
    return request("/referrals", {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  patchReferral(token: string, id: string, update: Partial<Pick<Referral, "status" | "reason" | "facility">>): Promise<Referral> {
    return request(`/referrals/${id}`, {
      method: "PATCH",
      headers: { Authorization: `Bearer ${token}` },
      body: JSON.stringify(update),
    });
  },

  getPendingReferralCount(token: string): Promise<number> {
    return request("/referrals/pending", {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  getHealthTips(token: string): Promise<HealthTip[]> {
    return request("/health-tips", {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  syncBatch(token: string, batch: unknown): Promise<SyncBatchResult> {
    return request("/sync/batch", {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
      body: JSON.stringify(batch),
    });
  },
};