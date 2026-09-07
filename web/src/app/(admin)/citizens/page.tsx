import { api } from "@/lib/api";
import { getSession } from "@/lib/auth";
import AddUser from "@/components/add-user";
import UsersTable from "@/components/users-table";
import { Card } from "@/components/ui";

export default async function CitizensPage() {
  const session = await getSession();
  const token = session?.token ?? "";

  let citizens: Awaited<ReturnType<typeof api.getUsers>> = [];
  let error: string | null = null;

  try {
    citizens = await api.getUsers(token, "citizen");
  } catch (err) {
    error = err instanceof Error ? err.message : "Could not reach the backend";
  }

  return (
    <div className="space-y-6">
      <header className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Citizens</h1>
          <p className="text-sm text-slate-500">
            Community members who use the SwasthAI app for self-screening.
          </p>
        </div>
        <AddUser role="citizen" />
      </header>

      <Card>
        {error && citizens.length === 0 ? (
          <p className="text-sm text-amber-800">
            <span className="font-semibold">Backend unreachable: </span>
            {error}
          </p>
        ) : (
          <UsersTable users={citizens} />
        )}
      </Card>
    </div>
  );
}