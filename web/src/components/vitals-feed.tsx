"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import type { Patient, VitalsRecord } from "@/lib/types";
import { Badge, EmptyState } from "@/components/ui";

export default function VitalsFeed({
  vitals,
  patients,
}: {
  vitals: VitalsRecord[];
  patients: Patient[];
}) {
  const [onlyFlagged, setOnlyFlagged] = useState(false);

  const byId = useMemo(
    () => new Map(patients.map((p) => [String(p.id), p])),
    [patients],
  );

  const flagged = useMemo(
    () =>
      vitals.filter(
        (v) =>
          (v.temperature ?? 36.6) >= 38 || (v.spo2 ?? 98) < 94 || (v.heartRate ?? 72) > 110,
      ),
    [vitals],
  );

  const shown = onlyFlagged ? flagged : vitals;

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <p className="text-xs text-slate-400">
          {flagged.length} of {vitals.length} readings need attention
        </p>
        <label className="flex items-center gap-2 text-sm text-slate-600">
          <input
            type="checkbox"
            checked={onlyFlagged}
            onChange={(e) => setOnlyFlagged(e.target.checked)}
            className="h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
          />
          Only flagged readings
        </label>
      </div>

      {shown.length === 0 ? (
        <EmptyState message="No vitals readings to show." />
      ) : (
        <ul className="space-y-2">
          {shown.map((v) => {
            const patient = byId.get(String(v.patientId ?? v.patient_id));
            const tempAnomaly = (v.temperature ?? 36.6) >= 38;
            const spo2Anomaly = (v.spo2 ?? 98) < 94;
            const pulseAnomaly = (v.heartRate ?? 72) > 110;
            const anyAnomaly = tempAnomaly || spo2Anomaly || pulseAnomaly;
            return (
              <li
                key={String(v.id)}
                className={`rounded-xl border px-4 py-3 ${
                  anyAnomaly ? "border-red-100 bg-red-50/60" : "border-slate-100 bg-white"
                }`}
              >
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="flex items-center gap-2">
                    {anyAnomaly ? <Badge>flagged</Badge> : null}
                    <Link
                      href={`/patients/${String(v.patientId ?? v.patient_id)}`}
                      className="text-sm font-medium text-indigo-600 hover:underline"
                    >
                      {patient?.name ?? String(v.patientId ?? v.patient_id)}
                    </Link>
                    <span className="text-xs text-slate-400">{v.id}</span>
                  </div>
                  <span className="font-mono text-xs text-slate-400">
                    {v.readingDate != null
                      ? String(v.readingDate)
                      : v.timestamp != null
                        ? String(v.timestamp)
                        : ""}
                  </span>
                </div>
                <div className="mt-2 grid grid-cols-2 gap-3 text-sm sm:grid-cols-4">
                  <div>
                    <p className="text-xs text-slate-400">Temperature</p>
                    <p className={`font-medium ${tempAnomaly ? "text-red-700" : "text-slate-700"}`}>
                      {v.temperature ?? "—"} °C
                    </p>
                  </div>
                  <div>
                    <p className="text-xs text-slate-400">Blood pressure</p>
                    <p className="font-medium text-slate-700">
                      {v.systolic ?? "—"}/{v.diastolic ?? "—"}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs text-slate-400">Pulse</p>
                    <p className={`font-medium ${pulseAnomaly ? "text-red-700" : "text-slate-700"}`}>
                      {v.heartRate ?? "—"} bpm
                    </p>
                  </div>
                  <div>
                    <p className="text-xs text-slate-400">SpO₂</p>
                    <p className={`font-medium ${spo2Anomaly ? "text-red-700" : "text-slate-700"}`}>
                      {v.spo2 ?? "—"} %
                    </p>
                  </div>
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}