"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import type { ManagedUser, Patient } from "@/lib/types";
import { Badge } from "@/components/ui";

export default function WorkersList({
  workers,
  patients,
}: {
  workers: ManagedUser[];
  patients: Patient[];
}) {
  const router = useRouter();
  const [busyId, setBusyId] = useState<number | null>(null);

  const byWorker = new Map<string, Patient[]>();
  for (const p of patients) {
    const key = String(p.workerId ?? "");
    if (!key) continue;
    byWorker.set(key, [...(byWorker.get(key) ?? []), p]);
  }

  async function toggleActive(user: ManagedUser) {
    setBusyId(user.id);
    try {
      await fetch(`/api/users/${user.id}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ is_active: !user.is_active }),
      });
      router.refresh();
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-sm">
        <thead>
          <tr className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-400">
            <th className="py-2 pr-4 font-semibold">Worker</th>
            <th className="py-2 pr-4 font-semibold">Email</th>
            <th className="py-2 pr-4 font-semibold">Status</th>
            <th className="py-2 pr-4 font-semibold">Patients</th>
            <th className="py-2 pr-4 font-semibold">Villages covered</th>
            <th className="py-2 text-right font-semibold">Action</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {workers.map((worker) => {
            const assigned = byWorker.get(String(worker.id)) ?? [];
            const villages = [
              ...new Set(
                assigned.map((p) => String(p.village ?? "")).filter(Boolean),
              ),
            ];
            return (
              <tr key={worker.id} className="hover:bg-slate-50">
                <td className="py-3 pr-4">
                  <div className="flex items-center gap-3">
                    <div className="flex h-9 w-9 items-center justify-center rounded-full bg-emerald-100 text-sm font-semibold text-emerald-700">
                      {(worker.username[0] ?? "?").toUpperCase()}
                    </div>
                    <span className="font-medium text-slate-800">{worker.username}</span>
                  </div>
                </td>
                <td className="py-3 pr-4 text-slate-500">{worker.email ?? "—"}</td>
                <td className="py-3 pr-4">
                  {worker.is_active ? <Badge>active</Badge> : <Badge>blocked</Badge>}
                </td>
                <td className="py-3 pr-4 text-slate-700">{assigned.length}</td>
                <td className="py-3 pr-4 text-slate-500">
                  {villages.length > 0 ? villages.join(", ") : "—"}
                </td>
                <td className="py-3 text-right">
                  {assigned.length > 0 ? (
                    <Link
                      href={`/patients?worker=${worker.id}`}
                      className="rounded-lg px-3 py-1.5 text-sm font-medium text-indigo-600 hover:bg-indigo-50"
                    >
                      View cases →
                    </Link>
                  ) : null}
                  <button
                    type="button"
                    disabled={busyId === worker.id}
                    onClick={() => toggleActive(worker)}
                    className={`ml-2 rounded-lg px-3 py-1.5 text-sm font-medium transition-colors disabled:opacity-50 ${
                      worker.is_active
                        ? "text-red-600 hover:bg-red-50"
                        : "text-emerald-600 hover:bg-emerald-50"
                    }`}
                  >
                    {busyId === worker.id ? "…" : worker.is_active ? "Block" : "Unblock"}
                  </button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
      {workers.length === 0 ? (
        <p className="py-8 text-center text-sm text-slate-400">No health workers onboarded.</p>
      ) : null}
    </div>
  );
}