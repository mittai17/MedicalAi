package com.swasthai.app.ai.engine

import com.swasthai.app.domain.model.RiskLevel

/**
 * Clinical knowledge base for the AI doctor's text reasoning.
 *
 * Every condition maps to its associated symptoms with an evidence weight
 * (0..1) representing how strongly that symptom indicates the condition.
 * These are authored from standard medical reference associations. The
 * reasoning engine combines the user's reported symptoms with these weights
 * to compute an evidence score per condition — so predictions and confidence
 * are derived, never fabricated.
 */
object ClinicalKnowledge {

    /** Canonical symptom identifiers used across the engine. */
    data class SymptomDef(val id: String, val display: String)

    val SYMPTOMS: List<SymptomDef> = listOf(
        SymptomDef("fever", "Fever"),
        SymptomDef("cough", "Cough"),
        SymptomDef("sore_throat", "Sore Throat"),
        SymptomDef("runny_nose", "Runny Nose / Cold"),
        SymptomDef("body_aches", "Body Aches"),
        SymptomDef("fatigue", "Fatigue / Weakness"),
        SymptomDef("headache", "Headache"),
        SymptomDef("nausea", "Nausea"),
        SymptomDef("vomiting", "Vomiting"),
        SymptomDef("diarrhea", "Diarrhea"),
        SymptomDef("abdominal_pain", "Abdominal / Stomach Pain"),
        SymptomDef("rash", "Skin Rash"),
        SymptomDef("breathing_difficulty", "Difficulty Breathing"),
        SymptomDef("chest_pain", "Chest Pain"),
        SymptomDef("dizziness", "Dizziness"),
        SymptomDef("chills", "Chills"),
        SymptomDef("loss_appetite", "Loss of Appetite"),
        SymptomDef("wheezing", "Wheezing"),
        SymptomDef("itching", "Itching"),
        SymptomDef("bleeding", "Bleeding"),
        SymptomDef("seizure", "Seizure / Fits"),
        SymptomDef("unconsciousness", "Unconsciousness / Fainting"),
        SymptomDef("bloody_stool", "Blood in Stool"),
        SymptomDef("swelling", "Swelling"),
        SymptomDef("mouth_sores", "Mouth Sores"),
        SymptomDef("abnormal_menstruation", "Abnormal Periods"),
        SymptomDef("acidity", "Acidity / Heartburn"),
        SymptomDef("acute_liver_failure", "Acute Liver Failure"),
        SymptomDef("altered_sensorium", "Altered Sensorium"),
        SymptomDef("anxiety", "Anxiety"),
        SymptomDef("back_pain", "Back Pain"),
        SymptomDef("blackheads", "Blackheads"),
        SymptomDef("bladder_discomfort", "Bladder Discomfort"),
        SymptomDef("blister", "Blisters"),
        SymptomDef("blood_in_sputum", "Blood in Sputum"),
        SymptomDef("blurred_and_distorted_vision", "Blurred Vision"),
        SymptomDef("brittle_nails", "Brittle Nails"),
        SymptomDef("bruising", "Bruising"),
        SymptomDef("burning_micturition", "Burning Urination"),
        SymptomDef("cold_hands_and_feet", "Cold Hands / Feet"),
        SymptomDef("coma", "Coma / Unconsciousness"),
        SymptomDef("congestion", "Nasal Congestion"),
        SymptomDef("constipation", "Constipation"),
        SymptomDef("continuous_feel_of_urine", "Constant Urge to Urinate"),
        SymptomDef("continuous_sneezing", "Continuous Sneezing"),
        SymptomDef("cramps", "Cramps"),
        SymptomDef("dark_urine", "Dark Urine"),
        SymptomDef("dehydration_sym", "Dehydration"),
        SymptomDef("depression", "Depression"),
        SymptomDef("dischromic_patches", "Discoloured Patches"),
        SymptomDef("distention_of_abdomen", "Abdominal Distension"),
        SymptomDef("drying_and_tingling_lips", "Dry / Tingling Lips"),
        SymptomDef("enlarged_thyroid", "Enlarged Thyroid"),
        SymptomDef("excessive_hunger", "Excessive Hunger"),
        SymptomDef("extra_marital_contacts", "Unprotected Sexual Contact"),
        SymptomDef("family_history", "Family History"),
        SymptomDef("fast_heart_rate", "Fast Heart Rate"),
        SymptomDef("fluid_overload", "Fluid Retention"),
        SymptomDef("foul_smell_of_urine", "Foul-Smelling Urine"),
        SymptomDef("hip_joint_pain", "Hip Joint Pain"),
        SymptomDef("history_of_alcohol_consumption", "Alcohol History"),
        SymptomDef("increased_appetite", "Increased Appetite"),
        SymptomDef("indigestion", "Indigestion"),
        SymptomDef("inflammatory_nails", "Inflamed Nails"),
        SymptomDef("internal_itching", "Internal Itching"),
        SymptomDef("irregular_sugar_level", "Irregular Blood Sugar"),
        SymptomDef("irritability", "Irritability"),
        SymptomDef("irritation_in_anus", "Anal Irritation"),
        SymptomDef("joint_pain", "Joint Pain"),
        SymptomDef("knee_pain", "Knee Pain"),
        SymptomDef("lack_of_concentration", "Poor Concentration"),
        SymptomDef("lethargy", "Lethargy"),
        SymptomDef("loss_of_balance", "Loss of Balance"),
        SymptomDef("loss_of_smell", "Loss of Smell"),
        SymptomDef("malaise", "Malaise"),
        SymptomDef("mood_swings", "Mood Swings"),
        SymptomDef("movement_stiffness", "Movement Stiffness"),
        SymptomDef("mucoid_sputum", "Mucoid Sputum"),
        SymptomDef("muscle_pain", "Muscle Pain"),
        SymptomDef("muscle_wasting", "Muscle Wasting"),
        SymptomDef("muscle_weakness", "Muscle Weakness"),
        SymptomDef("neck_pain", "Neck Pain"),
        SymptomDef("nodal_skin_eruptions", "Nodular Skin Eruptions"),
        SymptomDef("obesity", "Obesity"),
        SymptomDef("pain_behind_the_eyes", "Pain Behind Eyes"),
        SymptomDef("pain_during_bowel_movements", "Painful Bowel Movement"),
        SymptomDef("pain_in_anal_region", "Anal Pain"),
        SymptomDef("painful_walking", "Painful Walking"),
        SymptomDef("palpitations", "Palpitations"),
        SymptomDef("passage_of_gases", "Gas / Bloating"),
        SymptomDef("patches_in_throat", "Patches in Throat"),
        SymptomDef("phlegm", "Phlegm"),
        SymptomDef("polyuria", "Excessive Urination"),
        SymptomDef("prominent_veins_on_calf", "Prominent Leg Veins"),
        SymptomDef("puffy_face_and_eyes", "Puffy Face / Eyes"),
        SymptomDef("pus_filled_pimples", "Pus-Filled Pimples"),
        SymptomDef("receiving_blood_transfusion", "Blood Transfusion"),
        SymptomDef("receiving_unsterile_injections", "Unsterile Injections"),
        SymptomDef("red_sore_around_nose", "Sores Around Nose"),
        SymptomDef("red_spots_over_body", "Red Spots on Body"),
        SymptomDef("redness_of_eyes", "Red Eyes"),
        SymptomDef("restlessness", "Restlessness"),
        SymptomDef("rusty_sputum", "Rusty-Coloured Sputum"),
        SymptomDef("scurring", "Acne Scarring"),
        SymptomDef("shivering", "Shivering"),
        SymptomDef("silver_like_dusting", "Silvery Skin Scales"),
        SymptomDef("sinus_pressure", "Sinus Pressure"),
        SymptomDef("skin_peeling", "Skin Peeling"),
        SymptomDef("slurred_speech", "Slurred Speech"),
        SymptomDef("small_dents_in_nails", "Nail Pitting"),
        SymptomDef("spinning_movements", "Spinning Sensation"),
        SymptomDef("spotting_urination", "Spotting on Urination"),
        SymptomDef("stiff_neck", "Stiff Neck"),
        SymptomDef("stomach_bleeding", "Stomach Bleeding"),
        SymptomDef("sunken_eyes", "Sunken Eyes"),
        SymptomDef("swelling_joints", "Swollen Joints"),
        SymptomDef("swelling_of_stomach", "Stomach Swelling"),
        SymptomDef("swollen_blood_vessels", "Swollen Blood Vessels"),
        SymptomDef("throat_irritation", "Throat Irritation"),
        SymptomDef("toxic_look", "Toxic Look (Typhoid)"),
        SymptomDef("ulcers_on_tongue", "Tongue Ulcers"),
        SymptomDef("unsteadiness", "Unsteadiness"),
        SymptomDef("visual_disturbances", "Visual Disturbances"),
        SymptomDef("watering_from_eyes", "Watery Eyes"),
        SymptomDef("weakness_in_limbs", "Weakness in Limbs"),
        SymptomDef("weakness_of_one_body_side", "One-Sided Weakness"),
        SymptomDef("weight_gain", "Weight Gain"),
        SymptomDef("weight_loss", "Weight Loss"),
        SymptomDef("yellow_crust_ooze", "Yellow Crusting"),
        SymptomDef("yellow_urine", "Yellow Urine"),
        SymptomDef("yellowing_of_eyes", "Yellow Eyes"),
        SymptomDef("yellowish_skin", "Yellowish Skin")
    )

