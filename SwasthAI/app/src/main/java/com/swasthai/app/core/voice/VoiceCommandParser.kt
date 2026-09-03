package com.swasthai.app.core.voice

import com.swasthai.app.ai.engine.ScanType
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Converts a spoken command into a [VoiceIntent] plus the assistant's reply.
 *
 * Pure Kotlin (no Android dependencies) so it is unit-testable. Supports
 * English and Hindi keywords; other languages fall back to English phrases,
 * which the recognizer often returns anyway.
 *
 * Reminder phrasing supports a time ("at 9", "7 in the evening") and a
 * repeat ("every day", "every 2 hours") so elderly users can say things
 * like "remind me to take my medicine at 9 in the morning every day".
 */
@Singleton
class VoiceCommandParser @Inject constructor() {

    // ─────────────────────────────────────────────────────────────────
    // Entry point
    // ─────────────────────────────────────────────────────────────────

    fun parse(text: String, languageCode: String = "en"): ParsedVoiceCommand {
        val t = text.trim().lowercase(Locale.ROOT)
        if (t.isBlank()) {
            return ParsedVoiceCommand(VoiceIntent.Unknown(text), unknownReply(languageCode))
        }

        if (matchesAny(t, phrasesCancel(languageCode))) {
            return ParsedVoiceCommand(VoiceIntent.Cancel, cancelReply(languageCode))
        }
        if (matchesAny(t, phrasesEmergency(languageCode))) {
            return ParsedVoiceCommand(VoiceIntent.Emergency, emergencyReply(languageCode))
        }
        if (matchesAny(t, phrasesGreeting(languageCode))) {
            return ParsedVoiceCommand(VoiceIntent.Greeting, greetingReply(languageCode))
        }
        if (matchesAny(t, phrasesShowRecords(languageCode))) {
            return ParsedVoiceCommand(VoiceIntent.ShowRecords, showRecordsReply(languageCode))
        }
        if (matchesAny(t, phrasesShowReminders(languageCode))) {
            return ParsedVoiceCommand(VoiceIntent.ShowReminders, showRemindersReply(languageCode))
        }
        if (matchesReminder(t, languageCode)) {
            return ParsedVoiceCommand(buildReminder(t, languageCode), reminderPromptOnly(languageCode))
        }
        if (matchesAny(t, phrasesCamera(languageCode))) {
            val scanType = parseScanType(t, languageCode) ?: ScanType.PNEUMONIA
            return ParsedVoiceCommand(VoiceIntent.StartCamera(scanType), cameraOpeningReply(languageCode))
        }
        if (matchesAny(t, phrasesConsultation(languageCode))) {
            val reason = extractConsultationReason(t, languageCode)
            val urgency = parseUrgency(t, languageCode)
            return ParsedVoiceCommand(VoiceIntent.BookConsultation(reason, urgency), consultationPromptOnly(languageCode))
        }
        if (matchesAny(t, phrasesFindCare(languageCode))) {
            return ParsedVoiceCommand(VoiceIntent.FindCare, findCareReply(languageCode))
        }
        if (matchesHelp(t, languageCode)) {
            return ParsedVoiceCommand(VoiceIntent.Help, helpReply(languageCode))
        }
        if (matchesHealthCheck(t, languageCode)) {
            val symptoms = extractSymptomText(t, languageCode)
            return ParsedVoiceCommand(VoiceIntent.HealthCheck(symptoms), healthRunningReply(languageCode))
        }
        return ParsedVoiceCommand(VoiceIntent.Unknown(text), unknownReply(languageCode))
    }

    // ─────────────────────────────────────────────────────────────────
    // Intent matching helpers
    // ─────────────────────────────────────────────────────────────────

    private fun matchesAny(t: String, phrases: List<String>): Boolean =
        phrases.any { t.contains(it) }

