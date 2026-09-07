import { NextResponse } from "next/server";
import {
  defaultRagRetriever,
  reason,
  type Symptom,
  type Vitals,
  type PatientContext,
} from "@swasthai/knowledge";

export const runtime = "nodejs";

export async function POST(req: Request) {
  try {
    const body = (await req.json()) as {
      symptoms?: Symptom[];
      vitals?: Vitals;
      patientContext?: PatientContext | null;
    };
    const symptoms = (body.symptoms ?? []).filter(
      (s) => typeof s?.name === "string" && s.name.trim().length > 0,
    );

    if (symptoms.length === 0) {
      return NextResponse.json({ error: "Select at least one symptom" }, { status: 400 });
    }

    const result = reason({
      symptoms,
      vitals: body.vitals ?? {},
      patientContext: body.patientContext ?? null,
      ragRetriever: defaultRagRetriever,
    });

    return NextResponse.json(result);
  } catch (err) {
    const message = err instanceof Error ? err.message : "Reasoning failed";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}