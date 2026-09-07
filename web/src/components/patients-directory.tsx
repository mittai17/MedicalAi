"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import type { Patient } from "@/lib/types";
import { EmptyState } from "@/components/ui";

export default function PatientsDirectory({ patients }: { patients: Patient[] }) {
  const [query, setQuery] = useState("");

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return patients;
    return patients.filter(
      (p) =>
        String(p.id).toLowerCase().includes(q) ||
        String(p.name ?? "").toLowerCase().includes(q) ||
        String(p.gender ?? "").toLowerCase().includes(q) ||
        String(p.enrolled ?? "").toLowerCase().includes(q),
    );
  }, [patients, query]);

  function exportCsv() {
    const header = ["id", "name", "age", "gender", "contact"];
    const rows = filtered.map((p) =>
      header.map((key) => {
        const value = String(p[key] ?? "").replaceAll('"', '""');
        return `"${value}"`;
      }),
    );
    const csv = [header.join(","), ...rows.map((r) => r.join(","))].join("\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "patients.csv";
    a.click();
    URL.revokeObjectURL(url);
  }

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search by name, ID, gender…"
          className="min-w-0 flex-1 rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 placeholder:text-slate-400 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
        />
        <button
          type="button"
          onClick={exportCsv}
          disabled={filtered.length === 0}
          className="inline-flex items-center justify-center rounded-lg bg-white px-4 py-2 text-sm font-medium text-slate-700 ring-1 ring-inset ring-slate-300 transition-colors hover:bg-slate-50 disabled:cursor-not-allowed disabled:text-slate-400"
        >
          Export CSV
        </button>
      </div>

      <p className="mb-3 text-xs text-slate-400">
        {filtered.length} of {patients.length} patients
      </p>

      {filtered.length === 0 ? (
        <EmptyState message="No patients match your search." />
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-400">
                <th className="py-2 pr-4 font-semibold">ID</th>
                <th className="py-2 pr-4 font-semibold">Name</th>
                <th className="py-2 pr-4 font-semibold">Age</th>
                <th className="py-2 pr-4 font-semibold">Gender</th>
                <th className="py-2 pr-4 font-semibold">Enrolled</th>
                <th className="py-2 font-semibold" />
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filtered.map((p) => (
                <tr key={String(p.id)} className="hover:bg-slate-50">
                  <td className="py-2.5 pr-4 font-mono text-xs text-indigo-600">{String(p.id)}</td>
                  <td className="py-2.5 pr-4 font-medium text-slate-800">{p.name ?? "—"}</td>
                  <td className="py-2.5 pr-4 text-slate-600">
                    {p.age != null ? `${p.age} yrs` : "—"}
                  </td>
                  <td className="py-2.5 pr-4 capitalize text-slate-600">{p.gender ?? "—"}</td>
                  <td className="py-2.5 pr-4 text-slate-500">
                    {p.enrolled != null ? String(p.enrolled) : "—"}
                  </td>
                  <td className="py-2.5 text-right">
                    <Link
                      href={`/patients/${p.id}`}
                      className="rounded-lg px-3 py-1.5 text-sm font-medium text-indigo-600 hover:bg-indigo-50"
                    >
                      Records →
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}