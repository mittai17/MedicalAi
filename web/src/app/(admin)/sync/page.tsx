import SyncQueue from "@/components/sync-queue";

export default function SyncPage() {
  return (
    <div className="space-y-4">
      <header>
        <h1 className="text-2xl font-bold text-slate-900">Offline Sync</h1>
        <p className="text-sm text-slate-500">
          Monitor batches coming in from offline health-worker devices.
        </p>
      </header>
      <SyncQueue />
    </div>
  );
}