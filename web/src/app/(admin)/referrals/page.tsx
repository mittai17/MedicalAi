import { api } from "@/lib/api";
import { getSession } from "@/lib/auth";
import ReferralsList from "@/components/referrals-list";
import { Card } from "@/components/ui";

export default async function ReferralsPage() {
  const session = await getSession();
  const token = session?.token ?? "";

  let referrals: Awaited<ReturnType<typeof api.getReferrals>> = [];
  let error: string | null = null;

  try {
    referrals = await api.getReferrals(token);
  } catch (err) {
    error = err instanceof Error ? err.message : "Could not reach the backend";
  }

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-bold text-slate-900">Referrals</h1>
        <p className="text-sm text-slate-500">
          Track citizens escalated to higher facilities and close them out once handled.
        </p>
      </header>

      <Card>
        {error && referrals.length === 0 ? (
          <p className="text-sm text-amber-800">
            <span className="font-semibold">Backend unreachable: </span>
            {error}
          </p>
        ) : (
          <ReferralsList referrals={referrals} />
        )}
      </Card>
    </div>
  );
}