    /**
     * A condition the doctor can reason about. [symptoms] maps a canonical
     * symptom id to an evidence weight (0..1).
     */
    data class Condition(
        val id: String,
        val name: String,
        val baseRisk: RiskLevel,
        val redFlag: Boolean = false,
        val symptoms: Map<String, Float>
    )

    val CONDITIONS: List<Condition> = listOf(
        Condition("respiratory_infection", "Respiratory Infection", RiskLevel.MODERATE, symptoms = mapOf(
            "cough" to 0.9f, "sore_throat" to 0.6f, "fever" to 0.6f, "runny_nose" to 0.5f,
            "fatigue" to 0.4f, "body_aches" to 0.4f
        )),
        Condition("viral_fever", "Viral Fever", RiskLevel.MODERATE, symptoms = mapOf(
            "fever" to 0.9f, "body_aches" to 0.7f, "headache" to 0.6f, "fatigue" to 0.6f, "chills" to 0.6f
        )),
        Condition("upper_respiratory", "Upper Respiratory Tract Infection", RiskLevel.LOW, symptoms = mapOf(
            "sore_throat" to 0.9f, "runny_nose" to 0.8f, "cough" to 0.5f, "fever" to 0.3f, "headache" to 0.3f
        )),
        Condition("influenza", "Influenza (Flu)", RiskLevel.MODERATE, symptoms = mapOf(
            "fever" to 0.9f, "body_aches" to 0.9f, "cough" to 0.7f, "fatigue" to 0.7f,
            "headache" to 0.6f, "chills" to 0.6f, "sore_throat" to 0.5f
        )),
        Condition("pneumonia", "Pneumonia", RiskLevel.HIGH, redFlag = true, symptoms = mapOf(
            "cough" to 1.0f, "fever" to 1.0f, "breathing_difficulty" to 1.0f, "chest_pain" to 0.7f,
            "fatigue" to 0.6f, "chills" to 0.6f, "rusty_sputum" to 0.8f, "phlegm" to 0.5f
        )),
        Condition("covid19", "COVID-19 / Viral Pneumonia", RiskLevel.MODERATE, redFlag = true, symptoms = mapOf(
            "fever" to 0.8f, "cough" to 0.8f, "breathing_difficulty" to 0.7f, "fatigue" to 0.7f,
            "sore_throat" to 0.5f, "headache" to 0.5f, "loss_appetite" to 0.4f
        )),
        Condition("gastroenteritis", "Gastroenteritis", RiskLevel.MODERATE, symptoms = mapOf(
            "diarrhea" to 0.95f, "vomiting" to 0.9f, "nausea" to 0.7f, "abdominal_pain" to 0.6f,
            "dehydration_sym" to 0.9f, "sunken_eyes" to 0.9f, "fever" to 0.4f, "fatigue" to 0.4f
        )),
        Condition("food_poisoning", "Food Poisoning", RiskLevel.MODERATE, symptoms = mapOf(
            "vomiting" to 0.9f, "nausea" to 0.8f, "abdominal_pain" to 0.7f, "diarrhea" to 0.7f, "fever" to 0.4f
        )),
        Condition("dermatitis", "Dermatitis / Skin Allergy", RiskLevel.LOW, symptoms = mapOf(
            "rash" to 0.9f, "itching" to 0.8f, "swelling" to 0.4f
        )),
        Condition("urticaria", "Allergic Reaction (Urticaria)", RiskLevel.MODERATE, redFlag = true, symptoms = mapOf(
            "rash" to 0.8f, "itching" to 0.8f, "swelling" to 0.7f, "breathing_difficulty" to 0.5f, "dizziness" to 0.4f
        )),
        Condition("tension_headache", "Tension Headache", RiskLevel.LOW, symptoms = mapOf(
            "headache" to 0.9f, "fatigue" to 0.4f, "dizziness" to 0.3f
        )),
        Condition("migraine", "Migraine", RiskLevel.LOW, symptoms = mapOf(
            "headache" to 0.9f, "nausea" to 0.6f, "vomiting" to 0.4f, "dizziness" to 0.4f,
            "acidity" to 0.8f, "blurred_and_distorted_vision" to 0.8f, "depression" to 0.8f,
            "excessive_hunger" to 0.8f, "indigestion" to 0.8f, "irritability" to 0.8f,
            "stiff_neck" to 0.8f, "visual_disturbances" to 0.8f
        )),
        Condition("dehydration", "Dehydration", RiskLevel.MODERATE, symptoms = mapOf(
            "dizziness" to 0.8f, "fatigue" to 0.7f, "nausea" to 0.5f, "headache" to 0.4f,
            "vomiting" to 0.4f, "diarrhea" to 0.4f
        )),
        Condition("anemia", "Anemia / Iron Deficiency", RiskLevel.LOW, symptoms = mapOf(
            "fatigue" to 0.9f, "dizziness" to 0.6f, "headache" to 0.5f, "breathing_difficulty" to 0.4f
        )),
        Condition("hypoglycemia", "Low Blood Sugar", RiskLevel.MODERATE, redFlag = true, symptoms = mapOf(
            "dizziness" to 0.8f, "fatigue" to 0.8f, "nausea" to 0.8f, "headache" to 0.8f, "sweating" to 0.8f,
            "anxiety" to 0.8f, "blurred_and_distorted_vision" to 0.8f, "drying_and_tingling_lips" to 0.8f,
            "excessive_hunger" to 0.8f, "irritability" to 0.8f, "palpitations" to 0.8f, "slurred_speech" to 0.8f,
            "vomiting" to 0.8f
        )),
        Condition("asthma", "Asthma", RiskLevel.MODERATE, redFlag = true, symptoms = mapOf(
            "breathing_difficulty" to 0.9f, "wheezing" to 0.9f, "cough" to 0.8f, "chest_pain" to 0.4f,
            "family_history" to 0.8f, "fatigue" to 0.8f, "fever" to 0.8f, "mucoid_sputum" to 0.8f
        )),
        Condition("dengue", "Dengue Fever", RiskLevel.HIGH, redFlag = true, symptoms = mapOf(
            "fever" to 0.9f, "body_aches" to 0.8f, "headache" to 0.8f, "rash" to 0.8f,
            "nausea" to 0.8f, "vomiting" to 0.8f, "bleeding" to 0.4f, "back_pain" to 0.8f,
            "chills" to 0.8f, "fatigue" to 0.8f, "joint_pain" to 0.8f, "loss_appetite" to 0.8f,
            "malaise" to 0.8f, "muscle_pain" to 0.8f, "pain_behind_the_eyes" to 0.8f,
            "red_spots_over_body" to 0.8f
        )),
        Condition("malaria", "Malaria", RiskLevel.HIGH, redFlag = true, symptoms = mapOf(
            "fever" to 0.9f, "chills" to 0.8f, "body_aches" to 0.6f, "headache" to 0.8f,
            "nausea" to 0.8f, "vomiting" to 0.8f, "diarrhea" to 0.8f, "muscle_pain" to 0.8f,
            "sweating" to 0.8f
        )),
        Condition("typhoid", "Typhoid Fever", RiskLevel.MODERATE, symptoms = mapOf(
            "fever" to 0.9f, "fatigue" to 0.8f, "headache" to 0.8f, "abdominal_pain" to 0.8f,
            "loss_appetite" to 0.5f, "diarrhea" to 0.8f, "chills" to 0.8f, "constipation" to 0.8f,
            "nausea" to 0.8f, "toxic_look" to 0.8f, "vomiting" to 0.8f
        )),
        Condition("hypertension", "High Blood Pressure", RiskLevel.MODERATE, symptoms = mapOf(
            "headache" to 0.7f, "dizziness" to 0.7f, "chest_pain" to 0.5f, "loss_of_balance" to 0.7f,
            "lack_of_concentration" to 0.5f
        )),
        Condition("oral_infection", "Oral / Mouth Infection", RiskLevel.LOW, symptoms = mapOf(
            "mouth_sores" to 0.9f, "sore_throat" to 0.5f, "fever" to 0.3f
        )),
        Condition("general_debility", "General Debility", RiskLevel.LOW, symptoms = mapOf(
            "fatigue" to 0.6f, "body_aches" to 0.4f, "headache" to 0.3f
        )),
        Condition("vertigo", "Vertigo (Positional Vertigo)", RiskLevel.MODERATE, symptoms = mapOf("vomiting" to 0.95f, "headache" to 0.95f, "nausea" to 0.95f, "loss_of_balance" to 0.95f, "unsteadiness" to 0.95f, "spinning_movements" to 0.90f)),
        Condition("aids", "HIV / AIDS", RiskLevel.HIGH, redFlag = true, symptoms = mapOf("fever" to 0.95f, "muscle_wasting" to 0.90f, "patches_in_throat" to 0.90f, "extra_marital_contacts" to 0.90f)),
        Condition("acne", "Acne", RiskLevel.LOW, symptoms = mapOf("rash" to 0.95f, "pus_filled_pimples" to 0.90f, "blackheads" to 0.90f, "scurring" to 0.90f)),
        Condition("alcoholic_hepatitis", "Alcoholic hepatitis", RiskLevel.MODERATE, symptoms = mapOf("vomiting" to 0.95f, "yellowish_skin" to 0.95f, "abdominal_pain" to 0.95f, "swelling_of_stomach" to 0.95f, "distention_of_abdomen" to 0.95f, "history_of_alcohol_consumption" to 0.95f, "fluid_overload" to 0.95f)),
        Condition("allergy", "Allergy", RiskLevel.MODERATE, symptoms = mapOf("continuous_sneezing" to 0.90f, "shivering" to 0.90f, "chills" to 0.90f, "watering_from_eyes" to 0.90f)),
        Condition("arthritis", "Arthritis", RiskLevel.MODERATE, symptoms = mapOf("muscle_weakness" to 0.95f, "stiff_neck" to 0.95f, "swelling_joints" to 0.95f, "movement_stiffness" to 0.95f, "painful_walking" to 0.95f)),
        Condition("cervical_spondylosis", "Cervical spondylosis", RiskLevel.MODERATE, symptoms = mapOf("neck_pain" to 0.95f, "dizziness" to 0.95f, "loss_of_balance" to 0.95f, "back_pain" to 0.90f, "weakness_in_limbs" to 0.90f)),
        Condition("chicken_pox", "Chicken pox", RiskLevel.MODERATE, symptoms = mapOf("malaise" to 1.00f, "red_spots_over_body" to 1.00f, "itching" to 0.95f, "rash" to 0.95f, "fatigue" to 0.95f, "lethargy" to 0.95f, "fever" to 0.95f, "headache" to 0.95f, "loss_appetite" to 0.95f, "swelling" to 0.95f)),
        Condition("chronic_cholestasis", "Chronic cholestasis", RiskLevel.MODERATE, symptoms = mapOf("itching" to 0.95f, "vomiting" to 0.95f, "yellowish_skin" to 0.95f, "nausea" to 0.95f, "loss_appetite" to 0.95f, "abdominal_pain" to 0.95f, "yellowing_of_eyes" to 0.95f)),
        Condition("common_cold", "Common Cold", RiskLevel.LOW, symptoms = mapOf("phlegm" to 1.00f, "throat_irritation" to 1.00f, "redness_of_eyes" to 1.00f, "sinus_pressure" to 1.00f, "runny_nose" to 1.00f, "congestion" to 1.00f, "chest_pain" to 1.00f, "loss_of_smell" to 1.00f, "muscle_pain" to 1.00f, "continuous_sneezing" to 0.95f, "chills" to 0.95f, "fatigue" to 0.95f, "cough" to 0.95f, "fever" to 0.95f, "headache" to 0.95f, "swelling" to 0.95f, "malaise" to 0.95f)),
        Condition("diabetes", "Diabetes", RiskLevel.MODERATE, symptoms = mapOf("increased_appetite" to 1.00f, "polyuria" to 1.00f, "fatigue" to 0.95f, "weight_loss" to 0.95f, "restlessness" to 0.95f, "lethargy" to 0.95f, "irregular_sugar_level" to 0.95f, "blurred_and_distorted_vision" to 0.95f, "obesity" to 0.95f, "excessive_hunger" to 0.95f)),
        Condition("piles", "Piles (Hemorrhoids)", RiskLevel.MODERATE, symptoms = mapOf("constipation" to 0.95f, "pain_during_bowel_movements" to 0.95f, "pain_in_anal_region" to 0.95f, "bloody_stool" to 0.95f, "irritation_in_anus" to 0.95f)),
        Condition("drug_reaction", "Drug Reaction", RiskLevel.MODERATE, symptoms = mapOf("itching" to 0.95f, "rash" to 0.90f, "abdominal_pain" to 0.90f, "burning_micturition" to 0.90f, "spotting_urination" to 0.90f)),
        Condition("fungal_infection", "Fungal infection", RiskLevel.LOW, symptoms = mapOf("itching" to 0.90f, "rash" to 0.90f, "nodal_skin_eruptions" to 0.90f, "dischromic_patches" to 0.90f)),
        Condition("gerd", "GERD", RiskLevel.MODERATE, symptoms = mapOf("abdominal_pain" to 0.95f, "cough" to 0.95f, "chest_pain" to 0.95f, "acidity" to 0.90f, "ulcers_on_tongue" to 0.90f, "vomiting" to 0.90f)),
        Condition("heart_attack", "Heart attack", RiskLevel.HIGH, redFlag = true, symptoms = mapOf("chest_pain" to 0.95f, "vomiting" to 0.90f, "breathing_difficulty" to 0.90f, "sweating" to 0.90f)),
        Condition("hepatitis_b", "Hepatitis B", RiskLevel.HIGH, redFlag = true, symptoms = mapOf("yellowing_of_eyes" to 1.00f, "malaise" to 1.00f, "receiving_blood_transfusion" to 1.00f, "receiving_unsterile_injections" to 1.00f, "itching" to 0.95f, "fatigue" to 0.95f, "lethargy" to 0.95f, "yellowish_skin" to 0.95f, "dark_urine" to 0.95f, "loss_appetite" to 0.95f, "abdominal_pain" to 0.95f, "yellow_urine" to 0.95f)),
        Condition("hepatitis_c", "Hepatitis C", RiskLevel.HIGH, redFlag = true, symptoms = mapOf("fatigue" to 0.95f, "yellowish_skin" to 0.95f, "nausea" to 0.95f, "loss_appetite" to 0.95f, "family_history" to 0.95f, "yellowing_of_eyes" to 0.90f)),
        Condition("hepatitis_d", "Hepatitis D", RiskLevel.HIGH, redFlag = true, symptoms = mapOf("joint_pain" to 0.95f, "vomiting" to 0.95f, "fatigue" to 0.95f, "yellowish_skin" to 0.95f, "dark_urine" to 0.95f, "nausea" to 0.95f, "loss_appetite" to 0.95f, "abdominal_pain" to 0.95f, "yellowing_of_eyes" to 0.95f)),
        Condition("hepatitis_e", "Hepatitis E", RiskLevel.MODERATE, symptoms = mapOf("loss_appetite" to 1.00f, "abdominal_pain" to 1.00f, "yellowing_of_eyes" to 1.00f, "coma" to 1.00f, "stomach_bleeding" to 1.00f, "joint_pain" to 0.95f, "vomiting" to 0.95f, "fatigue" to 0.95f, "fever" to 0.95f, "yellowish_skin" to 0.95f, "dark_urine" to 0.95f, "nausea" to 0.95f, "acute_liver_failure" to 0.95f)),
        Condition("hyperthyroidism", "Hyperthyroidism", RiskLevel.MODERATE, symptoms = mapOf("muscle_weakness" to 1.00f, "irritability" to 1.00f, "abnormal_menstruation" to 1.00f, "fatigue" to 0.95f, "mood_swings" to 0.95f, "weight_loss" to 0.95f, "restlessness" to 0.95f, "sweating" to 0.95f, "diarrhea" to 0.95f, "fast_heart_rate" to 0.95f, "excessive_hunger" to 0.95f)),
        Condition("hypothyroidism", "Hypothyroidism", RiskLevel.MODERATE, symptoms = mapOf("enlarged_thyroid" to 1.00f, "brittle_nails" to 1.00f, "swelling" to 1.00f, "depression" to 1.00f, "irritability" to 1.00f, "abnormal_menstruation" to 1.00f, "weight_gain" to 0.95f, "cold_hands_and_feet" to 0.95f, "mood_swings" to 0.95f, "lethargy" to 0.95f, "dizziness" to 0.95f, "puffy_face_and_eyes" to 0.95f, "fatigue" to 0.90f)),
        Condition("impetigo", "Impetigo", RiskLevel.LOW, symptoms = mapOf("rash" to 0.95f, "blister" to 0.95f, "red_sore_around_nose" to 0.95f, "yellow_crust_ooze" to 0.95f, "fever" to 0.85f)),
        Condition("jaundice", "Jaundice", RiskLevel.MODERATE, symptoms = mapOf("itching" to 0.95f, "vomiting" to 0.95f, "fatigue" to 0.95f, "weight_loss" to 0.95f, "fever" to 0.95f, "yellowish_skin" to 0.95f, "dark_urine" to 0.95f, "abdominal_pain" to 0.95f)),
        Condition("osteoarthritis", "Osteoarthritis", RiskLevel.MODERATE, symptoms = mapOf("joint_pain" to 0.95f, "neck_pain" to 0.95f, "knee_pain" to 0.95f, "hip_joint_pain" to 0.95f, "swelling_joints" to 0.95f, "painful_walking" to 0.95f)),
        Condition("paralysis", "Paralysis (Suspected Stroke)", RiskLevel.HIGH, redFlag = true, symptoms = mapOf("altered_sensorium" to 0.95f, "vomiting" to 0.90f, "headache" to 0.90f, "weakness_of_one_body_side" to 0.90f)),
        Condition("peptic_ulcer", "Peptic Ulcer Disease", RiskLevel.MODERATE, symptoms = mapOf("vomiting" to 0.95f, "abdominal_pain" to 0.95f, "passage_of_gases" to 0.95f, "internal_itching" to 0.95f, "indigestion" to 0.90f, "loss_appetite" to 0.90f)),
        Condition("psoriasis", "Psoriasis", RiskLevel.LOW, symptoms = mapOf("rash" to 0.95f, "joint_pain" to 0.95f, "skin_peeling" to 0.95f, "silver_like_dusting" to 0.95f, "small_dents_in_nails" to 0.95f, "inflammatory_nails" to 0.95f)),
        Condition("tuberculosis", "Tuberculosis", RiskLevel.HIGH, redFlag = true, symptoms = mapOf("loss_appetite" to 1.00f, "fever" to 1.00f, "yellowing_of_eyes" to 1.00f, "swelling" to 1.00f, "malaise" to 1.00f, "phlegm" to 1.00f, "chest_pain" to 1.00f, "blood_in_sputum" to 1.00f, "chills" to 0.95f, "vomiting" to 0.95f, "fatigue" to 0.95f, "weight_loss" to 0.95f, "cough" to 0.95f, "breathing_difficulty" to 0.95f, "sweating" to 0.95f)),
        Condition("urinary_tract_infection", "Urinary Tract Infection", RiskLevel.MODERATE, symptoms = mapOf("bladder_discomfort" to 0.95f, "continuous_feel_of_urine" to 0.95f, "burning_micturition" to 0.90f, "foul_smell_of_urine" to 0.85f)),
        Condition("varicose_veins", "Varicose veins", RiskLevel.LOW, symptoms = mapOf("fatigue" to 0.95f, "cramps" to 0.95f, "bruising" to 0.95f, "obesity" to 0.95f, "swelling" to 0.95f, "prominent_veins_on_calf" to 0.95f, "swollen_blood_vessels" to 0.90f)),
        Condition("hepatitis_a", "Hepatitis A", RiskLevel.MODERATE, symptoms = mapOf("fever" to 1.00f, "yellowing_of_eyes" to 1.00f, "muscle_pain" to 1.00f, "joint_pain" to 0.95f, "vomiting" to 0.95f, "yellowish_skin" to 0.95f, "dark_urine" to 0.95f, "nausea" to 0.95f, "loss_appetite" to 0.95f, "abdominal_pain" to 0.95f, "diarrhea" to 0.95f))
    )

    /** Red-flag symptoms that escalate risk regardless of match score. */
    val RED_FLAG_SYMPTOMS: Set<String> = setOf(
        "breathing_difficulty", "chest_pain", "seizure", "unconsciousness",
        "bleeding", "bloody_stool", "high_fever"
    )

    /** Duration multipliers — prolonged symptoms raise evidence and risk. */
    fun durationMultiplier(duration: String?): Float = when (duration) {
        "More than 7 days" -> 1.4f
        "3 – 7 days" -> 1.2f
        "1 – 3 days" -> 1.05f
        else -> 1.0f
    }

    fun durationRiskEscalation(duration: String?): Boolean = duration == "More than 7 days"
}
