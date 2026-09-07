const STOP_WORDS = new Set([
    "the", "a", "an", "and", "or", "of", "to", "in", "on", "for", "with",
    "is", "are", "be", "your", "you", "it", "as", "if", "at", "by", "this",
    "that", "may", "can", "need", "from", "not", "do", "should", "does",
    "have", "has", "help", "about", "when", "how", "what", "why", "into",
]);
export function normalizeTerms(text) {
    const terms = new Map();
    const cleaned = text
        .toLowerCase()
        .replace(/[^a-z0-9 ]/g, " ")
        .split(/\s+/);
    for (const token of cleaned) {
        if (token.length > 2 && !STOP_WORDS.has(token)) {
            terms.set(token, (terms.get(token) ?? 0) + 1);
        }
    }
    return terms;
}
function buildIndex(documents) {
    return documents.map((document) => {
        const terms = normalizeTerms(searchText(document));
        return {
            document,
            terms,
            totalTerms: Array.from(terms.values()).reduce((a, b) => a + b, 0),
            seedTokens: Array.from(terms.entries()).flatMap(([term, count]) => Array.from({ length: count }, () => term)),
        };
    });
}
/**
 * Searchable text for a document — topic plus content, matching the app's
 * RagDocument.searchText.
 */
export function searchText(doc) {
    return `${doc.topic} ${doc.content}`;
}
/**
 * Frequency-weighted token overlap (BM25-like), the pure-Kotlin fallback score
 * from LocalRagRetriever.kotlinScore(). Query terms that appear get weight;
 * document term frequency is capped at 3. Normalized by query size plus a
 * slight length penalty for long docs.
 */
export function kotlinScore(entry, qTerms, querySize) {
    let overlap = 0;
    for (const [term, queryFreq] of qTerms) {
        const docFreq = entry.terms.get(term) ?? 0;
        if (docFreq > 0) {
            overlap += Math.min(queryFreq, 1) * (1 + Math.min(docFreq, 3));
        }
    }
    return overlap / (querySize + 0.1 * entry.totalTerms);
}
/**
 * On-device RAG retriever over the SwasthAI knowledge store (port of
 * LocalRagRetriever). Given a free-text query (symptoms, voice transcript or
 * image finding), returns the most relevant documents by lightweight lexical
 * similarity. Runs fully offline and needs no model download.
 */
export class InMemoryRagRetriever {
    constructor(documents) {
        this.index = buildIndex(documents);
    }
    retrieve(query, limit = 3, category = null) {
        const qTerms = normalizeTerms(query);
        if (qTerms.size === 0)
            return [];
        const querySize = qTerms.size;
        return this.index
            .filter((entry) => category == null || entry.document.category === category)
            .map((entry) => ({ doc: entry.document, score: kotlinScore(entry, qTerms, querySize) }))
            .filter((r) => r.score > 0)
            .sort((a, b) => b.score - a.score)
            .slice(0, limit)
            .map((r) => r.doc);
    }
}
