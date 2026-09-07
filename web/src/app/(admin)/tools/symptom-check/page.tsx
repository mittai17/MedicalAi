import SymptomCheck from "@/components/symptom-check";

export default function ToolsSymptomCheckPage() {
  return (
    <div className="space-y-4">
      <header>
        <h1 className="text-2xl font-bold text-slate-900">Symptom Check</h1>
        <p className="text-sm text-slate-500">
          Verify a reported screening with the same transparent reasoning engine the app uses
          (port of the Android clinical logic).
        </p>
      </header>
      <SymptomCheck />
    </div>
  );
}