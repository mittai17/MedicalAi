import { api } from "@/lib/api";
import { getSession } from "@/lib/auth";
import AddPatient from "@/components/add-patient";
import PatientsDirectory from "@/components/patients-directory";
import { Card } from "@/components/ui";

export default async function PatientsPage() {
  const session = await getSession();
  const token = session?.token ?? "";

  let patients: Awaited<ReturnType<typeof api.getPatients>> = [];
  let error: string | null = null;

  try {
    patients = await api.getPatients(token);
  } catch (err) {
    error = err instanceof Error ? err.message : "Could not reach the backend";
  }

  return (
    <div className="space-y-6">
      <header className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Patients</h1>
          <p className="text-sm text-slate-500">
            Beneficiary directory across all health workers. Open a record for the full
            clinical history.
          </p>
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