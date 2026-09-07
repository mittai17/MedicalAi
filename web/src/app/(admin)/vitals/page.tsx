import { api } from "@/lib/api";
import { getSession } from "@/lib/auth";
import VitalsFeed from "@/components/vitals-feed";
import { Card } from "@/components/ui";

export default async function VitalsPage() {
  const session = await getSession();
  const token = session?.token ?? "";

  let vitals: Awaited<ReturnType<typeof api.getVitals>> = [];
  let patients: Awaited<ReturnType<typeof api.getPatients>> = [];
  let error: string | null = null;

  try {
    [vitals, patients] = await Promise.all([
      api.getVitals(token),
      api.getPatients(token),
    ]);
  } catch (err) {
    error = err instanceof Error ? err.message : "Could not reach the backend";
  }

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-bold text-slate-900">Vitals</h1>
        <p className="text-sm text-slate-500">
          Field readings streamed from health workers. Out-of-range values are flagged.
        </p>
      </header>

      <Card>
        {error && vitals.length === 0 ? (
          <p className="text-sm text-amber-800">
            <span className="font-semibold">Backend unreachable: </span>
            {error}
          </p>
        ) : (
          <VitalsFeed vitals={vitals} patients={patients} />
        )}
      </Card>
    </div>
  );
}