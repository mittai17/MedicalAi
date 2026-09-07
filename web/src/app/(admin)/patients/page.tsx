import Link from "next/link";
import { api } from "@/lib/api";
import { getSession } from "@/lib/auth";
import AddPatient from "@/components/add-patient";
import PatientsDirectory from "@/components/patients-directory";
import { Card } from "@/components/ui";

export default async function PatientsPage({
  searchParams,
}: {
  searchParams: Promise<{ worker?: string }>;
}) {
  const session = await getSession();
  const token = session?.token ?? "";
  const { worker } = await searchParams;

  let patients: Awaited<ReturnType<typeof api.getPatients>> = [];
  let error: string | null = null;

  try {
    patients = await api.getPatients(token);
  } catch (err) {
    error = err instanceof Error ? err.message : "Could not reach the backend";
  }

  if (worker) {
    patients = patients.filter((p) => String(p.workerId) === worker);
  }

  return (
    <div className="space-y-6">
      <header className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">
            Patients{worker ? " — worker filtered" : ""}
          </h1>
          <p className="text-sm text-slate-500">
            Benefit directory across all health workers. Open a record for the full
            clinical history.
          </p>
          {worker ? (
            <Link
              href="/patients"
              className="mt-2 inline-block text-xs font-medium text-indigo-600 hover:underline"
            >
              Clear worker filter
            </Link>
          ) : null}
        </div>
        <AddPatient />
      </header>

      <Card>
        {error && patients.length === 0 ? (
          <p className="text-sm text-amber-800">
            <span className="font-semibold">Backend unreachable: </span>
            {error}
          </p>
        ) : (
          <PatientsDirectory patients={patients} />
        )}
      </Card>
    </div>
  );
}