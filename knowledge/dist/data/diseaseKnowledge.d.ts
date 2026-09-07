import type { DiseaseAdvice } from "../types.js";
/**
 * AI Doctor knowledge base — maps every condition the screening models can
 * predict (image findings, tissue/cell types, symptom-based diagnoses) to the
 * three answers a doctor gives: WHY it happens (cause), WHAT to do (remedy)
 * and WHO to see (doctor to consult).
 *
 * Screened content, not a diagnosis. Ported from the Android app's
 * DiseaseKnowledgeBase. Entries are keyed by the same normalized names and
 * kept in insertion order so adviceFor() behaves identically.
 */
export declare const DISEASE_ADVICE: Readonly<Record<string, DiseaseAdvice>>;
/**
 * Look up medical advice for a predicted condition.
 * Matching is case-insensitive and tolerant of the small label variants used
 * across models (e.g. "Adenocarcinoma" vs "CA Stroma"). Faithful port of
 * DiseaseKnowledgeBase.adviceFor().
 */
export declare function adviceFor(condition: string): DiseaseAdvice;
/**
 * Organ identification (CT organ models) names an organ region, not a disease.
 * Return honest, non-diagnostic guidance for those labels.
 */
export declare function organAdvice(organ: string): DiseaseAdvice;
export declare function fallbackAdvice(condition: string): DiseaseAdvice;
//# sourceMappingURL=diseaseKnowledge.d.ts.map