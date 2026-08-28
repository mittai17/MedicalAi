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
        val totalTerms: Int
    )

    private val index: List<Indexed> = documents.map { doc ->
        val terms = normalizeTerms(doc.searchText)
        Indexed(
            document = doc,
            terms = terms,
            totalTerms = terms.values.sum()
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

        return index
            .asSequence()
            .filter { category == null || it.document.category == category }
            .map { entry ->
                val overlap = qTerms.keys.sumOf { term ->
                    val e = entry.terms[term] ?: 0
                    // Frequency-weighted overlap (BM25-like). Query terms that
                    // appear get weight; document term frequency is capped.
                    if (e > 0) minOf(qTerms[term] ?: 1, 1) * (1 + minOf(e, 3)) else 0
                }
                // Normalize by query size + slight length penalty for long docs.
                val score = overlap.toDouble() / (qTerms.size + 0.1 * entry.totalTerms)
                entry.document to score
            }
            .filter { it.second > 0f }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
            .toList()
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
