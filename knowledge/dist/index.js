/**
 * @swasthai/knowledge — SwasthAI clinical knowledge base & reasoning engine.
 *
 * A self-contained TypeScript package shared across SwasthAI platforms.
 * Pure port of the Android app's AI engine (ClinicalKnowledge,
 * DiseaseKnowledgeBase, ClinicalReasoningEngine, ScanType, LocalRagRetriever)
 * so dashboards and web UIs can run the same transparent, evidence-based
 * screening logic.
 */
export * from "./types.js";
export * from "./canonicalize.js";
export * from "./reasoning.js";
export * from "./rag.js";
export { SYMPTOMS, SYMPTOM_BY_ID, } from "./data/symptoms.js";
export { CONDITIONS, CONDITION_BY_ID, RED_FLAG_SYMPTOMS, durationMultiplier, durationRiskEscalation, } from "./data/conditions.js";
export { SCAN_TYPES, SCAN_TYPE_BY_KEY, } from "./data/scanTypes.js";
export { DISEASE_ADVICE, adviceFor, organAdvice, fallbackAdvice, } from "./data/diseaseKnowledge.js";
export { RAG_KNOWLEDGE, RAG_KNOWLEDGE_VERSION, RAG_CATEGORIES, } from "./data/ragKnowledge.js";
import { RAG_KNOWLEDGE } from "./data/ragKnowledge.js";
import { InMemoryRagRetriever } from "./rag.js";
/** Default retriever over the bundled general & health knowledge documents. */
export const defaultRagRetriever = new InMemoryRagRetriever(RAG_KNOWLEDGE);