    private fun matchesReminder(t: String, lang: String): Boolean {
        val phrases = phrasesReminder(lang)
        if (phrases.any { t.contains(it) }) return true
        // "daily" / "every day" with a "remind" style stem.
        return (t.contains("daily") || t.contains("every day") || t.contains("har din")) &&
            (t.contains("remind") || t.contains("reminder") || t.contains("yaad"))
    }

    private fun matchesHealthCheck(t: String, lang: String): Boolean =
        phrasesHealthCheck(lang).any { t.contains(it) }

    /** "help" is only its own intent when the utterance is short/direct. */
    private fun matchesHelp(t: String, lang: String): Boolean {
        if (phrasesHelp(lang).any { t.contains(it) }) {
            if (t.length <= 40) return true
            // Longer utterances that mention help fall through to the
            // health check ("help me, I have fever and cough").
            return t.contains("what can you do") ||
                t.contains("how to use") ||
                t.contains("how do i use") ||
                t.contains("commands") ||
                t.contains("options")
        }
        return false
    }

    // ─────────────────────────────────────────────────────────────────
    // Reminder construction
    // ─────────────────────────────────────────────────────────────────

    private fun buildReminder(raw: String, lang: String): VoiceIntent.SetReminder {
        val title = extractReminderTitle(raw, lang)
        val timeOfDay = parseTimeOfDay(raw, lang)
        val repeat = parseRepeatInterval(raw, lang)
        return VoiceIntent.SetReminder(
            title = title,
            timeOfDayMillis = timeOfDay,
            repeatIntervalMillis = repeat
        )
    }

    private fun extractReminderTitle(raw: String, lang: String): String {
        var s = raw.lowercase(Locale.ROOT)

        // Cut everything before the first reminder trigger phrase.
        val triggers = phrasesReminder(lang).sortedByDescending { it.length }
        var bestStart = -1
        var bestLen = 0
        for (tr in triggers) {
            val i = s.indexOf(tr)
            if (i >= 0 && (bestStart < 0 || i < bestStart)) {
                bestStart = i
                bestLen = tr.length
            }
        }
        if (bestStart >= 0) {
            s = s.substring(bestStart + bestLen)
        }

        // Remove time + repeat fragments so they don't pollute the title.
        s = s
            .replace(timeFragments, " ")
            .replace(repeatFragments, " ")
            .replace("\u3000", " ")

        s = s
            .trim()
            .replace(LEADING_CONNECTORS, "")
            .trim()
            .trimEnd('.', '!', '?', ',')
            .trim()

        return s.takeIf { it.isNotBlank() } ?: "Health reminder"
    }

    fun parseTimeOfDay(raw: String, lang: String): Long? {
        val t = raw.lowercase(Locale.ROOT)

        // "9 pm", "9:30 am", "7 a m"
        EXACT_AMPM.find(t)?.let { m ->
            val hour = m.groupValues[1].toInt() % 24
            val minute = m.groupValues[2].takeIf { it.isNotBlank() }?.toInt() ?: 0
            val isPm = m.groupValues[3].startsWith("p")
            return hourOfDay(hour, minute, isPm = isPm, hint = null)
        }

        // "at 9 in the morning", "at 7 o'clock in the evening"
        WITH_PERIOD.find(t)?.let { m ->
            val hour = m.groupValues[1].toInt() % 24
            val minute = m.groupValues[2].takeIf { it.isNotBlank() }?.toInt() ?: 0
            val hint = periodHint(m.groupValues[4])
            return hourOfDay(hour, minute, isPm = null, hint = hint)
        }

        // "at 9", "at 9:30", "at noon", "at midnight"
        AT_TIME.find(t)?.let { m ->
            val token = m.groupValues[1]
            if (token == "noon") return 12L * HOUR
            if (token == "midnight") return 0L
            val hour = token.split(":").first().toInt() % 24
            val minute = token.split(":").getOrNull(1)?.toInt() ?: 0
            return hourOfDay(hour, minute, isPm = null, hint = null)
        }

        // Bare period words.
        return when {
            t.contains("noon") -> 12L * HOUR
            t.contains("midnight") -> 0L
            t.contains("morning") -> 9L * HOUR
            t.contains("afternoon") -> 14L * HOUR
            t.contains("evening") -> 18L * HOUR
            t.contains("night") -> 21L * HOUR
            else -> null
        }
    }

