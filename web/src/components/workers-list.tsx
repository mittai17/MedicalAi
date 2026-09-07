"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import type { ManagedUser, Patient } from "@/lib/types";
import { Badge } from "@/components/ui";

export default function WorkersList({ workers }: { workers: ManagedUser[] }) {
  const router = useRouter();
  const [expanded, setExpanded] = useState<number | null>(null);
  const [patients, setPatients] = useState<Patient[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);

  async function togglePatients(userId: number) {
    if (expanded === userId) {
      setExpanded(null);
      return;
    }
    setExpanded(userId);
    setError(null);
    setLoading(true);
    try {
      const res = await fetch(`/api/users/${userId}/patients`);
      const data = (await res.json()) as Patient[] | { error?: string };
      if (!Array.isArray(data)) {
        setError(data.error ?? "Could not load patients");
        setPatients([]);
      } else {
        setPatients(data);
      }
    } catch {
      setError("Network error.");
      setPatients([]);
    } finally {
      setLoading(false);
    }
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
    <div>
      {error ? <p className="mb-3 text-sm text-red-600">{error}</p> : null}
      {workers.length === 0 ? (
        <p className="py-8 text-center text-sm text-slate-400">No health workers onboarded.</p>
      ) : (
        <ul className="divide-y divide-slate-100">
          {workers.map((worker) => (
            <li key={worker.id}>
              <div className="flex items-center justify-between py-3">
                <button
                  type="button"
                  onClick={() => togglePatients(worker.id)}
                  className="flex items-center gap-3 text-left"
                >
                  <div className="flex h-9 w-9 items-center justify-center rounded-full bg-emerald-100 text-sm font-semibold text-emerald-700">
                    {(worker.username[0] ?? "?").toUpperCase()}
                  </div>
                  <div>
                    <p className="flex items-center gap-2 text-sm font-medium text-slate-800">
                      {worker.username}
                      {worker.is_active ? <Badge>active</Badge> : <Badge>blocked</Badge>}
                    </p>
                    <p className="text-xs text-slate-400">
                      {worker.email ?? "no email on file"}
                    </p>
                  </div>
                </button>
                <div className="flex items-center gap-2">
                  {expanded === worker.id ? (
                    <button
                      type="button"
                      onClick={() => setExpanded(null)}
                      className="rounded-lg px-3 py-1.5 text-sm font-medium text-slate-500 hover:bg-slate-100"
                    >
                      Hide patients
                    </button>
                  ) : null}
                  <button
                    type="button"
                    disabled={busyId === worker.id}
                    onClick={() => toggleActive(worker)}
                    className={`rounded-lg px-3 py-1.5 text-sm font-medium transition-colors disabled:opacity-50 ${
                      worker.is_active
                        ? "text-red-600 hover:bg-red-50"
                        : "text-emerald-600 hover:bg-emerald-50"
                    }`}
                  >
                    {busyId === worker.id ? "…" : worker.is_active ? "Block" : "Unblock"}
                  </button>
                </div>
              </div>

              {expanded === worker.id ? (
                <div className="mb-3 rounded-xl bg-slate-50 p-4">
                  <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-400">
                    Patients managed
                  </p>
                  {loading ? (
                    <p className="text-sm text-slate-400">Loading…</p>
                  ) : patients.length === 0 ? (
                    <p className="text-sm text-slate-400">No patients on record.</p>
                  ) : (
                    <ul className="space-y-1.5">
                      {patients.map((p) => (
                        <li key={String(p.id)} className="flex items-center justify-between text-sm">
                          <span className="text-slate-700">{p.name ?? "Unnamed patient"}</span>
                          <span className="text-xs text-slate-400">
                            {p.age != null ? `${p.age} yrs` : ""}
                            {p.gender ? ` · ${p.gender}` : ""}
                          </span>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              ) : null}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}