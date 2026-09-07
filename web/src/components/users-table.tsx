"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import type { ManagedUser } from "@/lib/types";
import { Badge } from "@/components/ui";

export default function UsersTable({ users }: { users: ManagedUser[] }) {
  const router = useRouter();
  const [busyId, setBusyId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function toggleActive(user: ManagedUser) {
    setBusyId(user.id);
    setError(null);
    try {
      const res = await fetch(`/api/users/${user.id}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ is_active: !user.is_active }),
      });
      const data = (await res.json()) as { ok?: boolean; error?: string };
      if (!res.ok || !data.ok) {
        setError(data.error ?? "Update failed");
      } else {
        router.refresh();
      }
    } catch {
      setError("Network error.");
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div>
      {error ? <p className="mb-3 text-sm text-red-600">{error}</p> : null}
      {users.length === 0 ? (
        <p className="py-8 text-center text-sm text-slate-400">No accounts to show.</p>
      ) : (
        <ul className="divide-y divide-slate-100">
          {users.map((user) => (
            <li key={user.id} className="flex items-center justify-between py-3">
              <div className="flex items-center gap-3">
                <div className="flex h-9 w-9 items-center justify-center rounded-full bg-indigo-100 text-sm font-semibold text-indigo-700">
                  {(user.username[0] ?? "?").toUpperCase()}
                </div>
                <div>
                  <p className="flex items-center gap-2 text-sm font-medium text-slate-800">
                    {user.username}
                    {user.is_active ? <Badge>active</Badge> : <Badge>blocked</Badge>}
                  </p>
                  <p className="text-xs text-slate-400">{user.email ?? "no email on file"}</p>
                </div>
              </div>
              <button
                type="button"
                disabled={busyId === user.id}
                onClick={() => toggleActive(user)}
                className={`rounded-lg px-3 py-1.5 text-sm font-medium transition-colors disabled:opacity-50 ${
                  user.is_active
                    ? "text-red-600 hover:bg-red-50"
                    : "text-emerald-600 hover:bg-emerald-50"
                }`}
              >
                {busyId === user.id ? "…" : user.is_active ? "Block" : "Unblock"}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}