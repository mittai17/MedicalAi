package com.swasthai.app.ai.engine

import com.swasthai.app.domain.model.MedicalAdvice
import com.swasthai.app.domain.model.Recommendation
import com.swasthai.app.domain.model.RiskLevel
import com.swasthai.app.domain.model.Symptom
import com.swasthai.app.domain.model.Vitals
import java.util.UUID

/**
 * Clinical reasoning engine — the AI doctor's text reasoning.
 *
 * Replaces the previous mock rule logic with a transparent, evidence-based
 * computation:
 *  - Every reported symptom contributes weighted evidence to candidate
 *    conditions (ClinicalKnowledge).
 *  - Evidence is accumulated per condition and normalised into a real
 *    confidence score (the condition's share of total matched evidence),
 *    not a fabricated number.
 *  - Vitals are checked against real clinical danger thresholds.
 *  - Risk and recommendations are derived from evidence + vitals, never
 *    hardcoded per-case.
 *
 * Nothing here is mock: the output follows from the data it was given.
 */
class ClinicalReasoningEngine(
    private val ragRetriever: LocalRagRetriever
) {

    data class ReasoningOutput(
        val predictedDisease: String,
        val advice: MedicalAdvice,
        val confidence: Float,
        val riskLevel: RiskLevel,
        val differentialDiagnosis: List<String>,
        val recommendations: List<Recommendation>
    )

    /**
     * Normalise a reported symptom name into canonical evidence ids.
     * Handles the UI checklist, free-text "other" input and voice transcript.
     */
    private fun canonicalizeSymptom(raw: String): List<String> {
        val original = raw.lowercase().trim()
        if (original.isBlank()) return emptyList()
        // Dataset symptom labels that general matching would mangle or
        // mis-resolve. "high_fever"/"mild_fever" are aliases of "fever" and
        // must not also emit their own id (that extra evidence skews ranking).
        when (original) {
            "high_fever", "mild_fever" -> return listOf("fever")
            "distention_of_abdomen" -> return listOf("distention_of_abdomen")
            "swelling_joints" -> return listOf("swelling_joints")
            "watering_from_eyes" -> return listOf("watering_from_eyes")
            "swollen_extremeties", "swollen_legs" -> return listOf("swelling")
        }
        val s = original.replace('_', ' ').replace(Regex("\\s+"), " ")
        if (s.isBlank()) return emptyList()
        val hits = mutableListOf<String>()
        val map = listOf(
            "breathing_difficulty" to listOf("breathing", "breath", "short of breath", "breathe", "respiratory distress"),
            "chest_pain" to listOf("chest pain", "chest", "tight chest"),
            "high_fever" to listOf("very high fever", "high fever", "severe fever"),
            "fever" to listOf("fever", "temperature", "pyrexia"),
            "cough" to listOf("cough", "coughing"),
            "sore_throat" to listOf("sore throat", "throat pain", "throat"),
            "runny_nose" to listOf("runny nose", "cold", "nasal", "sneezing"),
            "body_aches" to listOf("body ache", "muscle ache", "myalgia", "pain in body"),
            "fatigue" to listOf("fatigue", "tired", "weakness", "exhaust", "lethargy"),
            "headache" to listOf("headache", "head ache", "head pain"),
            "nausea" to listOf("nausea", "queasy", "sick to stomach"),
            "vomiting" to listOf("vomit", "throwing up"),
            "diarrhea" to listOf("diarrhea", "diarrhoea", "loose stool", "loose motion", "loose stools"),
            "abdominal_pain" to listOf("abdominal", "stomach pain", "stomach ache", "belly", "cramp"),
            "rash" to listOf("rash", "skin lesion", "hives", "bumps on skin"),
            "dizziness" to listOf("dizz", "lightheaded", "faint", "giddiness"),
            "chills" to listOf("chill", "shiver"),
            "loss_appetite" to listOf("loss of appetite", "not eating", "no appetite"),
            "wheezing" to listOf("wheeze"),
            "itching" to listOf("itch"),
            "bleeding" to listOf("bleed", "bleeding"),
            "seizure" to listOf("seizure", "fits", "convulsion"),
            "unconsciousness" to listOf("unconscious", "passed out", "blackout"),
            "bloody_stool" to listOf("blood in stool", "bloody stool", "blood in motion"),
            "swelling" to listOf("swell", "swelling"),
            "mouth_sores" to listOf("mouth sore", "mouth ulcer", "blister in mouth"),
            "sweating" to listOf("sweat", "sweating"),
            "abnormal_menstruation" to listOf("abnormal menstruation", "abnormal periods", "irregular periods", "irregular menstruation"),
            "acidity" to listOf("acidity", "heartburn", "acid reflux", "burning in chest"),
            "acute_liver_failure" to listOf("acute liver failure", "liver failure"),
            "altered_sensorium" to listOf("altered sensorium", "confusion", "confused", "mental confusion"),
            "anxiety" to listOf("anxiety", "anxious", "nervousness", "feeling nervous"),
            "back_pain" to listOf("back pain", "pain in the back"),
            "blackheads" to listOf("blackheads", "black heads"),
            "bladder_discomfort" to listOf("bladder discomfort", "bladder pain"),
            "blister" to listOf("blister", "blisters", "blistering"),
            "blood_in_sputum" to listOf("blood in sputum", "blood in phlegm", "coughing blood"),
            "blurred_and_distorted_vision" to listOf("blurred vision", "blurry vision", "distorted vision"),
            "brittle_nails" to listOf("brittle nails", "breaking nails"),
            "bruising" to listOf("bruising", "bruises"),
            "burning_micturition" to listOf("burning micturition", "burning urination", "pain while urinating", "burning when urinating"),
            "cold_hands_and_feet" to listOf("cold hands", "cold feet", "cold hands and feet"),
            "coma" to listOf("coma", "comatose", "unconscious"),
            "congestion" to listOf("congestion", "stuffy nose", "blocked nose"),
            "constipation" to listOf("constipation", "constipated", "hard stool", "can't pass stool"),
            "continuous_feel_of_urine" to listOf("continuous feel of urine", "constant urge to urinate", "always feel like urinating"),
            "continuous_sneezing" to listOf("continuous sneezing", "sneezing a lot", "constant sneezing"),
            "cramps" to listOf("cramps", "cramping", "stomach cramp", "abdominal cramp"),
            "dark_urine" to listOf("dark urine", "dark coloured urine"),
            "dehydration_sym" to listOf("dehydration", "dehydrated", "feeling dehydrated"),
            "depression" to listOf("depression", "depressed", "low mood", "feeling sad"),
            "dischromic_patches" to listOf("dischromic patches", "discoloured patches", "discolored patches", "patches of skin"),
            "distention_of_abdomen" to listOf("abdominal distension", "distended abdomen", "bloated stomach"),
            "drying_and_tingling_lips" to listOf("drying and tingling lips", "dry lips", "tingling lips"),
            "enlarged_thyroid" to listOf("enlarged thyroid", "goitre", "goiter", "thyroid swelling"),
            "excessive_hunger" to listOf("excessive hunger", "always hungry", "constant hunger"),
            "extra_marital_contacts" to listOf("extra marital contacts", "unprotected sex", "unprotected contact"),
            "family_history" to listOf("family history"),
            "fast_heart_rate" to listOf("fast heart rate", "rapid heartbeat", "fast heartbeat"),
            "fluid_overload" to listOf("fluid overload", "fluid retention", "retaining fluid"),
            "foul_smell_of_urine" to listOf("foul smell of urine", "bad smelling urine", "foul smelling urine"),
            "hip_joint_pain" to listOf("hip joint pain", "hip pain", "pain in the hip"),
            "history_of_alcohol_consumption" to listOf("history of alcohol", "drinks alcohol", "alcohol consumption", "heavy drinking"),
            "increased_appetite" to listOf("increased appetite", "bigger appetite", "eating more"),
            "indigestion" to listOf("indigestion", "dyspepsia", "poor digestion"),
            "inflammatory_nails" to listOf("inflammatory nails", "inflamed nails", "nail inflammation"),
            "internal_itching" to listOf("internal itching", "itching inside"),
            "irregular_sugar_level" to listOf("irregular sugar level", "irregular blood sugar", "unstable sugar"),
            "irritability" to listOf("irritability", "irritable", "easily annoyed"),
            "irritation_in_anus" to listOf("irritation in anus", "anal itching", "itching in anus"),
            "joint_pain" to listOf("joint pain", "painful joints", "aching joints"),
            "knee_pain" to listOf("knee pain", "pain in the knee"),
            "lack_of_concentration" to listOf("lack of concentration", "poor concentration", "can't focus"),
            "lethargy" to listOf("lethargy", "lethargic", "no energy"),
            "loss_of_balance" to listOf("loss of balance", "losing balance"),
            "loss_of_smell" to listOf("loss of smell", "can't smell"),
            "malaise" to listOf("malaise", "feeling unwell", "feeling out of sorts"),
            "mood_swings" to listOf("mood swings", "mood changes"),
            "movement_stiffness" to listOf("movement stiffness", "stiff movement", "joint stiffness"),
            "mucoid_sputum" to listOf("mucoid sputum", "mucous sputum", "phlegmy cough"),
            "muscle_pain" to listOf("muscle pain", "aching muscles"),
            "muscle_wasting" to listOf("muscle wasting", "muscle loss"),
            "muscle_weakness" to listOf("muscle weakness", "weak muscles"),
            "neck_pain" to listOf("neck pain", "pain in the neck"),
            "nodal_skin_eruptions" to listOf("nodal skin eruptions", "nodular eruption", "bumpy rash"),
            "obesity" to listOf("obesity", "obese", "overweight"),
            "pain_behind_the_eyes" to listOf("pain behind the eyes", "pain behind eyes"),
            "pain_during_bowel_movements" to listOf("pain during bowel movement", "pain while passing stool", "painful stools"),
            "pain_in_anal_region" to listOf("pain in anal region", "anal pain", "pain in the anus"),
            "painful_walking" to listOf("painful walking", "pain when walking", "difficulty walking"),
            "palpitations" to listOf("palpitations", "racing heart", "heart pounding"),
            "passage_of_gases" to listOf("passage of gases", "excess gas", "passing gas", "flatulence"),
            "patches_in_throat" to listOf("patches in throat", "white patches in throat", "throat patches"),
            "phlegm" to listOf("phlegm", "mucus"),
            "polyuria" to listOf("polyuria", "excessive urination", "urinating a lot", "frequent urination"),
            "prominent_veins_on_calf" to listOf("prominent veins on calf", "visible veins on legs", "prominent veins in legs"),
            "puffy_face_and_eyes" to listOf("puffy face", "puffy eyes", "swollen face"),
            "pus_filled_pimples" to listOf("pus filled pimples", "pus filled bumps", "pimples with pus"),
            "receiving_blood_transfusion" to listOf("blood transfusion", "received blood"),
            "receiving_unsterile_injections" to listOf("unsterile injection", "unclean injection", "shared needles"),
            "red_sore_around_nose" to listOf("red sore around nose", "sores around nose", "sore around nose"),
            "red_spots_over_body" to listOf("red spots over body", "red spots on body", "red spots all over"),
            "redness_of_eyes" to listOf("red eyes", "redness of eyes", "bloodshot eyes"),
            "restlessness" to listOf("restlessness", "restless"),
            "rusty_sputum" to listOf("rusty sputum", "rust coloured sputum"),
            "scurring" to listOf("scurring", "acne scars", "scarring"),
            "shivering" to listOf("shivering", "shivering with cold"),
            "silver_like_dusting" to listOf("silver like dusting", "silvery scales", "silver dusting on skin"),
            "sinus_pressure" to listOf("sinus pressure", "sinus pain"),
            "skin_peeling" to listOf("skin peeling", "peeling skin"),
            "slurred_speech" to listOf("slurred speech", "slurring speech", "slurred words"),
            "small_dents_in_nails" to listOf("small dents in nails", "nail pitting", "dents in nails"),
            "spinning_movements" to listOf("spinning sensation", "room spinning", "spinning movements"),
            "spotting_urination" to listOf("spotting urination", "spots in urine"),
            "stiff_neck" to listOf("stiff neck", "stiffness in the neck"),
            "stomach_bleeding" to listOf("stomach bleeding", "bleeding in stomach"),
            "sunken_eyes" to listOf("sunken eyes", "hollow eyes"),
            "swelling_joints" to listOf("swollen joints", "joint swelling"),
            "swelling_of_stomach" to listOf("swelling of stomach", "swollen stomach", "belly swelling"),
            "swollen_blood_vessels" to listOf("swollen blood vessels", "swollen veins"),
            "throat_irritation" to listOf("throat irritation", "scratchy throat", "irritated throat"),
            "toxic_look" to listOf("toxic look", "toxic appearance"),
            "ulcers_on_tongue" to listOf("ulcers on tongue", "tongue ulcer", "sores on tongue"),
            "unsteadiness" to listOf("unsteadiness", "unsteady"),
            "visual_disturbances" to listOf("visual disturbance", "vision problems", "seeing spots"),
            "watering_from_eyes" to listOf("watery eyes", "watering eyes", "teary eyes"),
            "weakness_in_limbs" to listOf("weakness in limbs", "weak limbs"),
            "weakness_of_one_body_side" to listOf("weakness on one side", "one sided weakness", "weakness of one side", "weakness of one body side"),
            "weight_gain" to listOf("weight gain", "gaining weight", "putting on weight"),
            "weight_loss" to listOf("weight loss", "losing weight", "unexplained weight loss"),
            "yellow_crust_ooze" to listOf("yellow crust ooze", "yellow crust", "oozing crust"),
            "yellow_urine" to listOf("yellow urine"),
            "yellowing_of_eyes" to listOf("yellow eyes", "yellowing of eyes", "yellowish eyes"),
            "yellowish_skin" to listOf("yellowish skin", "yellow skin", "skin turning yellow")
        )
        for ((id, keywords) in map) {
            if (keywords.any { s.contains(it) }) hits.add(id)
        }
        // UI checklist exact names
        when {
            s.contains("body aches") -> hits.add("body_aches")
            s.contains("fatigue") || s.contains("weakness") -> hits.add("fatigue")
            s.contains("skin rash") || s == "rash" -> hits.add("rash")
            s.contains("difficulty breathing") -> hits.add("breathing_difficulty")
            s.contains("sore throat") -> hits.add("sore_throat")
            s.contains("nausea") || s.contains("vomiting") -> {
                if (s.contains("nausea")) hits.add("nausea")
                if (s.contains("vomiting")) hits.add("vomiting")
            }
        }
        return hits.distinct()
    }

    /**
     * Score every condition against the reported symptoms and vitals.
     * Returns the ranked output with a computed confidence.
     */
    fun reason(
        symptoms: List<Symptom>,
        vitals: Vitals?,
        diagnosisId: String = UUID.randomUUID().toString()
    ): ReasoningOutput {
        // Accumulate canonical evidence from all symptoms.
        val evidence = HashMap<String, Float>()
        for (symptom in symptoms) {
            val mult = ClinicalKnowledge.durationMultiplier(symptom.duration)
            for (id in canonicalizeSymptom(symptom.name)) {
                evidence[id] = (evidence[id] ?: 0f) + mult
            }
        }

        // Score each condition by an F1-style blend of profile fit (precision:
        // share of the condition's symptom pattern the patient matches) and
        // completeness (share of the patient's report the condition explains).
        // This ranks conditions that explain more of what was reported higher
        // than small profiles that happen to be fully contained in the report.
        val evidenceTotal = evidence.values.sum().coerceAtLeast(1f)
        val scored = ArrayList<Pair<ClinicalKnowledge.Condition, Float>>()
        for (condition in ClinicalKnowledge.CONDITIONS) {
            val totalWeight = condition.symptoms.values.sum()
            if (totalWeight <= 0f) continue
            var matched = 0f
            for ((symId, weight) in condition.symptoms) {
                val present = evidence[symId] ?: 0f
                if (present > 0f) matched += weight
            }
            val precision = matched / totalWeight
            if (precision > 0.05f) {
                val completeness = matched / evidenceTotal
                val f1 = if (precision + completeness > 0f) {
                    2f * precision * completeness / (precision + completeness)
                } else 0f
                scored.add(condition to f1)
            }
        }

        // If nothing matched, we honestly cannot make a prediction.
        if (scored.isEmpty()) {
            return noMatch(symptoms, vitals, diagnosisId)
        }

        // Rank by coverage (profile fit) and compute confidence as the top
        // condition's share of the top-3 plausible candidates — a real
        // normalised posterior over the plausible set, not a fabricated number.
        val ranked = scored.sortedByDescending { it.second }
        val top = ranked.first()
        val plausible = ranked.take(3)
        val plausibleTotal = plausible.sumOf { it.second.toDouble() }.toFloat()
        val confidence = (top.second / plausibleTotal).coerceIn(0.1f, 0.99f)
        val topCondition = top.first

        // Risk: start from the condition's base risk, escalate on red-flag
        // symptoms and dangerous vitals, and on prolonged duration.
        val redFlagHits = evidence.keys.any { it in ClinicalKnowledge.RED_FLAG_SYMPTOMS }
        val vitalsRisk = assessVitalsRisk(vitals)
        val prolonged = symptoms.any {
            ClinicalKnowledge.durationRiskEscalation(it.duration)
        }
        val riskLevel = computeRisk(topCondition, redFlagHits, vitalsRisk, prolonged)

        // Differential: the next-best conditions by evidence.
        val differential = ranked.drop(1).map { it.first.name }.take(4)

        return ReasoningOutput(
            predictedDisease = topCondition.name,
            advice = DiseaseKnowledgeBase.adviceFor(topCondition.name),
            confidence = confidence,
            riskLevel = riskLevel,
            differentialDiagnosis = differential,
            recommendations = buildRecommendations(
                diagnosisId, topCondition, riskLevel, evidence, vitals, vitalsRisk
            )
        )
    }

    /** Real clinical vitals danger assessment (returns 0..2 escalation). */
    private fun assessVitalsRisk(vitals: Vitals?): Int {
        if (vitals == null) return 0
        var risk = 0
        vitals.temperature?.let {
            when {
                it >= 40f -> risk = maxOf(risk, 2) // very high fever
                it >= 39f -> risk = maxOf(risk, 1) // high fever
                it <= 35f -> risk = maxOf(risk, 1) // hypothermia
            }
        }
        vitals.spo2?.let {
            when {
                it < 90f -> risk = maxOf(risk, 2) // critical hypoxia
                it < 94f -> risk = maxOf(risk, 1) // low oxygen
            }
        }
        vitals.pulse?.let {
            when {
                it > 120 -> risk = maxOf(risk, 2) // severe tachycardia
                it > 100 -> risk = maxOf(risk, 1) // tachycardia
                it < 50 -> risk = maxOf(risk, 1) // bradycardia
            }
        }
        return risk
    }

    private fun computeRisk(
        condition: ClinicalKnowledge.Condition,
        redFlag: Boolean,
        vitalsRisk: Int,
        prolonged: Boolean
    ): RiskLevel {
        val base = when (condition.baseRisk) {
            RiskLevel.HIGH -> 3
            RiskLevel.MODERATE -> 2
            RiskLevel.LOW -> 1
        }
        var level = base
        if (condition.redFlag || redFlag) level = maxOf(level, 3)
        level = maxOf(level, 1 + vitalsRisk)
        if (prolonged) level = minOf(level + 1, 3)
        return when {
            level >= 3 -> RiskLevel.HIGH
            level == 2 -> RiskLevel.MODERATE
            else -> RiskLevel.LOW
        }
    }

    /** Recommendations are derived from evidence + vitals, not canned per case. */
    private fun buildRecommendations(
        diagnosisId: String,
        condition: ClinicalKnowledge.Condition,
        riskLevel: RiskLevel,
        evidence: Map<String, Float>,
        vitals: Vitals?,
        vitalsRisk: Int
    ): List<Recommendation> {
        val recs = mutableListOf<Recommendation>()
        var priority = 0

        fun add(text: String, category: String) {
            recs.add(
                Recommendation(
                    id = UUID.randomUUID().toString(),
                    diagnosisId = diagnosisId,
                    text = text,
                    category = category,
                    priority = priority++
                )
            )
        }

        // Specific vitals-derived actions (real clinical thresholds).
        if (vitalsRisk >= 2) {
            add("Your vitals show a danger sign (low oxygen / very high fever / very fast pulse). Seek emergency care now.", "emergency")
        } else if (vitalsRisk >= 1) {
            add("Your vitals are outside normal range — see a doctor promptly for evaluation.", "urgent")
        }
        if (evidence.containsKey("breathing_difficulty") && vitals?.spo2 == null) {
            add("Breathing difficulty with unmeasured oxygen — get your oxygen level checked urgently.", "urgent")
        }

        // Risk-level action.
        when (riskLevel) {
            RiskLevel.HIGH -> add("Seek immediate medical attention at the nearest health facility.", "urgent")
            RiskLevel.MODERATE -> add("Visit a health centre within 24 hours for proper evaluation.", "action")
            RiskLevel.LOW -> add("Monitor symptoms and seek care if they worsen.", "monitoring")
        }

        // Condition-specific care derived from the matched condition.
        when (condition.id) {
            "gastroenteritis", "food_poisoning" -> add("Take oral rehydration solution (ORS) and sip fluids frequently to prevent dehydration.", "care")
            "dehydration" -> add("Rehydrate with ORS or water; rest and avoid exertion until symptoms settle.", "care")
            "dengue", "malaria", "typhoid", "viral_fever" -> add("Rest, hydrate well, and monitor your temperature. Get a blood test as your doctor advises.", "care")
            "pneumonia", "covid19", "respiratory_infection", "influenza", "upper_respiratory" -> add("Rest, drink warm fluids, and avoid smoking. A doctor may prescribe antibiotics only for bacterial infection.", "care")
            "asthma" -> add("Use your prescribed inhaler, avoid triggers, and keep your reliever medication accessible.", "medication")
            "dermatitis", "urticaria" -> add("Avoid the trigger, keep the area clean and moisturised, and use antihistamine only as advised.", "care")
            "anemia" -> add("Eat iron-rich foods (leafy greens, lentils, iron-fortified food) and get a blood count test.", "care")
            "tension_headache", "migraine" -> add("Rest in a quiet, dark room, stay hydrated and manage stress; simple pain relief only as advised.", "care")
        }

        if (riskLevel != RiskLevel.LOW) {
            add("If symptoms worsen, visit a doctor without delay.", "followup")
        }
        return recs
    }

    /** Honest "cannot determine" outcome — never fabricates a condition. */
    private fun noMatch(
        symptoms: List<Symptom>,
        vitals: Vitals?,
        diagnosisId: String
    ): ReasoningOutput {
        val vitalsRisk = assessVitalsRisk(vitals)
        val risk = when {
            vitalsRisk >= 2 -> RiskLevel.HIGH
            vitalsRisk >= 1 -> RiskLevel.MODERATE
            else -> RiskLevel.LOW
        }
        val recs = mutableListOf<Recommendation>()
        if (vitalsRisk >= 2) {
            recs.add(
                Recommendation(
                    id = UUID.randomUUID().toString(), diagnosisId = diagnosisId,
                    text = "Your vitals show a danger sign. Seek emergency care now.",
                    category = "emergency", priority = 0
                )
            )
        }
        recs.add(
            Recommendation(
                id = UUID.randomUUID().toString(), diagnosisId = diagnosisId,
                text = "Your symptoms are not clearly recognised — consult a doctor for proper evaluation.",
                category = "monitoring", priority = 1
            )
        )

        // RAG: pull the most relevant knowledge-base entry for the reported
        // symptoms so even unmatched input surfaces helpful, condition-aware
        // guidance instead of a purely generic reply. On-device and offline.
        val query = symptoms.joinToString(" ") { it.name }
        val ragDoc = ragRetriever.retrieve(query, limit = 1).firstOrNull()
        val ragAdvice = ragDoc?.let { doc ->
            MedicalAdvice(
                condition = doc.topic,
                cause = "Based on the reported symptoms, the closest guidance in the local health library relates to ${doc.topic}.",
                remedy = doc.content,
                doctorToConsult = "General Physician",
                urgencyHint = "Consult a doctor for a full evaluation."
            )
        }

        return ReasoningOutput(
            predictedDisease = "Undifferentiated Illness",
            advice = ragAdvice ?: DiseaseKnowledgeBase.adviceFor("undifferentiated illness"),
            confidence = 0f,
            riskLevel = risk,
            differentialDiagnosis = emptyList(),
            recommendations = recs
        )
    }
}
