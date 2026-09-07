/**
 * Clinical conditions with evidence-weighted symptom profiles.
 * Ported verbatim from ClinicalKnowledge.CONDITIONS.
 */
export const CONDITIONS = [
    {
        id: "respiratory_infection",
        name: "Respiratory Infection",
        baseRisk: "moderate",
        symptoms: { cough: 0.9, sore_throat: 0.6, fever: 0.6, runny_nose: 0.5, fatigue: 0.4, body_aches: 0.4 },
    },
    {
        id: "viral_fever",
        name: "Viral Fever",
        baseRisk: "moderate",
        symptoms: { fever: 0.9, body_aches: 0.7, headache: 0.6, fatigue: 0.6, chills: 0.6 },
    },
    {
        id: "upper_respiratory",
        name: "Upper Respiratory Tract Infection",
        baseRisk: "low",
        symptoms: { sore_throat: 0.9, runny_nose: 0.8, cough: 0.5, fever: 0.3, headache: 0.3 },
    },
    {
        id: "influenza",
        name: "Influenza (Flu)",
        baseRisk: "moderate",
        symptoms: { fever: 0.9, body_aches: 0.9, cough: 0.7, fatigue: 0.7, headache: 0.6, chills: 0.6, sore_throat: 0.5 },
    },
    {
        id: "pneumonia",
        name: "Pneumonia",
        baseRisk: "high",
        redFlag: true,
        symptoms: { cough: 1.0, fever: 1.0, breathing_difficulty: 1.0, chest_pain: 0.7, fatigue: 0.6, chills: 0.6, rusty_sputum: 0.8, phlegm: 0.5 },
    },
    {
        id: "covid19",
        name: "COVID-19 / Viral Pneumonia",
        baseRisk: "moderate",
        redFlag: true,
        symptoms: { fever: 0.8, cough: 0.8, breathing_difficulty: 0.7, fatigue: 0.7, sore_throat: 0.5, headache: 0.5, loss_appetite: 0.4 },
    },
    {
        id: "gastroenteritis",
        name: "Gastroenteritis",
        baseRisk: "moderate",
        symptoms: { diarrhea: 0.95, vomiting: 0.9, nausea: 0.7, abdominal_pain: 0.6, dehydration_sym: 0.9, sunken_eyes: 0.9, fever: 0.4, fatigue: 0.4 },
    },
    {
        id: "food_poisoning",
        name: "Food Poisoning",
        baseRisk: "moderate",
        symptoms: { vomiting: 0.9, nausea: 0.8, abdominal_pain: 0.7, diarrhea: 0.7, fever: 0.4 },
    },
    {
        id: "dermatitis",
        name: "Dermatitis / Skin Allergy",
        baseRisk: "low",
        symptoms: { rash: 0.9, itching: 0.8, swelling: 0.4 },
    },
    {
        id: "urticaria",
        name: "Allergic Reaction (Urticaria)",
        baseRisk: "moderate",
        redFlag: true,
        symptoms: { rash: 0.8, itching: 0.8, swelling: 0.7, breathing_difficulty: 0.5, dizziness: 0.4 },
    },
    {
        id: "tension_headache",
        name: "Tension Headache",
        baseRisk: "low",
        symptoms: { headache: 0.9, fatigue: 0.4, dizziness: 0.3 },
    },
    {
        id: "migraine",
        name: "Migraine",
        baseRisk: "low",
        symptoms: {
            headache: 0.9, nausea: 0.6, vomiting: 0.4, dizziness: 0.4, acidity: 0.8,
            blurred_and_distorted_vision: 0.8, depression: 0.8, excessive_hunger: 0.8,
            indigestion: 0.8, irritability: 0.8, stiff_neck: 0.8, visual_disturbances: 0.8,
        },
    },
    {
        id: "dehydration",
        name: "Dehydration",
        baseRisk: "moderate",
        symptoms: { dizziness: 0.8, fatigue: 0.7, nausea: 0.5, headache: 0.4, vomiting: 0.4, diarrhea: 0.4 },
    },
    {
        id: "anemia",
        name: "Anemia / Iron Deficiency",
        baseRisk: "low",
        symptoms: { fatigue: 0.9, dizziness: 0.6, headache: 0.5, breathing_difficulty: 0.4 },
    },
    {
        id: "hypoglycemia",
        name: "Low Blood Sugar",
        baseRisk: "moderate",
        redFlag: true,
        symptoms: {
            dizziness: 0.8, fatigue: 0.8, nausea: 0.8, headache: 0.8, sweating: 0.8,
            anxiety: 0.8, blurred_and_distorted_vision: 0.8, drying_and_tingling_lips: 0.8,
            excessive_hunger: 0.8, irritability: 0.8, palpitations: 0.8, slurred_speech: 0.8,
            vomiting: 0.8,
        },
    },
    {
        id: "asthma",
        name: "Asthma",
        baseRisk: "moderate",
        redFlag: true,
        symptoms: { breathing_difficulty: 0.9, wheezing: 0.9, cough: 0.8, chest_pain: 0.4, family_history: 0.8, fatigue: 0.8, fever: 0.8, mucoid_sputum: 0.8 },
    },
    {
        id: "dengue",
        name: "Dengue Fever",
        baseRisk: "high",
        redFlag: true,
        symptoms: {
            fever: 0.9, body_aches: 0.8, headache: 0.8, rash: 0.8, nausea: 0.8, vomiting: 0.8,
            bleeding: 0.4, back_pain: 0.8, chills: 0.8, fatigue: 0.8, joint_pain: 0.8,
            loss_appetite: 0.8, malaise: 0.8, muscle_pain: 0.8, pain_behind_the_eyes: 0.8,
            red_spots_over_body: 0.8,
        },
    },
    {
        id: "malaria",
        name: "Malaria",
        baseRisk: "high",
        redFlag: true,
        symptoms: { fever: 0.9, chills: 0.8, body_aches: 0.6, headache: 0.8, nausea: 0.8, vomiting: 0.8, diarrhea: 0.8, muscle_pain: 0.8, sweating: 0.8 },
    },
    {
        id: "typhoid",
        name: "Typhoid Fever",
        baseRisk: "moderate",
        symptoms: { fever: 0.9, fatigue: 0.8, headache: 0.8, abdominal_pain: 0.8, loss_appetite: 0.5, diarrhea: 0.8, chills: 0.8, constipation: 0.8, nausea: 0.8, toxic_look: 0.8, vomiting: 0.8 },
    },
    {
        id: "hypertension",
        name: "High Blood Pressure",
        baseRisk: "moderate",
        symptoms: { headache: 0.7, dizziness: 0.7, chest_pain: 0.5, loss_of_balance: 0.7, lack_of_concentration: 0.5 },
    },
    {
        id: "oral_infection",
        name: "Oral / Mouth Infection",
        baseRisk: "low",
        symptoms: { mouth_sores: 0.9, sore_throat: 0.5, fever: 0.3 },
    },
    {
        id: "general_debility",
        name: "General Debility",
        baseRisk: "low",
        symptoms: { fatigue: 0.6, body_aches: 0.4, headache: 0.3 },
    },
    {
        id: "vertigo",
        name: "Vertigo (Positional Vertigo)",
        baseRisk: "moderate",
        symptoms: { vomiting: 0.95, headache: 0.95, nausea: 0.95, loss_of_balance: 0.95, unsteadiness: 0.95, spinning_movements: 0.9 },
    },
    {
        id: "aids",
        name: "HIV / AIDS",
        baseRisk: "high",
        redFlag: true,
        symptoms: { fever: 0.95, muscle_wasting: 0.9, patches_in_throat: 0.9, extra_marital_contacts: 0.9 },
    },
    {
        id: "acne",
        name: "Acne",
        baseRisk: "low",
        symptoms: { rash: 0.95, pus_filled_pimples: 0.9, blackheads: 0.9, scurring: 0.9 },
    },
    {
        id: "alcoholic_hepatitis",
        name: "Alcoholic hepatitis",
        baseRisk: "moderate",
        symptoms: { vomiting: 0.95, yellowish_skin: 0.95, abdominal_pain: 0.95, swelling_of_stomach: 0.95, distention_of_abdomen: 0.95, history_of_alcohol_consumption: 0.95, fluid_overload: 0.95 },
    },
    {
        id: "allergy",
        name: "Allergy",
        baseRisk: "moderate",
        symptoms: { continuous_sneezing: 0.9, shivering: 0.9, chills: 0.9, watering_from_eyes: 0.9 },
    },
    {
        id: "arthritis",
        name: "Arthritis",
        baseRisk: "moderate",
        symptoms: { muscle_weakness: 0.95, stiff_neck: 0.95, swelling_joints: 0.95, movement_stiffness: 0.95, painful_walking: 0.95 },
    },
    {
        id: "cervical_spondylosis",
        name: "Cervical spondylosis",
        baseRisk: "moderate",
        symptoms: { neck_pain: 0.95, dizziness: 0.95, loss_of_balance: 0.95, back_pain: 0.9, weakness_in_limbs: 0.9 },
    },
    {
        id: "chicken_pox",
        name: "Chicken pox",
        baseRisk: "moderate",
        symptoms: { malaise: 1.0, red_spots_over_body: 1.0, itching: 0.95, rash: 0.95, fatigue: 0.95, lethargy: 0.95, fever: 0.95, headache: 0.95, loss_appetite: 0.95, swelling: 0.95 },
    },
    {
        id: "chronic_cholestasis",
        name: "Chronic cholestasis",
        baseRisk: "moderate",
        symptoms: { itching: 0.95, vomiting: 0.95, yellowish_skin: 0.95, nausea: 0.95, loss_appetite: 0.95, abdominal_pain: 0.95, yellowing_of_eyes: 0.95 },
    },
    {
        id: "common_cold",
        name: "Common Cold",
        baseRisk: "low",
        symptoms: {
            phlegm: 1.0, throat_irritation: 1.0, redness_of_eyes: 1.0, sinus_pressure: 1.0,
            runny_nose: 1.0, congestion: 1.0, chest_pain: 1.0, loss_of_smell: 1.0, muscle_pain: 1.0,
            continuous_sneezing: 0.95, chills: 0.95, fatigue: 0.95, cough: 0.95, fever: 0.95,
            headache: 0.95, swelling: 0.95, malaise: 0.95,
        },
    },
    {
        id: "diabetes",
        name: "Diabetes",
        baseRisk: "moderate",
        symptoms: {
            increased_appetite: 1.0, polyuria: 1.0, fatigue: 0.95, weight_loss: 0.95,
            restlessness: 0.95, lethargy: 0.95, irregular_sugar_level: 0.95,
            blurred_and_distorted_vision: 0.95, obesity: 0.95, excessive_hunger: 0.95,
        },
    },
    {
        id: "piles",
        name: "Piles (Hemorrhoids)",
        baseRisk: "moderate",
        symptoms: { constipation: 0.95, pain_during_bowel_movements: 0.95, pain_in_anal_region: 0.95, bloody_stool: 0.95, irritation_in_anus: 0.95 },
    },
    {
        id: "drug_reaction",
        name: "Drug Reaction",
        baseRisk: "moderate",
        symptoms: { itching: 0.95, rash: 0.9, abdominal_pain: 0.9, burning_micturition: 0.9, spotting_urination: 0.9 },
    },
    {
        id: "fungal_infection",
        name: "Fungal infection",
        baseRisk: "low",
        symptoms: { itching: 0.9, rash: 0.9, nodal_skin_eruptions: 0.9, dischromic_patches: 0.9 },
    },
    {
        id: "gerd",
        name: "GERD",
        baseRisk: "moderate",
        symptoms: { abdominal_pain: 0.95, cough: 0.95, chest_pain: 0.95, acidity: 0.9, ulcers_on_tongue: 0.9, vomiting: 0.9 },
    },
    {
        id: "heart_attack",
        name: "Heart attack",
        baseRisk: "high",
        redFlag: true,
        symptoms: { chest_pain: 0.95, vomiting: 0.9, breathing_difficulty: 0.9, sweating: 0.9 },
    },
    {
        id: "hepatitis_b",
        name: "Hepatitis B",
        baseRisk: "high",
        redFlag: true,
        symptoms: {
            yellowing_of_eyes: 1.0, malaise: 1.0, receiving_blood_transfusion: 1.0,
            receiving_unsterile_injections: 1.0, itching: 0.95, fatigue: 0.95, lethargy: 0.95,
            yellowish_skin: 0.95, dark_urine: 0.95, loss_appetite: 0.95, abdominal_pain: 0.95,
            yellow_urine: 0.95,
        },
    },
    {
        id: "hepatitis_c",
        name: "Hepatitis C",
        baseRisk: "high",
        redFlag: true,
        symptoms: { fatigue: 0.95, yellowish_skin: 0.95, nausea: 0.95, loss_appetite: 0.95, family_history: 0.95, yellowing_of_eyes: 0.9 },
    },
    {
        id: "hepatitis_d",
        name: "Hepatitis D",
        baseRisk: "high",
        redFlag: true,
        symptoms: { joint_pain: 0.95, vomiting: 0.95, fatigue: 0.95, yellowish_skin: 0.95, dark_urine: 0.95, nausea: 0.95, loss_appetite: 0.95, abdominal_pain: 0.95, yellowing_of_eyes: 0.95 },
    },
    {
        id: "hepatitis_e",
        name: "Hepatitis E",
        baseRisk: "moderate",
        symptoms: {
            loss_appetite: 1.0, abdominal_pain: 1.0, yellowing_of_eyes: 1.0, coma: 1.0,
            stomach_bleeding: 1.0, joint_pain: 0.95, vomiting: 0.95, fatigue: 0.95, fever: 0.95,
            yellowish_skin: 0.95, dark_urine: 0.95, nausea: 0.95, acute_liver_failure: 0.95,
        },
    },
    {
        id: "hyperthyroidism",
        name: "Hyperthyroidism",
        baseRisk: "moderate",
        symptoms: {
            muscle_weakness: 1.0, irritability: 1.0, abnormal_menstruation: 1.0, fatigue: 0.95,
            mood_swings: 0.95, weight_loss: 0.95, restlessness: 0.95, sweating: 0.95,
            diarrhea: 0.95, fast_heart_rate: 0.95, excessive_hunger: 0.95,
        },
    },
    {
        id: "hypothyroidism",
        name: "Hypothyroidism",
        baseRisk: "moderate",
        symptoms: {
            enlarged_thyroid: 1.0, brittle_nails: 1.0, swelling: 1.0, depression: 1.0,
            irritability: 1.0, abnormal_menstruation: 1.0, weight_gain: 0.95,
            cold_hands_and_feet: 0.95, mood_swings: 0.95, lethargy: 0.95, dizziness: 0.95,
            puffy_face_and_eyes: 0.95, fatigue: 0.9,
        },
    },
    {
        id: "impetigo",
        name: "Impetigo",
        baseRisk: "low",
        symptoms: { rash: 0.95, blister: 0.95, red_sore_around_nose: 0.95, yellow_crust_ooze: 0.95, fever: 0.85 },
    },
    {
        id: "jaundice",
        name: "Jaundice",
        baseRisk: "moderate",
        symptoms: { itching: 0.95, vomiting: 0.95, fatigue: 0.95, weight_loss: 0.95, fever: 0.95, yellowish_skin: 0.95, dark_urine: 0.95, abdominal_pain: 0.95 },
    },
    {
        id: "osteoarthritis",
        name: "Osteoarthritis",
        baseRisk: "moderate",
        symptoms: { joint_pain: 0.95, neck_pain: 0.95, knee_pain: 0.95, hip_joint_pain: 0.95, swelling_joints: 0.95, painful_walking: 0.95 },
    },
    {
        id: "paralysis",
        name: "Paralysis (Suspected Stroke)",
        baseRisk: "high",
        redFlag: true,
        symptoms: { altered_sensorium: 0.95, vomiting: 0.9, headache: 0.9, weakness_of_one_body_side: 0.9 },
    },
    {
        id: "peptic_ulcer",
        name: "Peptic Ulcer Disease",
        baseRisk: "moderate",
        symptoms: { vomiting: 0.95, abdominal_pain: 0.95, passage_of_gases: 0.95, internal_itching: 0.95, indigestion: 0.9, loss_appetite: 0.9 },
    },
    {
        id: "psoriasis",
        name: "Psoriasis",
        baseRisk: "low",
        symptoms: { rash: 0.95, joint_pain: 0.95, skin_peeling: 0.95, silver_like_dusting: 0.95, small_dents_in_nails: 0.95, inflammatory_nails: 0.95 },
    },
    {
        id: "tuberculosis",
        name: "Tuberculosis",
        baseRisk: "high",
        redFlag: true,
        symptoms: {
            loss_appetite: 1.0, fever: 1.0, yellowing_of_eyes: 1.0, swelling: 1.0, malaise: 1.0,
            phlegm: 1.0, chest_pain: 1.0, blood_in_sputum: 1.0, chills: 0.95, vomiting: 0.95,
            fatigue: 0.95, weight_loss: 0.95, cough: 0.95, breathing_difficulty: 0.95, sweating: 0.95,
        },
    },
    {
        id: "urinary_tract_infection",
        name: "Urinary Tract Infection",
        baseRisk: "moderate",
        symptoms: { bladder_discomfort: 0.95, continuous_feel_of_urine: 0.95, burning_micturition: 0.9, foul_smell_of_urine: 0.85 },
    },
    {
        id: "varicose_veins",
        name: "Varicose veins",
        baseRisk: "low",
        symptoms: { fatigue: 0.95, cramps: 0.95, bruising: 0.95, obesity: 0.95, swelling: 0.95, prominent_veins_on_calf: 0.95, swollen_blood_vessels: 0.9 },
    },
    {
        id: "hepatitis_a",
        name: "Hepatitis A",
        baseRisk: "moderate",
        symptoms: {
            fever: 1.0, yellowing_of_eyes: 1.0, muscle_pain: 1.0, joint_pain: 0.95,
            vomiting: 0.95, yellowish_skin: 0.95, dark_urine: 0.95, nausea: 0.95,
            loss_appetite: 0.95, abdominal_pain: 0.95, diarrhea: 0.95,
        },
    },
];
export const CONDITION_BY_ID = new Map(CONDITIONS.map((c) => [c.id, c]));
/** Red-flag symptoms that escalate risk regardless of match score. */
export const RED_FLAG_SYMPTOMS = new Set([
    "breathing_difficulty",
    "chest_pain",
    "seizure",
    "unconsciousness",
    "bleeding",
    "bloody_stool",
    "high_fever",
]);
/** Duration multipliers — prolonged symptoms raise evidence and risk. */
export function durationMultiplier(duration) {
    switch (duration) {
        case "More than 7 days":
            return 1.4;
        case "3 – 7 days":
            return 1.2;
        case "1 – 3 days":
            return 1.05;
        default:
            return 1.0;
    }
}
export function durationRiskEscalation(duration) {
    return duration === "More than 7 days";
}
