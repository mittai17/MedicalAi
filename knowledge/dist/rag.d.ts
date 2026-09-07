import type { RagDocument } from "./types.js";
export type { RagDocument };
export interface RagRetriever {
    retrieve(query: string, limit?: number, category?: string | null): RagDocument[];
}
interface Indexed {
    document: RagDocument;
    terms: Map<string, number>;
    totalTerms: number;
    seedTokens: string[];
}
export declare function normalizeTerms(text: string): Map<string, number>;
/**
 * Searchable text for a document — topic plus content, matching the app's
 * RagDocument.searchText.
 */
export declare function searchText(doc: RagDocument): string;
/**
 * Frequency-weighted token overlap (BM25-like), the pure-Kotlin fallback score
 * from LocalRagRetriever.kotlinScore(). Query terms that appear get weight;
 * document term frequency is capped at 3. Normalized by query size plus a
 * slight length penalty for long docs.
 */
export declare function kotlinScore(entry: Indexed, qTerms: Map<string, number>, querySize: number): number;
/**
 * On-device RAG retriever over the SwasthAI knowledge store (port of
 * LocalRagRetriever). Given a free-text query (symptoms, voice transcript or
 * image finding), returns the most relevant documents by lightweight lexical
 * similarity. Runs fully offline and needs no model download.
 */
export declare class InMemoryRagRetriever implements RagRetriever {
    private readonly index;
    constructor(documents: RagDocument[]);
    retrieve(query: string, limit?: number, category?: string | null): RagDocument[];
}
//# sourceMappingURL=rag.d.ts.map