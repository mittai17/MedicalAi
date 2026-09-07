import type { Condition, PatientContext, ReasoningOutput, Recommendation, RiskLevel, Symptom, Vitals } from "./types.js";
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
export declare function reason(params: ReasonParams): ReasoningOutput;
/** Real clinical vitals danger assessment (returns 0..2 escalation). */
export declare function assessVitalsRisk(vitals: Vitals | null | undefined): number;
/**
 * Patient-context risk escalation (0..1). Only escalates on real facts:
 * age extremes (≤5 or ≥60), or a known chronic condition aggravated by the
 * reported presentation.
 */
export declare function contextEscalations(evidence: Record<string, number>, vitals: Vitals | null | undefined, patientContext: PatientContext | null | undefined): number;
export declare function computeRisk(condition: Condition, redFlag: boolean, vitalsRisk: number, prolonged: boolean, contextEscalation?: number): RiskLevel;
/** Recommendations are derived from evidence + vitals, not canned per case. */
export declare function buildRecommendations(diagnosisId: string, condition: Condition, riskLevel: RiskLevel, evidence: Record<string, number>, vitals: Vitals | null | undefined, vitalsRisk: number): Recommendation[];
/** Honest "cannot determine" outcome — never fabricates a condition. */
export declare function noMatch(symptoms: Symptom[], vitals: Vitals | null | undefined, diagnosisId: string, ragRetriever?: RagRetriever): ReasoningOutput;
//# sourceMappingURL=reasoning.d.ts.map