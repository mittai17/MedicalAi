package com.swasthai.app.ai.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalRagRetrieverTest {

    private val docs = listOf(
        RagDocument("d1", "Pneumonia", "clinical",
            "An infection of the air sacs in the lungs, usually with cough and fever. Rest and see a doctor."),
        RagDocument("d2", "Healthy Diet", "general",
            "Eat a balanced diet with vegetables, fruits, whole grains and protein. Drink clean water."),
        RagDocument("d3", "Handwashing", "hygiene",
            "Wash hands with soap and water for 20 seconds to prevent infection."),
        RagDocument("d4", "Burns", "first_aid",
            "Cool a minor burn under running water for 10 minutes. Do not use ice or butter."),
        RagDocument("d5", "Stress Management", "mental",
            "Use deep breathing, regular sleep and talking to trusted people to manage stress.")
    )

    private val retriever = LocalRagRetriever(docs)

    @Test
    fun `retrieves most relevant document for a query`() {
        val result = retriever.retrieve("how do I wash my hands to stop infection", limit = 1)
        assertEquals("Clean hands first for hygiene", "Handwashing", result.firstOrNull()?.topic)
    }

    @Test
    fun `retrieves diet document for nutrition query`() {
        val result = retriever.retrieve("what should I eat for a balanced healthy diet")
        assertEquals("Healthy Diet", result.firstOrNull()?.topic)
    }

    @Test
    fun `returns multiple relevant documents sorted best first`() {
        val result = retriever.retrieve("cough and fever", limit = 3)
        assertEquals("Pneumonia", result.firstOrNull()?.topic)
        assertTrue(result.size <= 3)
    }

    @Test
    fun `empty query yields empty result`() {
        assertTrue(retriever.retrieve("").isEmpty())
        assertTrue(retriever.retrieve("a the and of").isEmpty())
    }

    @Test
    fun `category filter narrows results`() {
        val result = retriever.retrieve("water burn", limit = 5, category = "first_aid")
        assertTrue(result.all { it.category == "first_aid" })
        assertEquals("Burns", result.firstOrNull()?.topic)
    }
}
