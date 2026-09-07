import { api } from "@/lib/api";
import { getSession } from "@/lib/auth";
import { Card, CardHeader, EmptyState, RiskBadge } from "@/components/ui";
import type { RiskLevel } from "@/components/ui";

function riskLevel(screening: { confidence?: number }): RiskLevel {
  const c = screening.confidence ?? 0;
  if (c >= 0.7) return "high";
  if (c >= 0.4) return "medium";
  return "low";
}

export default async function ScreeningsPage() {
  const session = await getSession();
  const token = session?.token ?? "";

  let screenings: Awaited<ReturnType<typeof api.getScreenings>> = [];
  let error: string | null = null;

  try {
    screenings = await api.getScreenings(token);
  } catch (err) {
    error = err instanceof Error ? err.message : "Could not reach the backend";
  }

  if (error && screenings.length === 0) {
    return (
      <Card>
        <p className="text-sm text-amber-800">
          <span className="font-semibold">Backend unreachable: </span>
          {error}
        </p>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-bold text-slate-900">Screening Feed</h1>
        <p className="text-sm text-slate-500">
          All screenings reported to the backend, flagged by risk.
        </p>
      </header>

      <Card>
        <CardHeader title="Activity" subtitle={`${screenings.length} screenings`} />
        {screenings.length === 0 ? (
          <EmptyState message="No screenings reported yet." />
        ) : (
          <ul className="divide-y divide-slate-100">
            {screenings.map((s) => (
              <li key={String(s.id)} className="flex items-center justify-between py-3">
                <div>
                  <p className="text-sm font-medium text-slate-800">{s.result ?? "Unknown result"}</p>
                  <p className="text-xs text-slate-400">
                    {s.type ?? "Screening"}
                    {s.patient_id || s.patientId ? ` · patient ${s.patient_id ?? s.patientId}` : ""}
                  </p>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-xs text-slate-400">
                    conf. {s.confidence != null ? Math.round(s.confidence * 100) : "—"}%
                  </span>
                  <RiskBadge level={riskLevel(s)} />
                </div>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  );
}