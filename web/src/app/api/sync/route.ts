import { NextResponse } from "next/server";
import { ApiError, api } from "@/lib/api";
import { getSession } from "@/lib/auth";

export async function POST() {
  const session = await getSession();
  if (!session) {
    return NextResponse.json({ error: "Not authenticated" }, { status: 401 });
  }

  try {
    const batch = {
      items: [
        {
          type: "screening",
          payload: {
            id: `S-offline-${Date.now()}`,
            patientId: "P-1001",
            type: "SYMPTOM_CHECK",
            result: "Influenza (Flu)",
            confidence: 0.62,
          },
        },
      ],
    };
    const result = await api.syncBatch(session.token, batch);
    return NextResponse.json({ ok: true, result });
  } catch (err) {
    if (err instanceof ApiError) {
      return NextResponse.json({ error: err.message }, { status: err.status });
    }
    const message = err instanceof Error ? err.message : "Sync failed";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}