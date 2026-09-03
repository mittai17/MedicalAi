package com.swasthai.app.feature.citizen.aichat

import com.swasthai.app.ai.engine.LocalRagRetriever
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local chat answer synthesizer.
 *
 * Turns free text into a helpful, sourced answer using only the tiny
 * on-device RAG retriever (the bundled clinical knowledge base + the
 * general health dataset). Everything runs offline and costs almost no
 * memory — the index is a few hundred KB of in-memory term maps.
 *
 * The UI streams the returned text word by word for a "live conversation"
 * feel; no generative model is involved.
 */
@Singleton
class AiChatEngine @Inject constructor(
    private val ragRetriever: LocalRagRetriever
) {

    /** Best-match skill for [text], or null for a general/FAQ answer. */
    fun detectSkill(text: String): AiSkill? {
        val normalized = text.lowercase()
        var best: AiSkill? = null
        var bestScore = 0
        AiSkill.entries.forEach { skill ->
            val score = skill.keywords.count { normalized.contains(it) }
            if (score > bestScore) {
                bestScore = score
                best = skill
            }
        }
        // GREETING is only chosen when nothing richer matched.
        if (best == AiSkill.GREETING && bestScore > 0) return best
        return best?.takeIf { bestScore > 0 }
    }

    /** Full composed answer for [text] (uses skill routing + RAG). */
    fun responseFor(text: String): String {
        val skill = detectSkill(text)
        return when (skill) {
            AiSkill.GREETING -> greeting()
            AiSkill.HEALTH_CHECK -> healthCheckIntro()
            AiSkill.TASTE -> categoryAnswer(
                title = "Tasty + healthy ideas",
                category = "nutrition",
                query = text
            ) { empty ->
                "I don't have a specific nutrition note for that yet, but here is a healthy habit: " +
                    "aim for at least five servings of vegetables and fruits a day. " +
                    "Tap 🍽 Taste and ask about a dish you like."
            }
            AiSkill.FIRST_AID -> categoryAnswer(
                title = "First aid guidance",
                category = "first_aid",
                query = text
            ) { empty ->
                "For any serious injury, bleeding, breathing trouble or poisoning, call 108 (ambulance) " +
                    "or visit the nearest health centre right away. For minor cuts: clean the wound, " +
                    "apply pressure to stop bleeding, and cover it with a clean dressing."
            }
            AiSkill.TIPS -> tipsAnswer()
            AiSkill.FIND_CARE -> findCareAnswer()
            else -> generalAnswer(text)
        }
    }

    private fun greeting(): String = buildString {
        appendLine("Hello! 👋 I'm your on-device AI doctor — everything runs locally, so there's no internet needed and your data stays on this phone.")
        appendLine()
        appendLine("You can ask me things like:")
        appendLine("• 🩺 “Check my symptoms” — a guided health check")
        appendLine("• 🍽 “Suggest something healthy to eat”")
        appendLine("• 🩹 “First aid for a burn”")
        appendLine("• 💡 “Health tips”")
        appendLine()
        append("Or just tap a skill chip below to get started.")
    }

    private fun healthCheckIntro(): String = buildString {
        appendLine("Sure — let's run a quick health check. 📋")
        appendLine("Step 1: What is your main problem? Tap all that apply — I've listed the common ones below.")
    }

    private fun generalAnswer(query: String): String {
        val docs = ragRetriever.retrieve(query, limit = 3)
        if (docs.isEmpty()) {
            return buildString {
                appendLine("I searched the local health library but couldn't find a confident match for that. 🤔")
                appendLine()
                appendLine("Try being more specific, or run a 🩺 Health Check so I can guide you step-by-step.")
            }
        }
        return buildString {
            appendLine("Here's what the local health library says about that: 📖")
            appendLine()
            docs.forEachIndexed { i, doc ->
                appendLine("${i + 1}. ${doc.topic}")
                appendLine("   ${doc.content}")
            }
            appendLine()
            append("This is general guidance — if symptoms are severe or lasting, see a doctor.")
        }
    }

    private fun categoryAnswer(
        title: String,
        category: String,
        query: String,
        fallback: (Boolean) -> String
    ): String {
        val docs = ragRetriever.retrieve(query, limit = 3, category = category)
        if (docs.isEmpty() && ragHasCategory(category)) {
            return fallback(false)
        }
        if (docs.isEmpty()) {
            return fallback(true)
        }
        return buildString {
            appendLine("$title: 📖")
            appendLine()
            docs.forEachIndexed { i, doc ->
                appendLine("${i + 1}. ${doc.topic}")
                appendLine("   ${doc.content}")
            }
            appendLine()
            append("Little adjustments like these add up. Want me to suggest a healthy meal, a first-aid step or a tip next?")
        }
    }

    private fun tipsAnswer(): String {
        val categories = listOf("general", "mental", "hygiene", "prevention", "chroni")
        val bullets = categories.flatMap { category ->
            ragRetriever.retrieve("weekly wellness", limit = 1, category = category)
        }.distinctBy { it.topic }
        return buildString {
            appendLine("Quick wellness tips: 💡")
            appendLine()
            bullets.forEach { doc ->
                appendLine("• ${doc.topic}: ${doc.content}")
            }
            appendLine()
            append("Small daily habits keep you well. Ask me about food, first aid or a health check too!")
        }
    }

    private fun findCareAnswer(): String = buildString {
        appendLine("Here's how to reach care: 🏥")
        appendLine("• Emergency / Ambulance: call 108")
        appendLine("• Free national health helpline: call 104")
        appendLine("• Visit your nearest PHC/CHC or district hospital for check-ups")
        appendLine()
        val docs = ragRetriever.retrieve("health centre doctor visit", limit = 2, category = "general")
        docs.forEach { doc ->
            appendLine("• ${doc.topic}: ${doc.content}")
        }
        appendLine()
        append("If you are running a symptom check, hit 🩺 Health Check and I can point you to the right kind of care based on your risk.")
    }

    private fun ragHasCategory(category: String): Boolean =
        ragRetriever.retrieve("health", limit = 1, category = category).isNotEmpty()
}