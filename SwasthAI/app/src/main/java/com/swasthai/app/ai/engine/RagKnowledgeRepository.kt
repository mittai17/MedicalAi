package com.swasthai.app.ai.engine

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the local RAG store on-device.
 *
 * Merges two sources:
 *  1. The built-in clinical [DiseaseKnowledgeBase] (conditions -> advice).
 *  2. The bundled general/health dataset asset `rag_knowledge.json`.
 *
 * This is the "RAG data DB": a single searchable set of documents the
 * [LocalRagRetriever] queries fully offline. No server or model needed.
 */
@Singleton
class RagKnowledgeRepository @Inject constructor(
    private val context: Context
) {

    private val datasetAsset = "rag_knowledge.json"

    /** Build the full document set. Cheap enough to call at startup. */
    fun buildDocuments(): List<RagDocument> {
        val docs = ArrayList<RagDocument>()

        // 1) Built-in clinical knowledge as documents.
        DiseaseKnowledgeBase.allEntries().forEach { (key, advice) ->
            docs.add(
                RagDocument(
                    id = "kb_$key",
                    topic = advice.condition,
                    category = "clinical",
                    content = "Cause: ${advice.cause} Remedy: ${advice.remedy} " +
                        "Consult: ${advice.doctorToConsult} ${advice.urgencyHint}".trim()
                )
            )
        }

        // 2) Bundled general/health dataset.
        docs.addAll(loadDatasetDocs())

        return docs
    }

    private fun loadDatasetDocs(): List<RagDocument> {
        return try {
            val raw = context.assets.open(datasetAsset).bufferedReader().use { it.readText() }
            val json = JSONObject(raw)
            val arr = json.getJSONArray("documents")
            (0 until arr.length()).map { i ->
                val o: JSONObject = arr.getJSONObject(i)
                RagDocument(
                    id = o.optString("id", "doc_$i"),
                    topic = o.optString("topic", "General"),
                    category = o.optString("category", "general"),
                    content = o.optString("content", "")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
