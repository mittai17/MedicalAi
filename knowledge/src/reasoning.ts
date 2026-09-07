import type {
  Condition,
  MedicalAdvice,
  PatientContext,
  ReasoningOutput,
  Recommendation,
  RiskLevel,
  Symptom,
  Vitals,
} from "./types.js";
import { CONDITIONS, RED_FLAG_SYMPTOMS, durationMultiplier, durationRiskEscalation } from "./data/conditions.js";
import { adviceFor } from "./data/diseaseKnowledge.js";
import { canonicalizeSymptom } from "./canonicalize.js";
import type { RagRetriever } from "./rag.js";

export interface ReasonParams {
  symptoms: Symptom[];
  vitals?: Vitals | null;
  diagnosisId?: string;
  patientContext?: PatientContext | null;
  ragRetriever?: RagRetriever;
}

/**
 * Clinical reasoning engine — the AI doctor's text reasoning.
 *
 * Transparent, evidence-based computation ported from the Android app:
 *  - Every reported symptom contributes weighted evidence to candidate
 *    conditions (see data/conditions.ts).
 *  - Evidence is accumulated per condition and normalised into a real
 *    confidence score (a normalised posterior over the top-3 plausible set),
 *    not a fabricated number.
 *  - Vitals are checked against real clinical danger thresholds.
 *  - Risk and recommendations are derived from evidence + vitals, never
 *    hardcoded per-case.
 */
export function reason(params: ReasonParams): ReasoningOutput {
  const { diagnosisId = cryptoRandomId() } = params;
  const symptoms = params.symptoms ?? [];
  const vitals = params.vitals ?? null;
  const patientContext = params.patientContext ?? null;

  // Accumulate canonical evidence from all symptoms.
  const evidence: Record<string, number> = {};
  for (const symptom of symptoms) {
    const mult = durationMultiplier(symptom.duration);
    for (const id of canonicalizeSymptom(symptom.name)) {
      evidence[id] = (evidence[id] ?? 0) + mult;
    }
  }

  const evidenceTotal = Math.max(1, Object.values(evidence).reduce((a, b) => a + b, 0));

  const scored: Array<{ condition: Condition; f1: number }> = [];
  for (const condition of CONDITIONS) {
    const totalWeight = Object.values(condition.symptoms).reduce((a, b) => a + b, 0);
    if (totalWeight <= 0) continue;
    let matched = 0;
    for (const [symId, weight] of Object.entries(condition.symptoms)) {
      const present = evidence[symId] ?? 0;
      if (present > 0) matched += weight;
    }
    const precision = matched / totalWeight;
    if (precision > 0.05) {
      const completeness = matched / evidenceTotal;
      const f1 =
        precision + completeness > 0
          ? (2 * precision * completeness) / (precision + completeness)
          : 0;
      scored.push({ condition, f1 });
    }
  }

  if (scored.length === 0) {
    return noMatch(symptoms, vitals, diagnosisId, params.ragRetriever);
  }

  const ranked = [...scored].sort((a, b) => b.f1 - a.f1);
  const top = ranked[0]!;
  const plausible = ranked.slice(0, 3);
  const plausibleTotal = plausible.reduce((a, p) => a + p.f1, 0);
  const confidence = Math.min(0.99, Math.max(0.1, top.f1 / (plausibleTotal || 1)));

  const topCondition = top.condition;

  const redFlagHits = Object.keys(evidence).some((k) => RED_FLAG_SYMPTOMS.has(k));
  const vitalsRisk = assessVitalsRisk(vitals);
  const prolonged = symptoms.some((s) => durationRiskEscalation(s.duration));
  const contextEscalation = contextEscalations(evidence, vitals, patientContext);
  const riskLevel = computeRisk(topCondition, redFlagHits, vitalsRisk, prolonged, contextEscalation);

  const differential = ranked.slice(1).map((r) => r.condition.name).slice(0, 4);

  return {
    predictedDisease: topCondition.name,
    advice: adviceFor(topCondition.name),
    confidence,
    riskLevel,
    differentialDiagnosis: differential,
    recommendations: buildRecommendations(diagnosisId, topCondition, riskLevel, evidence, vitals, vitalsRisk),
  };
}

