import type { ScanType } from "../types.js";
/**
 * Screening model registry. Ported from the app's ScanType enum.
 * Binary models emit a single sigmoid (positive-class probability);
 * multiclass models emit a softmax over [labels]; multilabel models emit a
 * sigmoid per label, gated by perLabelThresholds.
 */
export declare const SCAN_TYPES: ScanType[];
export declare const SCAN_TYPE_BY_KEY: ReadonlyMap<string, ScanType>;
//# sourceMappingURL=scanTypes.d.ts.map