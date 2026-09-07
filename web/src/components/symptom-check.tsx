"use client";

import { useState } from "react";
import { SYMPTOMS, type ReasoningOutput, type RiskLevel } from "@swasthai/knowledge";
import { Button, Card, CardHeader, Input, RiskBadge } from "@/components/ui";

const QUICK_IDS = [
  "fever",
  "cough",
  "sore_throat",
  "runny_nose",
  "headache",
  "body_aches",
  "fatigue",
  "breathing_difficulty",
  "chest_pain",
  "wheezing",
  "nausea",
  "vomiting",
  "diarrhea",
  "abdominal_pain",
  "loss_appetite",
  "rash",
];

const quickSet = new Set(QUICK_IDS);
const SYMPTOM_OPS = SYMPTOMS.map((s) => ({ id: s.id, display: s.display }));
const quick = SYMPTOM_OPS.filter((s) => quickSet.has(s.id));
const rest = SYMPTOM_OPS.filter((s) => !quickSet.has(s.id));

const DURATIONS = [
  { value: "", label: "No info" },
  { value: "1 – 3 days", label: "1–3 days" },
  { value: "3 – 7 days", label: "3–7 days" },
  { value: "More than 7 days", label: "More than a week" },
];

export default function SymptomCheck() {
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [durations, setDurations] = useState<Record<string, string>>({});
  const [age, setAge] = useState("");
  const [temperature, setTemperature] = useState("");
  const [spo2, setSpo2] = useState("");
  const [pulse, setPulse] = useState("");
  const [result, setResult] = useState<ReasoningOutput | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  function toggle(id: string) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  const names = SYMPTOM_OPS.filter((s) => selected.has(s.id)).map((s) => ({
    name: s.display,
    duration: durations[s.display],
  }));

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    const vitals: Record<string, number> = {};
    if (temperature) vitals.temperature = Number(temperature);
    if (spo2) vitals.spo2 = Number(spo2);
    if (pulse) vitals.pulse = Number(pulse);
    try {
      const res = await fetch("/api/check", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          symptoms: names,
          vitals,
          patientContext: age ? { age: Number(age) } : null,
        }),
      });
      const data = await res.json();
      if (!res.ok) {
        setError((data as { error?: string }).error ?? "Check failed");
        setResult(null);
        return;
      }
      setResult(data as ReasoningOutput);
    } catch {
      setError("Network error while running the check.");
      setResult(null);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="space-y-6">
      <form onSubmit={handleSubmit} className="space-y-6">
        <Card>
          <CardHeader
            title="Select symptoms"
            subtitle="Run the same clinical reasoning engine the Android app uses, to verify a screening."
          />
          <fieldset className="space-y-4">
            <legend className="sr-only">Symptoms</legend>
            <div className="flex flex-wrap gap-2">
              {quick.map((s) => (
                <button
                  key={s.id}
                  type="button"
                  onClick={() => toggle(s.id)}
                  className={`rounded-full border px-3 py-1.5 text-sm font-medium transition-colors ${
                    selected.has(s.id)
                      ? "border-indigo-600 bg-indigo-600 text-white"
                      : "border-slate-300 bg-white text-slate-700 hover:bg-slate-50"
                  }`}
                >
                  {s.display}
                </button>
              ))}
              {rest.map((s) => (
                <button
                  key={s.id}
                  type="button"
                  onClick={() => toggle(s.id)}
                  className={`rounded-full border px-3 py-1.5 text-sm font-medium transition-colors ${
                    selected.has(s.id)
                      ? "border-indigo-600 bg-indigo-600 text-white"
                      : "border-slate-300 bg-white text-slate-700 hover:bg-slate-50"
                  }`}
                >
                  {s.display}
                </button>
              ))}
            </div>
          </fieldset>
          {names.length > 0 ? (
            <div className="mt-4 space-y-2 border-t border-slate-100 pt-4">
              <p className="text-sm font-medium text-slate-700">Selected symptoms with duration:</p>
              {names.map((s) => (
                <div key={s.name} className="flex items-center gap-3">
                  <span className="w-48 text-sm text-slate-800">{s.name}</span>
                  <select
                    value={durations[s.name] ?? ""}
                    onChange={(e) =>
                      setDurations((prev) => ({ ...prev, [s.name]: e.target.value }))
                    }
                    className="rounded-lg border border-slate-300 bg-white px-2 py-1.5 text-sm text-slate-700 focus:border-indigo-500 focus:outline-none"
                  >
                    {DURATIONS.map((d) => (
                      <option key={d.value} value={d.value}>
                        {d.label}
                      </option>
                    ))}
                  </select>
                </div>
              ))}
            </div>
          ) : null}
        </Card>

        <Card>
          <CardHeader title="Vitals (optional)" subtitle="Enter what you know; blank fields are skipped." />
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <Input
              label="Age (years)"
              type="number"
              min={0}
              max={120}
              value={age}
              onChange={(e) => setAge(e.target.value)}
              placeholder="e.g. 34"
            />
            <Input
              label="Temperature (°C)"
              type="number"
              step="0.1"
              value={temperature}
              onChange={(e) => setTemperature(e.target.value)}
              placeholder="e.g. 38.5"
            />
            <Input
              label="Oxygen SpO₂ (%)"
              type="number"
              min={50}
              max={100}
              value={spo2}
              onChange={(e) => setSpo2(e.target.value)}
              placeholder="e.g. 96"
            />
            <Input
              label="Pulse (bpm)"
              type="number"
              min={30}
              max={220}
              value={pulse}
              onChange={(e) => setPulse(e.target.value)}
              placeholder="e.g. 78"
            />
          </div>
        </Card>

        {error ? <p className="text-sm text-red-600">{error}</p> : null}

        <Button type="submit" disabled={loading || selected.size === 0}>
          {loading ? "Analyzing…" : "Run symptom check"}
        </Button>
      </form>

      {result ? <ResultPanel result={result} /> : null}
    </div>
  );
}

