package com.swasthai.app.feature.citizen.aichat

/**
 * Built-in "skills" the local AI doctor can run. Each skill maps free text
 * to a retrieval category in the on-device RAG knowledge store.
 *
 * Skills are intentionally lightweight: no model, just keyword intent
 * routing + retrieval over the tiny local knowledge set, so the whole AI
 * console stays far below the 100 MB peak-memory budget.
 */
enum class AiSkill(
    val label: String,
    val chipLabel: String,
    val keywords: List<String>,
    val ragCategory: String? = null
) {
    HEALTH_CHECK(
        label = "Health Check",
        chipLabel = "🩺 Health Check",
        keywords = listOf(
            "health check", "symptom check", "feel sick", "not well", "unwell",
            "check my symptoms", "diagnose", "screening", "health screening",
            "am i sick", "what is wrong", "headache", "fever", "cough", "ache"
        ),
        ragCategory = null
    ),
    TASTE(
        label = "Taste Skill",
        chipLabel = "🍽 Taste",
        keywords = listOf(
            "taste", "recipe", "cook", "cooking", "food", "eat", "meal",
            "nutrition", "diet", "hungry", "snack", "dish", "healthy food"
        ),
        ragCategory = "nutrition"
    ),
    FIRST_AID(
        label = "First Aid",
        chipLabel = "🩹 First Aid",
        keywords = listOf(
            "first aid", "injury", "cut", "burn", "bleeding", "wound",
            "fall", "accident", "bite", "sting", "sprain", "hurt myself"
        ),
        ragCategory = "first_aid"
    ),
    TIPS(
        label = "Health Tips",
        chipLabel = "💡 Tips",
        keywords = listOf(
            "tips", "tip", "advice", "health tips", "wellness", "sleep",
            "stress", "hygiene", "exercise", "immunity", "stay healthy",
            "healthy lifestyle"
        ),
        ragCategory = null
    ),
    FIND_CARE(
        label = "Find Care",
        chipLabel = "🏥 Find Care",
        keywords = listOf(
            "doctor", "hospital", "phc", "clinic", "health centre", "health center",
            "emergency", "helpline", "near me", "ambulance", "find care"
        ),
        ragCategory = "general"
    ),
    FAQ(
        label = "Health FAQ",
        chipLabel = "❓ FAQ",
        keywords = listOf(
            "what is", "what are", "why do", "why is", "how to", "faq",
            "meaning", "cure", "treatment", "remedy", "difference"
        ),
        ragCategory = null
    ),
    GREETING(
        label = "Greeting",
        chipLabel = "👋 Hello",
        keywords = listOf(
            "hi", "hello", "hey", "namaste", "good morning", "good evening",
            "good afternoon", "hii", "greetings", "how are you"
        ),
        ragCategory = null
    )
}