package com.swasthai.app.ai.engine

/**
 * A single retrievable chunk in the local RAG store. Sources include the
 * built-in clinical knowledge base and the bundled general/health dataset
 * (`assets/rag_knowledge.json`).
 *
 * @param id       Unique identifier (e.g. "gen_001" or a condition key).
 * @param topic    Short title used for matching and display.
 * @param category Category such as general, first_aid, nutrition, mental, etc.
 * @param content  Full retrieval text returned to the caller.
 */
data class RagDocument(
    val id: String,
    val topic: String,
    val category: String,
    val content: String
) {
    /** Text used for indexing/matching (topic + content). */
    val searchText: String get() = "$topic $content"
}