/** Real clinical vitals danger assessment (returns 0..2 escalation). */
export function assessVitalsRisk(vitals: Vitals | null | undefined): number {
  if (!vitals) return 0;
  let risk = 0;
  if (vitals.temperature !== undefined) {
    if (vitals.temperature >= 40) risk = Math.max(risk, 2);
    else if (vitals.temperature >= 39) risk = Math.max(risk, 1);
    else if (vitals.temperature <= 35) risk = Math.max(risk, 1);
  }
  if (vitals.spo2 !== undefined) {
    if (vitals.spo2 < 90) risk = Math.max(risk, 2);
    else if (vitals.spo2 < 94) risk = Math.max(risk, 1);
  }
  if (vitals.pulse !== undefined) {
    if (vitals.pulse > 120) risk = Math.max(risk, 2);
    else if (vitals.pulse > 100) risk = Math.max(risk, 1);
    else if (vitals.pulse < 50) risk = Math.max(risk, 1);
  }
  return risk;
}

/**
 * Patient-context risk escalation (0..1). Only escalates on real facts:
 * age extremes (≤5 or ≥60), or a known chronic condition aggravated by the
 * reported presentation.
 */
export function contextEscalations(
  evidence: Record<string, number>,
  vitals: Vitals | null | undefined,
  patientContext: PatientContext | null | undefined,
): number {
  if (!patientContext) return 0;
  let escalation = 0;

  const age = patientContext.age;
  if (age != null && (age <= 5 || age >= 60)) escalation = 1;

  const conditions = (patientContext.chronicConditions ?? []).join(" ").toLowerCase();

  const hasHypertension =
    conditions.includes("hypertension") || conditions.includes("high bp") || conditions.includes("high blood pressure");
  const hasDiabetes = conditions.includes("diabetes") || conditions.includes("sugar");
  const hasLungDisease = conditions.includes("asthma") || conditions.includes("copd");
  const hasHeartDisease = conditions.includes("heart") || conditions.includes("cardiac");

  if (hasHypertension) {
    const raw = vitals?.bloodPressure ?? "";
    const parts = raw.split("/");
    const sys = parseInt((parts[0] ?? "").trim(), 10);
    const dia = parseInt((parts[1] ?? "").trim(), 10);
    if ((!Number.isNaN(sys) && sys >= 160) || (!Number.isNaN(dia) && dia >= 100)) escalation = 1;
  }
  if (hasDiabetes && (evidence["polyuria"] || evidence["irregular_sugar_level"])) escalation = 1;
  if (hasLungDisease && (evidence["breathing_difficulty"] || evidence["wheezing"])) escalation = 1;
  if (
    hasHeartDisease &&
    (evidence["chest_pain"] || evidence["palpitations"] || evidence["breathing_difficulty"])
  ) {
    escalation = 1;
  }

  return escalation;
}

export function computeRisk(
  condition: Condition,
  redFlag: boolean,
  vitalsRisk: number,
  prolonged: boolean,
  contextEscalation = 0,
): RiskLevel {
  const base = condition.baseRisk === "high" ? 3 : condition.baseRisk === "moderate" ? 2 : 1;
  let level = base;
  if (condition.redFlag || redFlag) level = Math.max(level, 3);
  level = Math.max(level, 1 + vitalsRisk);
  if (prolonged) level = Math.min(level + 1, 3);
  level = Math.min(level + contextEscalation, 3);
  if (level >= 3) return "high";
  if (level === 2) return "moderate";
  return "low";
}