    fun parseRepeatInterval(raw: String, lang: String): Long? {
        val t = raw.lowercase(Locale.ROOT)
        REPEAT_UNIT.find(t)?.let { m ->
            val count = m.groupValues[1].toIntOrNull() ?: return@let
            val unit = m.groupValues[2]
            return if (unit.contains("hour") || unit.contains("gha")) {
                count * HOUR
            } else if (unit.contains("minute") || unit.contains("min")) {
                count * MINUTE
            } else {
                count * DAY
            }
        }
        return when {
            t.contains("daily") || t.contains("every day") ||
                t.contains("har din") || t.contains("har roz") || t.contains("roz") -> DAY
            t.contains("every hour") || t.contains("har ghante") -> HOUR
            else -> null
        }
    }

    private fun periodHint(raw: String): String? {
        if (raw.isBlank()) return null
        return when {
            raw.contains("noon") -> "noon"
            raw.contains("morning") -> "morning"
            raw.contains("afternoon") -> "afternoon"
            raw.contains("evening") -> "evening"
            raw.contains("night") -> "night"
            else -> null
        }
    }

    private fun hourOfDay(hour: Int, minute: Int, isPm: Boolean?, hint: String?): Long {
        var h = hour % 24
        if (isPm == true && h < 12) h += 12
        if (isPm == false) h %= 12
        if (isPm == null) {
            when (hint) {
                "evening", "night" -> if (h < 12) h += 12
                "morning", "afternoon" -> if (h == 0) h = 12
                "noon" -> h = 12
                else -> {
                    // Bare "at 9": assume 9 AM. Numbers 1–6 with no hints are
                    // treated as afternoon/evening (post-lunch medicines).
                    if (h in 1..6) h += 12
                }
            }
        }
        return h * HOUR + minute * MINUTE
    }

    // ─────────────────────────────────────────────────────────────────
    // Scan type / urgency / consultation reason extraction
    // ─────────────────────────────────────────────────────────────────

    fun parseScanType(raw: String, lang: String): ScanType? {
        val t = raw.lowercase(Locale.ROOT)
        return when {
            phrasesChestXray().any { t.contains(it) } -> ScanType.PNEUMONIA
            phrasesBreast().any { t.contains(it) } -> ScanType.BREAST_SCAN
            phrasesRetina().any { t.contains(it) } -> ScanType.RETINA
            phrasesSkin().any { t.contains(it) } -> ScanType.SKIN_LESION
            phrasesBlood().any { t.contains(it) } -> ScanType.BLOOD_CELL
            t.contains("oct") -> ScanType.OCT_RETINA
            phrasesColon().any { t.contains(it) } -> ScanType.COLON_PATH
            phrasesKidney().any { t.contains(it) } -> ScanType.KIDNEY_TISSUE
            else -> null
        }
    }

    fun parseUrgency(raw: String, lang: String): String = if (
        matchesAny(raw.lowercase(Locale.ROOT), phrasesUrgent(lang))
    ) "HIGH" else "NORMAL"

