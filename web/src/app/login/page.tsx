"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Button, Input } from "@/components/ui";

export default function LoginPage() {
  const router = useRouter();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const res = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password }),
      });
      const data = (await res.json()) as { ok?: boolean; error?: string };
      if (!res.ok || !data.ok) {
        setError(data.error ?? "Login failed");
        setLoading(false);
        return;
      }
      router.push("/overview");
      router.refresh();
    } catch {
      setError("Network error. Is the backend running?");
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100 px-4">
      <div className="w-full max-w-md">
        <div className="mb-6 text-center">
          <p className="text-xs font-semibold uppercase tracking-wider text-indigo-600">
            SwasthAI Dashboard
          </p>
          <h1 className="mt-1 text-2xl font-bold text-slate-900">Govt / SaaS Admin Console</h1>
          <p className="mt-1 text-sm text-slate-500">
            Manage citizens, health workers and screening activity
          </p>
        </div>

        <form
          onSubmit={handleSubmit}
          className="space-y-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm"
        >
          <Input
            label="Username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
            autoComplete="username"
            placeholder="e.g. asha"
          />
          <Input
            label="Password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            autoComplete="current-password"
          />

          {error ? <p className="text-sm text-red-600">{error}</p> : null}

          <Button type="submit" disabled={loading} className="w-full">
            {loading ? "Signing in…" : "Sign in"}
          </Button>

          <p className="text-xs text-slate-400">
            Any username/password works. Demo accounts: asha, pragya, vijay (workers);
            meera, raman, lakshmi (citizens). Set{" "}
            <code className="rounded bg-slate-100 px-1">NEXT_PUBLIC_API_URL</code> to point at the
            FastAPI backend (default <code className="rounded bg-slate-100 px-1">http://127.0.0.1:8000/api/v1</code>).
          </p>
        </form>
      </div>
    </div>
  );
}