/** Recommendations are derived from evidence + vitals, not canned per case. */
export function buildRecommendations(
  diagnosisId: string,
  condition: Condition,
  riskLevel: RiskLevel,
  evidence: Record<string, number>,
  vitals: Vitals | null | undefined,
  vitalsRisk: number,
): Recommendation[] {
  const recs: Recommendation[] = [];
  let priority = 0;

  const add = (text: string, category: string) => {
    recs.push({ id: cryptoRandomId(), diagnosisId, text, category, priority: priority++ });
  };

  if (vitalsRisk >= 2) {
    add(
      "Your vitals show a danger sign (low oxygen / very high fever / very fast pulse). Seek emergency care now.",
      "emergency",
    );
  } else if (vitalsRisk >= 1) {
    add("Your vitals are outside normal range — see a doctor promptly for evaluation.", "urgent");
  }
  if (evidence["breathing_difficulty"] && vitals?.spo2 == null) {
    add("Breathing difficulty with unmeasured oxygen — get your oxygen level checked urgently.", "urgent");
  }

  switch (riskLevel) {
    case "high":
      add("Seek immediate medical attention at the nearest health facility.", "urgent");
      break;
    case "moderate":
      add("Visit a health centre within 24 hours for proper evaluation.", "action");
      break;
    case "low":
      add("Monitor symptoms and seek care if they worsen.", "monitoring");
      break;
  }

  switch (condition.id) {
    case "gastroenteritis":
    case "food_poisoning":
      add("Take oral rehydration solution (ORS) and sip fluids frequently to prevent dehydration.", "care");
      break;
    case "dehydration":
      add("Rehydrate with ORS or water; rest and avoid exertion until symptoms settle.", "care");
      break;
    case "dengue":
    case "malaria":
    case "typhoid":
    case "viral_fever":
      add("Rest, hydrate well, and monitor your temperature. Get a blood test as your doctor advises.", "care");
      break;
    case "pneumonia":
    case "covid19":
    case "respiratory_infection":
    case "influenza":
    case "upper_respiratory":
      add("Rest, drink warm fluids, and avoid smoking. A doctor may prescribe antibiotics only for bacterial infection.", "care");
      break;
    case "asthma":
      add("Use your prescribed inhaler, avoid triggers, and keep your reliever medication accessible.", "medication");
      break;
    case "dermatitis":
    case "urticaria":
      add("Avoid the trigger, keep the area clean and moisturised, and use antihistamine only as advised.", "care");
      break;
    case "anemia":
      add("Eat iron-rich foods (leafy greens, lentils, iron-fortified food) and get a blood count test.", "care");
      break;
    case "tension_headache":
    case "migraine":
      add("Rest in a quiet, dark room, stay hydrated and manage stress; simple pain relief only as advised.", "care");
      break;
  }

  if (riskLevel !== "low") {
    add("If symptoms worsen, visit a doctor without delay.", "followup");
  }
  return recs;
}

/** Honest "cannot determine" outcome — never fabricates a condition. */
export function noMatch(
  symptoms: Symptom[],
  vitals: Vitals | null | undefined,
  diagnosisId: string,
  ragRetriever?: RagRetriever,
): ReasoningOutput {
  const vitalsRisk = assessVitalsRisk(vitals);
  const risk: RiskLevel = vitalsRisk >= 2 ? "high" : vitalsRisk >= 1 ? "moderate" : "low";
  const recs: Recommendation[] = [];
  if (vitalsRisk >= 2) {
    recs.push({
      id: cryptoRandomId(),
      diagnosisId,
      text: "Your vitals show a danger sign. Seek emergency care now.",
      category: "emergency",
      priority: 0,
    });
  }
  recs.push({
    id: cryptoRandomId(),
    diagnosisId,
    text: "Your symptoms are not clearly recognised — consult a doctor for proper evaluation.",
    category: "monitoring",
    priority: 1,
  });

  let advice: MedicalAdvice = adviceFor("undifferentiated illness");
  if (ragRetriever) {
    const query = symptoms.map((s) => s.name).join(" ");
    const ragDoc = ragRetriever.retrieve(query, 1)[0];
    if (ragDoc) {
      advice = {
        condition: ragDoc.topic,
        cause: `Based on the reported symptoms, the closest guidance in the local health library relates to ${ragDoc.topic}.`,
        remedy: ragDoc.content,
        doctorToConsult: "General Physician",
        urgencyHint: "Consult a doctor for a full evaluation.",
      };
    }
  }

  return {
    predictedDisease: "Undifferentiated Illness",
    advice,
    confidence: 0,
    riskLevel: risk,
    differentialDiagnosis: [],
    recommendations: recs,
  };
}

function cryptoRandomId(): string {
  if (typeof globalThis !== "undefined") {
    const g = globalThis as unknown as { crypto?: { randomUUID?: () => string } };
    if (g.crypto && typeof g.crypto.randomUUID === "function") {
      return g.crypto.randomUUID();
    }
  }
  return `diag-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}