    private fun extractConsultationReason(raw: String, lang: String): String {
        var s = raw.lowercase(Locale.ROOT)
        phrasesConsultation(lang).sortedByDescending { it.length }.forEach { tr ->
            s = s.replace(tr, " ")
        }
        s = s
            .replace(CONSULTATION_LINKERS, " ")
            .trim()
            .trimEnd('.', '!', '?')
            .trim()
        if (s.isEmpty()) return "Requested via voice assistant"
        // Heuristic title casing for a nicer saved reason.
        return s.split(" ").joinToString(" ") { word ->
            if (word.length <= 2) word else word.replaceFirstChar { it.uppercase() }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Symptom extraction for the health check
    // ─────────────────────────────────────────────────────────────────

    private fun extractSymptomText(raw: String, lang: String): String {
        var s = raw.trim().lowercase(Locale.ROOT)
        // Strip the triggering prefix phrases from the start of the phrase,
        // keeping the descriptive part ("I have fever and cough" → fever...).
        phrasesHealthCheck(lang)
            .sortedByDescending { it.length }
            .forEach { tr ->
                s = s.replace(" $tr", " ").trim()
                s = s.replace(LEADING_CONNECTORS, "").trim()
            }
        if (s.isNotEmpty() && s[0].isLetter()) {
            s = s.replaceFirstChar { it.uppercase() }
        }
        return s
    }

    // ─────────────────────────────────────────────────────────────────
    // Phrase tables (English + Hindi)
    // ─────────────────────────────────────────────────────────────────

    private fun phrasesCancel(lang: String): List<String> = if (lang == "hi") {
        listOf("cancel", "rok do", "bas karo", "mat karo", "band karo", "wapas", "khatam")
    } else {
        listOf("cancel", "stop listening", "stop", "go back", "never mind", "forget it")
    }

    private fun phrasesEmergency(lang: String): List<String> = if (lang == "hi") {
        listOf("emergency", "emarjansi", "108 par call", "call 108", "urgent help")
    } else {
        listOf("emergency", "call emergency", "call 108", "dial 108", "emergency help", "urgent help")
    }

    private fun phrasesGreeting(lang: String): List<String> = if (lang == "hi") {
        listOf("namaste", "namaskar", "pranam", "hello", "helo", "good morning", "good evening")
    } else {
        listOf("good morning", "good afternoon", "good evening", "hello", "hi", "hey")
    }

    private fun phrasesReminder(lang: String): List<String> = if (lang == "hi") {
        listOf(
            "reminder set karo", "reminder laga do", "reminder bana do",
            "yaad dilana", "yaad dilao", "yaad dila", "yaad dilwane",
            "set a reminder", "set the reminder", "set reminder", "add a reminder",
            "add reminder", "remind me to", "remind me", "reminder",
            "daily reminder", "medicine reminder", "pill reminder",
            "take my medicine", "dava", "dawa", "remind karo"
        )
    } else {
        listOf(
            "set a reminder", "set the reminder", "set reminder", "add a reminder",
            "add reminder", "remind me to", "remind me", "daily reminder",
            "medicine reminder", "medication reminder", "pill reminder",
            "take my medicine", "reminder"
        )
    }

    private fun phrasesCamera(lang: String): List<String> = if (lang == "hi") {
        listOf(
            "take a photo", "take a picture", "take photo", "open camera", "camera kholo",
            "photo lo", "photo kholo", "photo kheecho", "scan karo", "image check",
            "x-ray", "xray", "x ray", "chest xray", "chest x-ray", "check xray",
            "skin ka photo", "camera"
        )
    } else {
        listOf(
            "take a photo", "take a picture", "take photo", "open the camera",
            "open camera", "camera", "scan my", "scan my chest", "scan my xray",
            "scan my x-ray", "scan my skin", "scan my eye", "image check",
            "x-ray", "xray", "x ray", "take a scan", "photo of", "picture of"
        )
    }

    private fun phrasesConsultation(lang: String): List<String> = if (lang == "hi") {
        listOf(
            "consultation book karo", "appointment book karo", "book a consultation",
            "book consultation", "book an appointment", "book appointment",
            "doctor se baat", "doctor se milna", "consult a doctor",
            "consultation", "talk to a doctor", "see a doctor", "i want to see a doctor",
            "online consultation", "doctor ko dikhana", "appointment"
        )
    } else {
        listOf(
            "book a consultation", "book consultation", "book an appointment",
            "book appointment", "talk to a doctor", "talk to doctor", "see a doctor",
            "consult a doctor", "i want to see a doctor", "consultation",
            "online consultation", "doctor appointment", "appointment with a doctor"
        )
    }

    private fun phrasesFindCare(lang: String): List<String> = if (lang == "hi") {
        listOf(
            "hospital dhundho", "paas ka hospital", "aspaatal", "phc kahan hai",
            "health center doondho", "find a hospital", "nearest hospital",
            "find a health center", "health center", "primary health center",
            "hospital", "doctor dhundho"
        )
    } else {
        listOf(
            "find a hospital", "find hospital", "nearest hospital", "nearby hospital",
            "find a health center", "find a health centre", "health center",
            "health centre", "primary health center", "primary health centre",
            "where is a doctor", "hospital near me", "hospital"
        )
    }

    private fun phrasesShowRecords(lang: String): List<String> = if (lang == "hi") {
        listOf("records dekh", "meri records", "mera health record", "my records", "my health records")
    } else {
        listOf(
            "my records", "my health records", "show records", "see my records",
            "past screenings", "screening records", "show my reports", "my reports"
        )
    }

    private fun phrasesShowReminders(lang: String): List<String> = if (lang == "hi") {
        listOf(
            "reminders dekh", "meri reminders", "reminder list",
            "show my reminders", "show reminders", "show my reminder",
            "my reminders", "what reminders", "what reminders do i have"
        )
    } else {
        listOf(
            "my reminders", "show reminders", "show my reminders",
            "what reminders", "reminder list", "what reminders do i have"
        )
    }

    private fun phrasesHelp(lang: String): List<String> = if (lang == "hi") {
        listOf("help karo", "kya kar sakte ho", "kya karta hai", "madad", "kya karna hai")
    } else {
        listOf(
            "help", "what can you do", "what can we do", "how do i use",
            "how to use", "commands", "options", "what do you do"
        )
    }

    private fun phrasesHealthCheck(lang: String): List<String> = if (lang == "hi") {
        listOf(
            "symptom check karo", "health check karo", "check my health", "check health",
            "check my symptoms", "symptom check", "mera swasthya", "tabiyat kharab",
            "meri tabiyat", "mujhe bukhar", "mujhe khansi", "mujhe dard", "mujhe",
            "i am not feeling well", "i'm not feeling well", "not feeling well",
            "feeling unwell", "i feel sick", "feel sick", "i have", "i've got",
            "i got", "sick", "health check", "bimaar", "beemar"
        )
    } else {
        listOf(
            "check my health", "check health", "health check", "check my symptoms",
            "symptom check", "i am not feeling well", "i'm not feeling well",
            "not feeling well", "feeling unwell", "i feel sick", "feel sick",
            "i am sick", "i'm sick", "i have", "i've got", "i got", "please check me"
        )
    }

    private fun phrasesUrgent(lang: String): List<String> = if (lang == "hi") {
        listOf("urgent", "urgency", "jaldi", "bahut zaroori", "emergency", "turant", "high priority")
    } else {
        listOf("urgent", "urgency", "emergency", "as soon as possible", "asap", "critical", "high priority")
    }

    private fun phrasesChestXray(): List<String> =
        listOf("chest", "xray", "x-ray", "x ray", "lung", "pneumonia", "phuphra", "phuphre")

    private fun phrasesBreast(): List<String> = listOf("breast")

    private fun phrasesRetina(): List<String> =
        listOf("retina", "retinal", "eye", "aankh", "vision", "fundus")

    private fun phrasesSkin(): List<String> =
        listOf("skin", "mole", "rash", "lesion")

    private fun phrasesBlood(): List<String> = listOf("blood", "khoon", "blood cell", "cells")

    private fun phrasesColon(): List<String> =
        listOf("colon", "colonoscopy", "biopsy", "stomach biopsy")

    private fun phrasesKidney(): List<String> =
        listOf("kidney", "gurda", "gurde", "renal")

    // ─────────────────────────────────────────────────────────────────
    // Reply templates
    // ─────────────────────────────────────────────────────────────────

    fun greetingReply(lang: String): String = if (lang == "hi") {
        "Namaste! Main aapke liye health check kar sakti hoon, reminders set kar sakti hoon, " +
            "consultation book kar sakti hoon, camera khol sakti hoon, ya aapke records dikha " +
            "sakti hoon. Bas boliye kya karna hai."
    } else {
        "Hello! I can run a health check, set a reminder, book a consultation, open the camera, " +
            "or open your records. Just tell me what you need."
    }

    fun helpReply(lang: String): String = if (lang == "hi") {
        "Aap keh sakte hain: check my health; mujhe bukhar aur khansi hai; " +
            "set a reminder to take my medicine at 9 in the morning; har din reminder; " +
            "book a consultation; take a photo of my skin; show my reminders; " +
            "find a hospital; ya show my records."
    } else {
        "You can say: check my health; I have fever and cough; set a reminder to take my " +
            "medicine at 9 in the morning; remind me every day; book a consultation; take a " +
            "photo of my skin; show my reminders; find a hospital; or open my records."
    }

    fun cancelReply(lang: String): String =
        if (lang == "hi") "Theek hai, cancel kar diya." else "Okay, cancelled."

    fun emergencyReply(lang: String): String = if (lang == "hi") {
        "Emergency mein turant 108 par call karein. Main ab aas-paas ki health services " +
            "khol rahi hoon."
    } else {
        "In an emergency, call 108 immediately. I am opening the nearby health services page."
    }

    fun findCareReply(lang: String): String =
        if (lang == "hi") "Aas-paas ki health services khol rahi hoon."
        else "Opening nearby health services."

    fun showRecordsReply(lang: String): String =
        if (lang == "hi") "Aapke health records khol rahi hoon."
        else "Opening your health records."

    fun showRemindersReply(lang: String): String =
        if (lang == "hi") "Aapke reminders dekh rahi hoon."
        else "Looking at your reminders."

    fun unknownReply(lang: String): String = if (lang == "hi") {
        "Maaf kijiye, main samjhi nahi. 'Help' boliye, main bataa dungi kya kya kar " +
            "sakti hoon."
    } else {
        "Sorry, I didn't understand that. Say 'help' to hear what I'm able to do."
    }

    fun promptSymptoms(lang: String): String = if (lang == "hi") {
        "Bilkul. Apne symptoms bataiye, jaise: mujhe bukhar aur khansi hai."
    } else {
        "Of course. Tell me your symptoms, for example: I have fever and cough."
    }

    fun promptReminderTime(lang: String): String = if (lang == "hi") {
        "Kis time par? Jaise: at 9 in the morning, ya har din."
    } else {
        "At what time? For example: at 9 in the morning, or every day."
    }

    fun promptConsultationReason(lang: String): String = if (lang == "hi") {
        "Consultation ki wajah bataiye? Jaise: fever for three days."
    } else {
        "What is the consultation for? For example: fever for three days."
    }

    fun reminderPromptOnly(lang: String): String = promptReminderTime(lang)

    fun consultationPromptOnly(lang: String): String = promptConsultationReason(lang)

    fun confirmedReminder(lang: String, title: String, timeText: String?, repeatText: String): String {
        val head = if (lang == "hi") "Reminder set ho gaya" else "Reminder set"
        val what = if (lang == "hi") "ke liye" else "for"
        return buildString {
            append(head)
            if (title.isNotBlank()) {
                if (lang == "hi") append(": ${title}.")
                else append(" $what ${title}.")
            }
            if (!timeText.isNullOrBlank()) append(" $timeText.")
            if (repeatText.isNotBlank()) append(" $repeatText.")
        }.trim()
    }

    fun confirmedConsultation(lang: String): String = if (lang == "hi") {
        "Aapka consultation request book ho gaya. Yah saved hai aur online hone par " +
            "sync ho jayega."
    } else {
        "Your consultation request is booked. It is saved on this device and will be " +
            "synced when you're online."
    }

    fun consultationFailed(lang: String): String = if (lang == "hi") {
        "Consultation request save nahi hua. Kripya dobara try karein."
    } else {
        "I couldn't save the consultation request. Please try again."
    }

    fun healthRunningReply(lang: String): String = if (lang == "hi") {
        "Theek hai, main aapke symptoms check kar rahi hoon."
    } else {
        "Alright, I'm checking your symptoms now."
    }

    fun imageRunningReply(lang: String): String = if (lang == "hi") {
        "Photo aa gayi. Main iska analysis kar rahi hoon."
    } else {
        "Photo taken. I'm analysing it now."
    }

    fun cameraOpeningReply(lang: String): String = if (lang == "hi") {
        "Camera khol rahi hoon. Phone ko seedha rakhein, area ko clear rakkhein aur " +
            "achi light mein photo lein."
    } else {
        "Opening the camera. Hold the phone steady, keep the area clearly in frame " +
            "with good light, and press the shutter."
    }

    fun waitWhileAnalysing(lang: String): String = if (lang == "hi") {
        "Bas ek pal, analysis ho raha hai."
    } else {
        "Just a moment, analysing now."
    }

    fun remindersEmptyReply(lang: String): String =
        if (lang == "hi") "Abhi koi reminder set nahi hai." else "You have no reminders yet."

    fun remindersSummaryReply(lang: String, summary: String): String = if (lang == "hi") {
        "Aapke reminders: $summary"
    } else {
        "Your reminders: $summary"
    }

    /** Spoken summary of a finished diagnosis. */
    fun resultSummary(
        disease: String,
        confidence: Int,
        risk: String,
        topAdvice: String?
    ): String = buildString {
        append("Your result is ready. It shows $disease, with confidence $confidence percent. ")
        append("Risk level is $risk. ")
        if (!topAdvice.isNullOrBlank()) append(topAdvice)
    }

    companion object {
        // One hour / minute / day in millis.
        val HOUR: Long = 3_600_000L
        val MINUTE: Long = 60_000L
        val DAY: Long = 24L * 3_600_000L

        private val EXACT_AMPM =
            Regex("""(?:at\s+)?(\d{1,2})(?::(\d{2}))?\s*(a\.?\s*m\.?|p\.?\s*m\.?)\b""")
        private val WITH_PERIOD = Regex(
            """at\s+(\d{1,2})(?::(\d{2}))?\s*(o'?clock)?\s*((?:in\s+the\s+)?(?:morning|afternoon|evening|night|noon))\b"""
        )
        private val AT_TIME =
            Regex("""at\s+(\d{1,2}(?::\d{2})?|noon|midnight)\b""")

        private val LEADING_CONNECTORS = Regex("""^\s*(?:to\s+|about\s+|for\s+|regarding\s+)+""")

        private val CONSULTATION_LINKERS = Regex("""\b(?:about|for|regarding|i have|i've got)\b""")

        /** Fragments removed from a reminder title once the time is used. */
        private val timeFragments = Regex(
            """\bat\s+\d{1,2}(?::\d{2})?(?:\s*(?:am|pm|o'?clock))?""" +
                """(?:\s+(?:in\s+the\s+)?(?:morning|afternoon|evening|night|noon))?""" +
                """|\b\d{1,2}(?::\d{2})?\s*(?:am|pm)\b(?:\s+(?:in\s+the\s+)?(?:morning|afternoon|evening|night))?""" +
                """|\b\d{1,2}(?::\d{2})?\s+(?:in\s+the\s+)?(?:morning|afternoon|evening|night|noon)\b""" +
                """|\bat\s+(?:noon|midnight)\b""" +
                """|\b(?:in\s+the\s+)?(?:morning|afternoon|evening|night)\b"""
        )

        /** Fragments removed from a reminder title once the repeat is used. */
        private val repeatFragments = Regex(
            """\bevery\s+\d+\s*(?:hours?|hrs?|minutes?|mins?|days?)s?\b""" +
                """|\bevery\s+day\b|\bdaily\b|\bhar\s+din\b|\bhar\s+roz\b|\broz\b"""
        )

        private val REPEAT_UNIT = Regex(
            """every\s+(\d+)\s*(hours?|hrs?|minutes?|mins?|days?|ghante?|din)\b"""
        )
    }
}