const riskFrom = (r: RiskLevel) =>
  r === "high" ? "high" : r === "moderate" ? "medium" : "low";

function ResultPanel({ result }: { result: ReasoningOutput }) {
  const recs = [...result.recommendations].sort((a, b) => a.priority - b.priority);
  const categories: Record<string, string> = {
    emergency: "text-red-700",
    urgent: "text-orange-600",
    action: "text-amber-600",
    monitoring: "text-slate-600",
    care: "text-emerald-700",
    medication: "text-violet-700",
    followup: "text-slate-500",
  };

  return (
    <Card className="border-indigo-200">
      <div className="flex flex-wrap items-center gap-3">
        <h2 className="text-lg font-semibold text-slate-900">{result.predictedDisease}</h2>
        <RiskBadge level={riskFrom(result.riskLevel)} />
        <span className="text-xs text-slate-400">
          confidence {(result.confidence * 100).toFixed(0)}%
        </span>
      </div>

      <div className="mt-4 space-y-4 text-sm">
        <div>
          <h3 className="font-medium text-slate-700">What this might be</h3>
          <p className="mt-1 text-slate-600">{result.advice.cause}</p>
        </div>
        <div>
          <h3 className="font-medium text-slate-700">What to do</h3>
          <p className="mt-1 text-slate-600">{result.advice.remedy}</p>
        </div>
        <div>
          <h3 className="font-medium text-slate-700">Consult</h3>
          <p className="mt-1 text-slate-600">
            {result.advice.doctorToConsult} · {result.advice.urgencyHint}
          </p>
        </div>

        {result.differentialDiagnosis.length > 0 ? (
          <div>
            <h3 className="font-medium text-slate-700">Other possibilities</h3>
            <ul className="mt-1 space-y-1">
              {result.differentialDiagnosis.map((d) => (
                <li key={d} className="text-slate-600">• {d}</li>
              ))}
            </ul>
          </div>
        ) : null}

        <div>
          <h3 className="font-medium text-slate-700">Recommended steps</h3>
          <ul className="mt-1 space-y-1.5">
            {recs.map((rec) => (
              <li key={rec.id} className="flex items-start gap-2">
                <span
                  className={`mt-0.5 font-medium ${categories[rec.category] ?? "text-slate-600"}`}
                >
                  {rec.category}:
                </span>
                <span className="text-slate-600">{rec.text}</span>
              </li>
            ))}
          </ul>
        </div>
      </div>

      <p className="mt-4 border-t border-slate-100 pt-3 text-xs text-slate-400">
        Screening is a decision-support aid, not a medical diagnosis. Seek professional care
        for persistent or severe symptoms.
      </p>
    </Card>
  );
}