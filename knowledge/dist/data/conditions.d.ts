import type { Condition } from "../types.js";
/**
 * Clinical conditions with evidence-weighted symptom profiles.
 * Ported verbatim from ClinicalKnowledge.CONDITIONS.
 */
export declare const CONDITIONS: Condition[];
export declare const CONDITION_BY_ID: ReadonlyMap<string, Condition>;
/** Red-flag symptoms that escalate risk regardless of match score. */
export declare const RED_FLAG_SYMPTOMS: ReadonlySet<string>;
/** Duration multipliers — prolonged symptoms raise evidence and risk. */
export declare function durationMultiplier(duration: string | undefined): number;
export declare function durationRiskEscalation(duration: string | undefined): boolean;
//# sourceMappingURL=conditions.d.ts.map