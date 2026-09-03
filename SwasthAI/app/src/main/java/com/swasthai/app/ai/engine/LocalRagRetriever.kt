package com.swasthai.app.ai.engine

import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device RAG retriever over the SwasthAI knowledge store.
 *
 * Primary path on low-end devices (no heavy LLM, no unsupported native libs):
 * it indexes every [RagDocument] (built-in clinical [DiseaseKnowledgeBase]
 * entries + the bundled general/health dataset) and, given a free-text query
 * (symptoms, voice transcript or image finding), returns the most relevant
 * documents by lightweight lexical similarity (token overlap + frequency
 * weighting). It runs fully offline on 32-bit ARM and needs no model download.
 */
@Singleton
class LocalRagRetriever @Inject constructor(
    private val documents: List<RagDocument>
) {

    /** Normalized search text for each indexed document. */
    private data class Indexed(
        val document: RagDocument,
        val terms: Map<String, Int>,
        val totalTerms: Int,
        /** Per-term tokens expanded by frequency — the [AiNative] input. */
        val docTokens: Array<String>
    )

    private val index: List<Indexed> = documents.map { doc ->
        val terms = normalizeTerms(doc.searchText)
        Indexed(
            document = doc,
            terms = terms,
            totalTerms = terms.values.sum(),
            docTokens = terms.entries
                .flatMap { (term, count) -> List(count) { term } }
                .toTypedArray()
        )
    }

    /**
     * Return up to [limit] most relevant documents for [query], sorted
     * best-first. Returns an empty list if nothing scores above the floor or
     * if there is no usable query.
     */
    fun retrieve(query: String, limit: Int = 3, category: String? = null): List<RagDocument> {
        val qTerms = normalizeTerms(query)
        if (qTerms.isEmpty()) return emptyList()
        val qTokenArray = qTerms.keys.toTypedArray()

        return index
            .asSequence()
            .filter { category == null || it.document.category == category }
            .map { entry ->
                // Native score with a pure-Kotlin fallback; both compute the
                // same formula so results never drift between the paths.
                val score = AiNative.docScoreOrNull(qTokenArray, entry.docTokens)?.toDouble()
                    ?: kotlinScore(entry, qTerms, qTerms.size)
                entry.document to score
            }
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
            .toList()
    }

    /** Kotlin twin of the native kernel — same formula, used as the fallback. */
    private fun kotlinScore(
        entry: Indexed,
        qTerms: Map<String, Int>,
        querySize: Int
    ): Double {
        var overlap = 0.0
        for ((term, queryFreq) in qTerms) {
            val docFreq = entry.terms[term] ?: 0
            if (docFreq > 0) {
                // Frequency-weighted overlap (BM25-like). Query terms that
                // appear get weight; document term frequency is capped.
                overlap += minOf(queryFreq, 1) * (1 + minOf(docFreq, 3))
            }
        }
        // Normalize by query size + slight length penalty for long docs.
        return overlap / (querySize + 0.1 * entry.totalTerms)
    }

    private fun normalizeTerms(text: String): Map<String, Int> {
        val stop = setOf(
            "the", "a", "an", "and", "or", "of", "to", "in", "on", "for", "with",
            "is", "are", "be", "your", "you", "it", "as", "if", "at", "by", "this",
            "that", "may", "can", "need", "from", "not", "do", "should", "does",
            "have", "has", "help", "about", "when", "how", "what", "why", "into"
        )
        val terms = mutableMapOf<String, Int>()
        text.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it.length > 2 && it !in stop }
            .forEach { terms[it] = (terms[it] ?: 0) + 1 }
        return terms
    }
}
