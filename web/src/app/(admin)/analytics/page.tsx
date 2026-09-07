import { api } from "@/lib/api";
import { getSession } from "@/lib/auth";
import type { Screening } from "@/lib/types";
import { Card, CardHeader } from "@/components/ui";

function Bar({
  label,
  value,
  max,
  color = "bg-indigo-500",
  suffix = "",
}: {
  label: string;
  value: number;
  max: number;
  color?: string;
  suffix?: string;
}) {
  const width = max > 0 ? Math.round((value / max) * 100) : 0;
  return (
    <div className="flex items-center gap-3">
      <span className="w-40 truncate text-right text-xs text-slate-500">{label}</span>
      <div className="h-5 flex-1 overflow-hidden rounded-md bg-slate-100">
        <div
          className={`h-full ${color}`}
          style={{ width: `${Math.max(width, value > 0 ? 4 : 0)}%` }}
        />
      </div>
      <span className="w-10 text-xs font-medium text-slate-700">
        {value}
        {suffix}
      </span>
    </div>
  );
}

function riskBucket(confidence?: number): string {
  if (confidence == null) return "unknown";
  if (confidence >= 0.7) return "high";
  if (confidence >= 0.4) return "medium";
  return "low";
}

export default async function AnalyticsPage() {
  const session = await getSession();
  const token = session?.token ?? "";

  let stats: Awaited<ReturnType<typeof api.getStats>> | null = null;
  let screenings: Screening[] = [];
  let workers: Awaited<ReturnType<typeof api.getUsers>> = [];
  let workerLoad: number[] = [];
  let error: string | null = null;

  try {
    const [s, sc, w] = await Promise.all([
      api.getStats(token),
      api.getScreenings(token),
      api.getUsers(token, "health_worker"),
    ]);
    stats = s;
    screenings = sc;
    workers = w;
    workerLoad = await Promise.all(
      w.map(async (worker) => (await api.getWorkerPatients(token, worker.id)).length),
    );
  } catch (err) {
    error = err instanceof Error ? err.message : "Could not reach the backend";
  }

  if (error && !stats) {
    return (
      <p className="text-sm text-amber-800">
        <span className="font-semibold">Backend unreachable: </span>
        {error}
      </p>
    );
  }

  const riskCounts: Record<string, number> = { low: 0, medium: 0, high: 0, unknown: 0 };
  const typeCounts: Record<string, number> = {};
  const conditionCounts: Record<string, number> = {};
  for (const s of screenings) {
    riskCounts[riskBucket(s.confidence)] += 1;
    const type = s.type ?? "UNKNOWN";
    typeCounts[type] = (typeCounts[type] ?? 0) + 1;
    const cond = (s.result ?? "Pending").trim();
    conditionCounts[cond] = (conditionCounts[cond] ?? 0) + 1;
  }

  const workerLoadPairs = workers.map((w, i) => ({ name: w.username, count: workerLoad[i] ?? 0 }));
  const maxWorkerLoad = Math.max(1, ...workerLoadPairs.map((x) => x.count));
  const maxRisk = Math.max(1, ...Object.values(riskCounts));
  const maxType = Math.max(1, ...Object.values(typeCounts));
  const maxCondition = Math.max(1, ...Object.values(conditionCounts));
  const maxRegistrations = Math.max(
    1,
    stats?.total_users ?? 0,
    stats?.patients ?? 0,
  );

  const pendingPct = stats && stats.referrals > 0
    ? Math.round((stats.pending_referrals / stats.referrals) * 100)
    : 0;

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-bold text-slate-900">Analytics</h1>
        <p className="text-sm text-slate-500">
          Aggregate view of screenings risk, caseload, and registrations.
        </p>
      </header>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader
            title="Screening risk distribution"
            subtitle="Confidence buckets across all screenings"
          />
          <div className="space-y-2">
            <Bar label="High risk (≥70%)" value={riskCounts.high} max={maxRisk} color="bg-red-500" />
            <Bar label="Medium (40–70%)" value={riskCounts.medium} max={maxRisk} color="bg-amber-500" />
            <Bar label="Low (<40%)" value={riskCounts.low} max={maxRisk} color="bg-emerald-500" />
            <Bar label="Unknown" value={riskCounts.unknown} max={maxRisk} color="bg-slate-300" />
          </div>
        </Card>

        <Card>
          <CardHeader title="Screenings by type" subtitle="How assessments were captured" />
          <div className="space-y-2">
            {Object.keys(typeCounts).length === 0 ? (
              <p className="text-sm text-slate-400">No screenings yet.</p>
            ) : (
              Object.entries(typeCounts)
                .sort((a, b) => b[1] - a[1])
                .map(([type, count]) => (
                  <Bar key={type} label={type} value={count} max={maxType} />
                ))
            )}
          </div>
        </Card>

        <Card>
          <CardHeader title="Conditions detected" subtitle="Most common screening results" />
          <div className="space-y-2">
            {Object.keys(conditionCounts).length === 0 ? (
              <p className="text-sm text-slate-400">No conditions recorded.</p>
            ) : (
              Object.entries(conditionCounts)
                .sort((a, b) => b[1] - a[1])
                .map(([cond, count]) => (
                  <Bar key={cond} label={cond} value={count} max={maxCondition} color="bg-violet-500" />
                ))
            )}
          </div>
        </Card>

        <Card>
          <CardHeader title="Health worker caseload" subtitle="Patients managed per worker" />
          <div className="space-y-2">
            {workerLoadPairs.length === 0 ? (
              <p className="text-sm text-slate-400">No health workers onboarded.</p>
            ) : (
              workerLoadPairs.map((w) => (
                <Bar key={w.name} label={w.name} value={w.count} max={maxWorkerLoad} color="bg-emerald-500" />
              ))
            )}
          </div>
        </Card>

        <Card>
          <CardHeader title="Registrations" subtitle="Accounts and beneficiaries on record" />
          <div className="space-y-2">
            <Bar label="Health workers" value={stats?.users.health_worker ?? 0} max={maxRegistrations} color="bg-emerald-500" />
            <Bar label="Citizens" value={stats?.users.citizen ?? 0} max={maxRegistrations} color="bg-sky-500" />
            <Bar label="Patients" value={stats?.patients ?? 0} max={maxRegistrations} color="bg-indigo-500" />
          </div>
        </Card>

        <Card>
          <CardHeader title="Referral closure" subtitle={`${stats?.pending_referrals ?? 0} pending of ${stats?.referrals ?? 0} total`} />
          <div className="space-y-2">
            <Bar label="Pending" value={stats?.pending_referrals ?? 0} max={Math.max(1, stats?.referrals ?? 1)} color="bg-red-500" />
            <Bar
              label="Completed"
              value={(stats?.referrals ?? 0) - (stats?.pending_referrals ?? 0)}
              max={Math.max(1, stats?.referrals ?? 1)}
              color="bg-emerald-500"
            />
          </div>
          <p className="mt-4 text-sm text-slate-500">
            {pendingPct}% of referrals are still awaiting action.
          </p>
        </Card>
      </div>
    </div>
  );
}