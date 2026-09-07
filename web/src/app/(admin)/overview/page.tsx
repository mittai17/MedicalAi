import Link from "next/link";
import { api } from "@/lib/api";
import { getSession } from "@/lib/auth";
import { Card, CardHeader, StatCard } from "@/components/ui";

export default async function OverviewPage() {
  const session = await getSession();
  const token = session?.token ?? "";

  let stats: Awaited<ReturnType<typeof api.getStats>> | null = null;
  let referrals: Awaited<ReturnType<typeof api.getReferrals>> = [];
  let error: string | null = null;

  try {
    [stats, referrals] = await Promise.all([
      api.getStats(token),
      api.getReferrals(token),
    ]);
  } catch (err) {
    error = err instanceof Error ? err.message : "Could not reach the backend";
  }

  const pending = referrals.filter((r) => (r.status ?? "").toLowerCase() === "pending");

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-bold text-slate-900">Programme Overview</h1>
        <p className="text-sm text-slate-500">
          Population-level view of citizens, health workers and screening activity.
        </p>
      </header>

      {error && !stats ? (
        <Card className="border-amber-300 bg-amber-50">
          <p className="text-sm text-amber-800">
            <span className="font-semibold">Backend unreachable: </span>
            {error}. Start it with{" "}
            <code className="rounded bg-amber-100 px-1">uvicorn backend.main:app</code> from the repo root.
          </p>
        </Card>
      ) : null}

      {stats ? (
        <>
          <div className="grid gap-4 sm:grid-cols-3 lg:grid-cols-6">
            <StatCard label="Citizens" value={stats.users.citizen} hint="registered accounts" />
            <StatCard label="Health workers" value={stats.users.health_worker} hint="field clinicians" />
            <StatCard label="Patients" value={stats.patients} hint="in the registry" />
            <StatCard label="Screenings" value={stats.screenings} hint="total performed" />
            <StatCard label="High-risk flags" value={stats.high_risk_screenings} hint="conf. ≥ 70%" />
            <StatCard label="Vitals" value={stats.vitals} hint="field readings" />
            <StatCard label="Referrals" value={stats.referrals} hint={`${stats.pending_referrals} pending`} />
            <StatCard label="Reports" value={stats.reports} hint="on file" />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <Card>
              <CardHeader title="Manage users" subtitle="Onboard or suspend citizen and worker accounts." />
              <div className="flex gap-2">
                <Link
                  href="/citizens"
                  className="inline-flex items-center justify-center rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-500"
                >
                  Citizens
                </Link>
                <Link
                  href="/workers"
                  className="inline-flex items-center justify-center rounded-lg bg-white px-4 py-2 text-sm font-medium text-slate-700 ring-1 ring-inset ring-slate-300 hover:bg-slate-50"
                >
                  Health workers
                </Link>
                <Link
                  href="/patients"
                  className="inline-flex items-center justify-center rounded-lg bg-white px-4 py-2 text-sm font-medium text-slate-700 ring-1 ring-inset ring-slate-300 hover:bg-slate-50"
                >
                  Patients
                </Link>
              </div>
            </Card>
            <Card>
              <CardHeader title="Clinical tools" subtitle="Inspect results, vitals and verify with the reasoning engine." />
              <div className="flex gap-2">
                <Link
                  href="/screenings"
                  className="inline-flex items-center justify-center rounded-lg bg-white px-4 py-2 text-sm font-medium text-slate-700 ring-1 ring-inset ring-slate-300 hover:bg-slate-50"
                >
                  Screening feed
                </Link>
                <Link
                  href="/vitals"
                  className="inline-flex items-center justify-center rounded-lg bg-white px-4 py-2 text-sm font-medium text-slate-700 ring-1 ring-inset ring-slate-300 hover:bg-slate-50"
                >
                  Vitals
                </Link>
                <Link
                  href="/referrals"
                  className="inline-flex items-center justify-center rounded-lg bg-white px-4 py-2 text-sm font-medium text-slate-700 ring-1 ring-inset ring-slate-300 hover:bg-slate-50"
                >
                  Referrals
                </Link>
                <Link
                  href="/tools/symptom-check"
                  className="inline-flex items-center justify-center rounded-lg bg-white px-4 py-2 text-sm font-medium text-slate-700 ring-1 ring-inset ring-slate-300 hover:bg-slate-50"
                >
                  Symptom check
                </Link>
              </div>
            </Card>
          </div>
        </>
      ) : null}

      {pending.length > 0 ? (
        <Card>
          <CardHeader title="Pending referrals" subtitle="High-risk cases needing follow-up" />
          <ul className="space-y-2">
            {pending.map((r) => (
              <li key={String(r.id)} className="flex items-center justify-between rounded-lg bg-slate-50 px-4 py-3">
                <div>
                  <p className="text-sm font-medium text-slate-800">{r.reason ?? "Referral"}</p>
                  <p className="text-xs text-slate-400">to {r.facility ?? "a facility"}</p>
                </div>
                <span className="text-xs font-medium text-amber-600">pending</span>
              </li>
            ))}
          </ul>
        </Card>
      ) : null}
    </div>
  );
}