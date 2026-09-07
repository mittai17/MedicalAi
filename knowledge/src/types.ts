/**
 * Core domain types for the SwasthAI knowledge base and reasoning engine.
 *
 * These mirror the Android app's domain model (RiskLevel, Vitals,
 * MedicalAdvice, Recommendation, PatientContext, Symptom).
 */

export type RiskLevel = "low" | "moderate" | "high";

export interface Symptom {
  name: string;
  duration?: string;
}

export interface Vitals {
  temperature?: number;
  spo2?: number;
  pulse?: number;
  bloodPressure?: string;
}

export interface PatientContext {
  age?: number;
  chronicConditions?: string[];
}

export interface MedicalAdvice {
  condition: string;
  cause: string;
  remedy: string;
  doctorToConsult: string;
  urgencyHint: string;
}

export interface Recommendation {
  id: string;
  diagnosisId: string;
  text: string;
  category: string;
  priority: number;
}

export interface ReasoningOutput {
  predictedDisease: string;
  advice: MedicalAdvice;
  confidence: number;
  riskLevel: RiskLevel;
  differentialDiagnosis: string[];
  recommendations: Recommendation[];
}

/** Canonical symptom definition in the knowledge base. */
export interface SymptomDef {
  id: string;
  display: string;
}

/** A clinical condition with its evidence-weighted symptom profile. */
export interface Condition {
  id: string;
  name: string;
  baseRisk: RiskLevel;
  redFlag?: boolean;
  symptoms: Record<string, number>;
}

/**
 * Image-based screening model registry entry (port of the app's ScanType).
 */
export type ScanModelKind = "binary" | "multiclass" | "multilabel";

export interface ScanType {
  key: string;
  modelFile: string;
  displayName: string;
  description: string;
  kind: ScanModelKind;
  isBinary: boolean;
  labels: string[];
  inputChannels: number;
  inputSize: number;
  reportGate: number;
  perLabelThresholds?: number[];
}

/** Retrieval-augmented-generation knowledge document. */
export interface RagDocument {
  id: string;
  category: string;
  topic: string;
  content: string;
}

export interface DiseaseAdvice {
  condition: string;
  cause: string;
  remedy: string;
  doctorToConsult: string;
  urgencyHint: string;
}

export interface DurationInfo {
  multiplier: number;
  riskEscalation: boolean;
}