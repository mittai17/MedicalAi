import { api } from "@/lib/api";
import { getSession } from "@/lib/auth";
import AddUser from "@/components/add-user";
import WorkersList from "@/components/workers-list";
import { Card } from "@/components/ui";

export default async function WorkersPage() {
  const session = await getSession();
  const token = session?.token ?? "";

  let workers: Awaited<ReturnType<typeof api.getUsers>> = [];
  let patients: Awaited<ReturnType<typeof api.getPatients>> = [];
  let error: string | null = null;

  try {
    [workers, patients] = await Promise.all([
      api.getUsers(token, "health_worker"),
      api.getPatients(token),
    ]);
  } catch (err) {
    error = err instanceof Error ? err.message : "Could not reach the backend";
  }

  return (
    <div className="space-y-6">
      <header className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Health Workers</h1>
          <p className="text-sm text-slate-500">
            Field clinicians who run screenings on citizens, with their caseload.
          </p>
        </div>
        <AddUser role="health_worker" />
      </header>

      <Card>
        {error && workers.length === 0 ? (
          <p className="text-sm text-amber-800">
            <span className="font-semibold">Backend unreachable: </span>
            {error}
          </p>
        ) : (
          <WorkersList workers={workers} patients={patients} />
        )}
      </Card>
    </div>
  );
}