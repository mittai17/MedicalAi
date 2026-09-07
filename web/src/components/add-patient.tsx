"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Button, Input } from "@/components/ui";

export default function AddPatient() {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [name, setName] = useState("");
  const [age, setAge] = useState("");
  const [gender, setGender] = useState("female");
  const [contact, setContact] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const id = `P-${Date.now().toString(36).toUpperCase()}`;
      const res = await fetch("/api/patients", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          id,
          name,
          age: age ? Number(age) : null,
          gender,
          contact: contact || undefined,
          "registration-source": "govt-admin",
          enrolled: new Date().toISOString().slice(0, 10),
        }),
      });
      const data = (await res.json()) as { status?: string; error?: string };
      if (!res.ok || data.status !== "success") {
        setError(data.error ?? "Could not register patient");
        setLoading(false);
        return;
      }
      setName("");
      setAge("");
      setContact("");
      setOpen(false);
      router.push(`/patients/${id}`);
    } catch {
      setError("Network error.");
      setLoading(false);
    }
  }

  if (!open) {
    return <Button onClick={() => setOpen(true)}>Register patient</Button>;
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="space-y-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm"
    >
      <div className="grid gap-4 sm:grid-cols-2">
        <Input
          label="Full name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
          placeholder="e.g. Kavitha Rao"
        />
        <Input
          label="Age"
          type="number"
          min={0}
          max={130}
          value={age}
          onChange={(e) => setAge(e.target.value)}
          placeholder="e.g. 34"
        />
        <label className="block">
          <span className="mb-1 block text-sm font-medium text-slate-700">Gender</span>
          <select
            value={gender}
            onChange={(e) => setGender(e.target.value)}
            className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/40"
          >
            <option value="female">Female</option>
            <option value="male">Male</option>
            <option value="other">Other</option>
          </select>
        </label>
        <Input
          label="Contact (optional)"
          value={contact}
          onChange={(e) => setContact(e.target.value)}
          placeholder="e.g. 98xxxxx09"
        />
      </div>

      {error ? <p className="text-sm text-red-600">{error}</p> : null}

      <div className="flex gap-2">
        <Button type="submit" disabled={loading}>
          {loading ? "Saving…" : "Register patient"}
        </Button>
        <Button type="button" variant="secondary" onClick={() => setOpen(false)} disabled={loading}>
          Cancel
        </Button>
      </div>
    </form>
  );
}