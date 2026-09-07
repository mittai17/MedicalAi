"use client";

import { useState } from "react";
import Link from "next/link";
import type { Referral } from "@/lib/types";
import { Badge, EmptyState } from "@/components/ui";

export default function ReferralsList({ referrals }: { referrals: Referral[] }) {
  const [items, setItems] = useState(referrals);
  const [busyId, setBusyId] = useState<string | null>(null);

  async function markCompleted(r: Referral) {
    setBusyId(String(r.id));
    try {
      const res = await fetch(`/api/referrals/${r.id}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ status: "completed" }),
      });
      const updated = (await res.json()) as Referral | { error?: string };
      if (res.ok && "status" in updated) {
        setItems((prev) =>
          prev.map((x) => (x.id === r.id ? { ...x, status: "completed" } : x)),
        );
      }
    } finally {
      setBusyId(null);
    }
  }

  const pending = items.filter((r) => r.status === "pending");
  const done = items.filter((r) => r.status !== "pending");

  function renderReferral(r: Referral) {
    return (
      <li
        key={String(r.id)}
        className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-100 px-4 py-3"
      >
        <div className="min-w-0">
          <p className="flex flex-wrap items-center gap-2 text-sm font-medium text-slate-800">
            {r.status === "pending" ? (
              <Badge>pending</Badge>
            ) : (
              <span className="inline-flex items-center rounded-full bg-emerald-50 px-2.5 py-0.5 text-xs font-semibold uppercase tracking-wide text-emerald-700 ring-1 ring-emerald-200">
                completed
              </span>
            )}
            <span>{r.facility ?? "Facility"}</span>
            <span className="text-xs font-normal text-slate-400">
              {r.id} · patient {String(r.patientId ?? r.patient_id)}
            </span>
          </p>
          <p className="mt-0.5 truncate text-sm text-slate-500">{r.reason ?? "No reason given"}</p>
        </div>
        <div className="flex items-center gap-2">
          <Link
            href={`/patients/${String(r.patientId ?? r.patient_id)}`}
            className="rounded-lg px-3 py-1.5 text-sm font-medium text-indigo-600 hover:bg-indigo-50"
          >
            Patient →
          </Link>
          {r.status === "pending" ? (
            <button
              type="button"
              disabled={busyId === String(r.id)}
              onClick={() => markCompleted(r)}
              className="rounded-lg bg-emerald-600 px-3 py-1.5 text-sm font-medium text-white transition-colors hover:bg-emerald-500 disabled:opacity-50"
            >
              {busyId === String(r.id) ? "…" : "Mark completed"}
            </button>
          ) : null}
        </div>
      </li>
    );
  }

  return (
    <div className="space-y-6">
      <section>
        <h3 className="mb-2 text-sm font-semibold text-slate-900">Need action ({pending.length})</h3>
        {pending.length === 0 ? (
          <EmptyState message="No pending referrals. All caught up." />
        ) : (
          <ul className="space-y-2">{pending.map(renderReferral)}</ul>
        )}
      </section>

      <section>
        <h3 className="mb-2 text-sm font-semibold text-slate-900">Completed ({done.length})</h3>
        {done.length === 0 ? (
          <EmptyState message="No completed referrals yet." />
        ) : (
          <ul className="space-y-2">{done.map(renderReferral)}</ul>
        )}
      </section>
    </div>
  );
}