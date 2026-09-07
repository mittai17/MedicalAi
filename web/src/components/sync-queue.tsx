"use client";

import { useState } from "react";
import { Button } from "@/components/ui";

interface SyncResult {
  ok?: boolean;
  error?: string;
  result?: { status?: string; processed_count?: number; errors?: string[] };
}

export default function SyncQueue() {
  const [result, setResult] = useState<SyncResult | null>(null);
  const [loading, setLoading] = useState(false);

  async function push() {
    setLoading(true);
    setResult(null);
    try {
      const res = await fetch("/api/sync", { method: "POST" });
      const data = (await res.json()) as SyncResult;
      setResult(data);
    } catch {
      setResult({ ok: false, error: "Network error during sync." });
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="space-y-4">
      <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h3 className="text-base font-semibold text-slate-900">Pending offline queue</h3>
        <p className="mt-1 text-sm text-slate-500">
          Health-worker devices queue screening batches while offline. This demo pushes one
          sample batch to the backend so you can watch the sync pipeline.
        </p>
        <ul className="mt-3 space-y-1 text-sm">
          <li className="flex items-center justify-between rounded-lg bg-slate-50 px-3 py-2">
            <span className="text-slate-700">Offline screening · Influenza (Flu)</span>
            <span className="text-xs font-medium text-amber-600">queued</span>
          </li>
        </ul>
        <Button onClick={push} disabled={loading} className="mt-4">
          {loading ? "Syncing…" : "Push pending batches"}
        </Button>
      </div>

      {result ? (
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          {result.error ? (
            <p className="text-sm text-red-600">{result.error}</p>
          ) : (
            <p className="text-sm text-slate-700">
              Batch {result.result?.status ?? "sent"} —{" "}
              {result.result?.processed_count ?? 0} item(s) processed
              {result.result?.errors?.length ? `, ${result.result.errors.length} error(s)` : ""}.
            </p>
          )}
        </div>
      ) : null}
    </div>
  );
}