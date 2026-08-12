package com.swasthai.app.ai.engine

import com.swasthai.app.domain.model.RiskLevel
import com.swasthai.app.domain.model.Symptom
import com.swasthai.app.domain.model.Vitals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ClinicalReasoningEngineTest {

    private val engine = ClinicalReasoningEngine()

    private fun sym(name: String, duration: String? = null) =
        Symptom(id = UUID.randomUUID().toString(), screeningId = "s", name = name, duration = duration)

    private fun vitals(temp: Float? = null, pulse: Int? = null, spo2: Float? = null) =
        Vitals(id = "v", screeningId = "s", temperature = temp, pulse = pulse, spo2 = spo2)

    @Test
    fun `fever and cough with breathlessness maps to pneumonia with high risk`() {
        val out = engine.reason(
            symptoms = listOf(sym("Fever"), sym("Cough"), sym("Difficulty Breathing")),
            vitals = vitals(spo2 = 91f)
        )
        assertEquals("Pneumonia", out.predictedDisease)
        assertEquals(RiskLevel.HIGH, out.riskLevel)
        assertTrue(out.confidence in 0.1f..0.99f)
        assertTrue(out.advice.doctorToConsult.contains("Pulmonologist"))
        assertTrue(out.recommendations.any { it.category == "emergency" || it.category == "urgent" })
    }

    @Test
    fun `fever and cough only maps to respiratory infection moderate`() {
        val out = engine.reason(
            symptoms = listOf(sym("Fever"), sym("Cough")),
            vitals = null
        )
        assertEquals("Respiratory Infection", out.predictedDisease)
        assertEquals(RiskLevel.MODERATE, out.riskLevel)
        assertTrue(out.confidence in 0.1f..0.99f)
    }

    @Test
    fun `vomiting and diarrhea with cramps maps to gastroenteritis or food poisoning`() {
        val out = engine.reason(
            symptoms = listOf(sym("Nausea / Vomiting"), sym("Diarrhea"), sym("Stomach pain")),
            vitals = null
        )
        assertTrue(
            "expected gastro/food-poisoning but got ${out.predictedDisease}",
            out.predictedDisease == "Gastroenteritis" || out.predictedDisease == "Food Poisoning"
        )
        assertTrue(out.recommendations.any { it.text.contains("ORS") })
    }

    @Test
    fun `dangerous vitals escalate risk even with mild symptoms`() {
        val out = engine.reason(
            symptoms = listOf(sym("Headache")),
            vitals = vitals(temp = 39.5f, spo2 = 88f)
        )
        assertEquals(RiskLevel.HIGH, out.riskLevel)
        assertTrue(out.recommendations.any { it.category == "emergency" })
    }

    @Test
    fun `skin rash maps to dermatitis low risk`() {
        val out = engine.reason(
            symptoms = listOf(sym("Skin Rash")),
            vitals = null
        )
        assertEquals("Dermatitis / Skin Allergy", out.predictedDisease)
        assertEquals(RiskLevel.LOW, out.riskLevel)
    }

    @Test
    fun `unrecognised symptoms produce honest cannot-determine outcome`() {
        val out = engine.reason(
            symptoms = listOf(sym("Very unusual symptom phrase nobody matches")),
            vitals = null
        )
        assertEquals("Undifferentiated Illness", out.predictedDisease)
        assertEquals(0f, out.confidence)
    }

    @Test
    fun `prolonged duration escalates risk`() {
        val out = engine.reason(
            symptoms = listOf(sym("Headache", duration = "More than 7 days")),
            vitals = null
        )
        assertEquals(RiskLevel.MODERATE, out.riskLevel)
    }

    @Test
    fun `confidence is a derived posterior not a fixed number`() {
        val a = engine.reason(listOf(sym("Fever"), sym("Cough")), null)
        val b = engine.reason(listOf(sym("Fever"), sym("Cough"), sym("Sore Throat"), sym("Runny nose")), null)
        assertTrue("confidence must vary with evidence", a.confidence != b.confidence)
    }
}
