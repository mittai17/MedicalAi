import Link from "next/link";
import { notFound } from "next/navigation";
import { api } from "@/lib/api";
import { getSession } from "@/lib/auth";
import type { PatientRecord } from "@/lib/types";
import type { RiskLevel } from "@/components/ui";
import { Badge, Card, CardHeader, RiskBadge } from "@/components/ui";

function riskFor(confidence?: number): RiskLevel {
  if (confidence == null) return "unknown";
  if (confidence >= 0.7) return "high";
  if (confidence >= 0.4) return "medium";
  return "low";
}

function VitalsTable({ records }: { records: PatientRecord["vitals"] }) {
  return (
    <Card>
      <CardHeader title="Vitals" subtitle="Latest measurements recorded in the field" />
      {records.length === 0 ? (
        <p className="text-sm text-slate-400">No vitals recorded.</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-400">
                <th className="py-2 pr-4 font-semibold">ID</th>
                <th className="py-2 pr-4 font-semibold">Temp (°C)</th>
                <th className="py-2 pr-4 font-semibold">BP</th>
                <th className="py-2 pr-4 font-semibold">Pulse</th>
                <th className="py-2 pr-4 font-semibold">SpO₂ (%)</th>
                <th className="py-2 font-semibold">Flag</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {records.map((v) => {
                const anomaly =
                  (v.temperature ?? 36.6) >= 38 ||
                  (v.spo2 ?? 98) < 94 ||
                  (v.heartRate ?? 72) > 110;
                return (
                  <tr key={String(v.id)} className="hover:bg-slate-50">
                    <td className="py-2.5 pr-4 font-mono text-xs text-slate-500">{v.id}</td>
                    <td className="py-2.5 pr-4 text-slate-700">{v.temperature ?? "—"}</td>
                    <td className="py-2.5 pr-4 text-slate-700">
                      {v.systolic ?? "—"}/{v.diastolic ?? "—"}
                    </td>
                    <td className="py-2.5 pr-4 text-slate-700">{v.heartRate ?? "—"}</td>
                    <td className="py-2.5 pr-4 text-slate-700">{v.spo2 ?? "—"}</td>
                    <td className="py-2.5">
                      {anomaly ? (
                        <Badge>attention</Badge>
                      ) : (
                        <span className="text-xs text-slate-400">normal</span>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  );
}

function ScreeningsCard({ records }: { records: PatientRecord["screenings"] }) {
  return (
    <Card>
      <CardHeader title="Screenings" subtitle="AI-assisted assessments run on the app" />
      {records.length === 0 ? (
        <p className="text-sm text-slate-400">No screenings on record.</p>
      ) : (
        <ul className="space-y-3">
          {records.map((s) => (
            <li
              key={String(s.id)}
              className="flex items-center justify-between rounded-xl border border-slate-100 bg-slate-50 px-4 py-3"
            >
              <div>
                <p className="text-sm font-medium text-slate-800">
                  {s.result ?? "No result"}
                  <span className="ml-2 text-xs font-normal text-slate-400">{s.type}</span>
                </p>
                <p className="mt-0.5 text-xs text-slate-500">
                  {Array.isArray(s.symptoms)
                    ? (s.symptoms as string[]).join(", ")
                    : s.voiceTranscript
                      ? `Voice: ${String(s.voiceTranscript)}`
                      : "No symptom details"}
                </p>
              </div>
              <RiskBadge level={riskFor(s.confidence)} />
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}

export default async function PatientDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const session = await getSession();
  const token = session?.token ?? "";

  let record: PatientRecord | null = null;
  let error: string | null = null;
  try {
    record = await api.getPatientRecords(token, id);
  } catch (err) {
    error = err instanceof Error ? err.message : "Could not load the patient record";
  }

  if (error && !record) {
    if (error.toLowerCase().includes("not found")) {
      notFound();
    }
  }
  if (!record) {
    return (
      <p className="text-sm text-amber-800">
        <span className="font-semibold">Could not load record: </span>
        {error}
      </p>
    );
  }

  const patient = record.patient;
  const pendingReferrals = record.referrals.filter((r) => r.status === "pending");

  return (
    <div className="space-y-6">
      <Link href="/patients" className="text-sm font-medium text-indigo-600 hover:underline">
        ← Back to patients
      </Link>

      <Card>
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="font-mono text-xs text-slate-400">{patient.id}</p>
            <h1 className="mt-0.5 text-2xl font-bold text-slate-900">
              {patient.name ?? "Unnamed patient"}
            </h1>
            <p className="mt-1 text-sm text-slate-500">
              {patient.age != null ? `${patient.age} yrs` : "Age unknown"}
              {patient.gender ? ` · ${patient.gender}` : ""}
              {patient.contact ? ` · ${patient.contact}` : ""}
              {patient.enrolled ? ` · enrolled ${patient.enrolled}` : ""}
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <Badge>{record.screenings.length} screenings</Badge>
            <Badge>{record.vitals.length} vitals</Badge>
            {pendingReferrals.length > 0 ? <Badge>{pendingReferrals.length} referrals</Badge> : null}
          </div>
        </div>
      </Card>

      <VitalsTable records={record.vitals} />
      <ScreeningsCard records={record.screenings} />

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader title="Reports" subtitle="Lab / diagnostic reports" />
          {record.reports.length === 0 ? (
            <p className="text-sm text-slate-400">No reports tagged to this patient.</p>
          ) : (
            <ul className="space-y-3">
              {record.reports.map((r) => (
                <li key={String(r.id)} className="rounded-xl border border-slate-100 px-4 py-3">
                  <p className="flex items-center justify-between text-sm font-medium text-slate-800">
                    {r.title ?? "Clinical report"}
                    <span className="text-xs font-normal text-slate-400">{r.createdAt ?? r.created_at ?? ""}</span>
                  </p>
                  {r.summary ? <p className="mt-1 text-sm text-slate-500">{r.summary}</p> : null}
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card>
          <CardHeader title="Referrals" subtitle="Follow-up actions to higher facilities" />
          {record.referrals.length === 0 ? (
            <p className="text-sm text-slate-400">No referrals for this patient.</p>
          ) : (
            <ul className="space-y-3">
              {record.referrals.map((r) => (
                <li key={String(r.id)} className="rounded-xl border border-slate-100 px-4 py-3">
                  <p className="flex items-center justify-between text-sm font-medium text-slate-800">
                    {r.facility ?? "Facility"}
                    <RiskBadge level={r.status === "pending" ? "high" : "low"} />
                  </p>
                  <p className="mt-1 text-sm text-slate-500">{r.reason ?? ""}</p>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>

      {record.symptoms.length > 0 ? (
        <Card>
          <CardHeader title="Symptom notes" subtitle="Free-form notes captured offline" />
          <ul className="space-y-2">
            {record.symptoms.map((s) => (
              <li key={String(s.id)} className="text-sm text-slate-600">
                {s.notes || JSON.stringify(s.symptoms ?? {})}
              </li>
            ))}
          </ul>
        </Card>
      ) : null}
    </div>
  );
}