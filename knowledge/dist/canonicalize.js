/**
 * Normalise a reported symptom name into canonical evidence ids.
 * Faithful port of ClinicalReasoningEngine.canonicalizeSymptom().
 * Handles UI checklist entries, free-text "other" input and voice transcripts.
 */
const ALIASES = {
    high_fever: "fever",
    mild_fever: "fever",
    distention_of_abdomen: "distention_of_abdomen",
    swelling_joints: "swelling_joints",
    watering_from_eyes: "watering_from_eyes",
    swollen_extremeties: "swelling",
    swollen_legs: "swelling",
};
/**
 * Keyword -> canonical symptom id map. Keep in the same order as the Kotlin
 * source so first-hit precedence matches exactly.
 */
const KEYWORD_MAP = [
    ["breathing_difficulty", ["breathing", "breath", "short of breath", "breathe", "respiratory distress"]],
    ["chest_pain", ["chest pain", "chest", "tight chest"]],
    ["high_fever", ["very high fever", "high fever", "severe fever"]],
    ["fever", ["fever", "temperature", "pyrexia"]],
    ["cough", ["cough", "coughing"]],
    ["sore_throat", ["sore throat", "throat pain", "throat"]],
    ["runny_nose", ["runny nose", "cold", "nasal", "sneezing"]],
    ["body_aches", ["body ache", "muscle ache", "myalgia", "pain in body"]],
    ["fatigue", ["fatigue", "tired", "weakness", "exhaust", "lethargy"]],
    ["headache", ["headache", "head ache", "head pain"]],
    ["nausea", ["nausea", "queasy", "sick to stomach"]],
    ["vomiting", ["vomit", "throwing up"]],
    ["diarrhea", ["diarrhea", "diarrhoea", "loose stool", "loose motion", "loose stools"]],
    ["abdominal_pain", ["abdominal", "stomach pain", "stomach ache", "belly", "cramp"]],
    ["rash", ["rash", "skin lesion", "hives", "bumps on skin"]],
    ["dizziness", ["dizz", "lightheaded", "faint", "giddiness"]],
    ["chills", ["chill", "shiver"]],
    ["loss_appetite", ["loss of appetite", "not eating", "no appetite"]],
    ["wheezing", ["wheeze"]],
    ["itching", ["itch"]],
    ["bleeding", ["bleed", "bleeding"]],
    ["seizure", ["seizure", "fits", "convulsion"]],
    ["unconsciousness", ["unconscious", "passed out", "blackout"]],
    ["bloody_stool", ["blood in stool", "bloody stool", "blood in motion"]],
    ["swelling", ["swell", "swelling"]],
    ["mouth_sores", ["mouth sore", "mouth ulcer", "blister in mouth"]],
    ["sweating", ["sweat", "sweating"]],
    ["abnormal_menstruation", ["abnormal menstruation", "abnormal periods", "irregular periods", "irregular menstruation"]],
    ["acidity", ["acidity", "heartburn", "acid reflux", "burning in chest"]],
    ["acute_liver_failure", ["acute liver failure", "liver failure"]],
    ["altered_sensorium", ["altered sensorium", "confusion", "confused", "mental confusion"]],
    ["anxiety", ["anxiety", "anxious", "nervousness", "feeling nervous"]],
    ["back_pain", ["back pain", "pain in the back"]],
    ["blackheads", ["blackheads", "black heads"]],
    ["bladder_discomfort", ["bladder discomfort", "bladder pain"]],
    ["blister", ["blister", "blisters", "blistering"]],
    ["blood_in_sputum", ["blood in sputum", "blood in phlegm", "coughing blood"]],
    ["blurred_and_distorted_vision", ["blurred vision", "blurry vision", "distorted vision"]],
    ["brittle_nails", ["brittle nails", "breaking nails"]],
    ["bruising", ["bruising", "bruises"]],
    ["burning_micturition", ["burning micturition", "burning urination", "pain while urinating", "burning when urinating"]],
    ["cold_hands_and_feet", ["cold hands", "cold feet", "cold hands and feet"]],
    ["coma", ["coma", "comatose", "unconscious"]],
    ["congestion", ["congestion", "stuffy nose", "blocked nose"]],
    ["constipation", ["constipation", "constipated", "hard stool", "can't pass stool"]],
    ["continuous_feel_of_urine", ["continuous feel of urine", "constant urge to urinate", "always feel like urinating"]],
    ["continuous_sneezing", ["continuous sneezing", "sneezing a lot", "constant sneezing"]],
    ["cramps", ["cramps", "cramping", "stomach cramp", "abdominal cramp"]],
    ["dark_urine", ["dark urine", "dark coloured urine"]],
    ["dehydration_sym", ["dehydration", "dehydrated", "feeling dehydrated"]],
    ["depression", ["depression", "depressed", "low mood", "feeling sad"]],
    ["dischromic_patches", ["dischromic patches", "discoloured patches", "discolored patches", "patches of skin"]],
    ["distention_of_abdomen", ["abdominal distension", "distended abdomen", "bloated stomach"]],
    ["drying_and_tingling_lips", ["drying and tingling lips", "dry lips", "tingling lips"]],
    ["enlarged_thyroid", ["enlarged thyroid", "goitre", "goiter", "thyroid swelling"]],
    ["excessive_hunger", ["excessive hunger", "always hungry", "constant hunger"]],
    ["extra_marital_contacts", ["extra marital contacts", "unprotected sex", "unprotected contact"]],
    ["family_history", ["family history"]],
    ["fast_heart_rate", ["fast heart rate", "rapid heartbeat", "fast heartbeat"]],
    ["fluid_overload", ["fluid overload", "fluid retention", "retaining fluid"]],
    ["foul_smell_of_urine", ["foul smell of urine", "bad smelling urine", "foul smelling urine"]],
    ["hip_joint_pain", ["hip joint pain", "hip pain", "pain in the hip"]],
    ["history_of_alcohol_consumption", ["history of alcohol", "drinks alcohol", "alcohol consumption", "heavy drinking"]],
    ["increased_appetite", ["increased appetite", "bigger appetite", "eating more"]],
    ["indigestion", ["indigestion", "dyspepsia", "poor digestion"]],
    ["inflammatory_nails", ["inflammatory nails", "inflamed nails", "nail inflammation"]],
    ["internal_itching", ["internal itching", "itching inside"]],
    ["irregular_sugar_level", ["irregular sugar level", "irregular blood sugar", "unstable sugar"]],
    ["irritability", ["irritability", "irritable", "easily annoyed"]],
    ["irritation_in_anus", ["irritation in anus", "anal itching", "itching in anus"]],
    ["joint_pain", ["joint pain", "painful joints", "aching joints"]],
    ["knee_pain", ["knee pain", "pain in the knee"]],
    ["lack_of_concentration", ["lack of concentration", "poor concentration", "can't focus"]],
    ["lethargy", ["lethargy", "lethargic", "no energy"]],
    ["loss_of_balance", ["loss of balance", "losing balance"]],
    ["loss_of_smell", ["loss of smell", "can't smell"]],
    ["malaise", ["malaise", "feeling unwell", "feeling out of sorts"]],
    ["mood_swings", ["mood swings", "mood changes"]],
    ["movement_stiffness", ["movement stiffness", "stiff movement", "joint stiffness"]],
    ["mucoid_sputum", ["mucoid sputum", "mucous sputum", "phlegmy cough"]],
    ["muscle_pain", ["muscle pain", "aching muscles"]],
    ["muscle_wasting", ["muscle wasting", "muscle loss"]],
    ["muscle_weakness", ["muscle weakness", "weak muscles"]],
    ["neck_pain", ["neck pain", "pain in the neck"]],
    ["nodal_skin_eruptions", ["nodal skin eruptions", "nodular eruption", "bumpy rash"]],
    ["obesity", ["obesity", "obese", "overweight"]],
    ["pain_behind_the_eyes", ["pain behind the eyes", "pain behind eyes"]],
    ["pain_during_bowel_movements", ["pain during bowel movement", "pain while passing stool", "painful stools"]],
    ["pain_in_anal_region", ["pain in anal region", "anal pain", "pain in the anus"]],
    ["painful_walking", ["painful walking", "pain when walking", "difficulty walking"]],
    ["palpitations", ["palpitations", "racing heart", "heart pounding"]],
    ["passage_of_gases", ["passage of gases", "excess gas", "passing gas", "flatulence"]],
    ["patches_in_throat", ["patches in throat", "white patches in throat", "throat patches"]],
    ["phlegm", ["phlegm", "mucus"]],
    ["polyuria", ["polyuria", "excessive urination", "urinating a lot", "frequent urination"]],
    ["prominent_veins_on_calf", ["prominent veins on calf", "visible veins on legs", "prominent veins in legs"]],
    ["puffy_face_and_eyes", ["puffy face", "puffy eyes", "swollen face"]],
    ["pus_filled_pimples", ["pus filled pimples", "pus filled bumps", "pimples with pus"]],
    ["receiving_blood_transfusion", ["blood transfusion", "received blood"]],
    ["receiving_unsterile_injections", ["unsterile injection", "unclean injection", "shared needles"]],
    ["red_sore_around_nose", ["red sore around nose", "sores around nose", "sore around nose"]],
    ["red_spots_over_body", ["red spots over body", "red spots on body", "red spots all over"]],
    ["redness_of_eyes", ["red eyes", "redness of eyes", "bloodshot eyes"]],
    ["restlessness", ["restlessness", "restless"]],
    ["rusty_sputum", ["rusty sputum", "rust coloured sputum"]],
    ["scurring", ["scurring", "acne scars", "scarring"]],
    ["shivering", ["shivering", "shivering with cold"]],
    ["silver_like_dusting", ["silver like dusting", "silvery scales", "silver dusting on skin"]],
    ["sinus_pressure", ["sinus pressure", "sinus pain"]],
    ["skin_peeling", ["skin peeling", "peeling skin"]],
    ["slurred_speech", ["slurred speech", "slurring speech", "slurred words"]],
    ["small_dents_in_nails", ["small dents in nails", "nail pitting", "dents in nails"]],
    ["spinning_movements", ["spinning sensation", "room spinning", "spinning movements"]],
    ["spotting_urination", ["spotting urination", "spots in urine"]],
    ["stiff_neck", ["stiff neck", "stiffness in the neck"]],
    ["stomach_bleeding", ["stomach bleeding", "bleeding in stomach"]],
    ["sunken_eyes", ["sunken eyes", "hollow eyes"]],
    ["swelling_joints", ["swollen joints", "joint swelling"]],
    ["swelling_of_stomach", ["swelling of stomach", "swollen stomach", "belly swelling"]],
    ["swollen_blood_vessels", ["swollen blood vessels", "swollen veins"]],
    ["throat_irritation", ["throat irritation", "scratchy throat", "irritated throat"]],
    ["toxic_look", ["toxic look", "toxic appearance"]],
    ["ulcers_on_tongue", ["ulcers on tongue", "tongue ulcer", "sores on tongue"]],
    ["unsteadiness", ["unsteadiness", "unsteady"]],
    ["visual_disturbances", ["visual disturbance", "vision problems", "seeing spots"]],
    ["watering_from_eyes", ["watery eyes", "watering eyes", "teary eyes"]],
    ["weakness_in_limbs", ["weakness in limbs", "weak limbs"]],
    ["weakness_of_one_body_side", ["weakness on one side", "one sided weakness", "weakness of one side", "weakness of one body side"]],
    ["weight_gain", ["weight gain", "gaining weight", "putting on weight"]],
    ["weight_loss", ["weight loss", "losing weight", "unexplained weight loss"]],
    ["yellow_crust_ooze", ["yellow crust ooze", "yellow crust", "oozing crust"]],
    ["yellow_urine", ["yellow urine"]],
    ["yellowing_of_eyes", ["yellow eyes", "yellowing of eyes", "yellowish eyes"]],
    ["yellowish_skin", ["yellowish skin", "yellow skin", "skin turning yellow"]],
];
export function canonicalizeSymptom(raw) {
    const original = raw.toLowerCase().trim();
    if (!original)
        return [];
    if (Object.prototype.hasOwnProperty.call(ALIASES, original)) {
        const substitute = ALIASES[original];
        return substitute ? [substitute] : [];
    }
    const s = original.replace(/_/g, " ").replace(/\s+/g, " ");
    if (!s)
        return [];
    const hits = [];
    for (const [id, keywords] of KEYWORD_MAP) {
        if (keywords.some((k) => s.includes(k)))
            hits.push(id);
    }
    // UI checklist exact names.
    if (s.includes("body aches"))
        hits.push("body_aches");
    if (s.includes("fatigue") || s.includes("weakness"))
        hits.push("fatigue");
    if (s.includes("skin rash") || s === "rash")
        hits.push("rash");
    if (s.includes("difficulty breathing"))
        hits.push("breathing_difficulty");
    if (s.includes("sore throat"))
        hits.push("sore_throat");
    if (s.includes("nausea") || s.includes("vomiting")) {
        if (s.includes("nausea"))
            hits.push("nausea");
        if (s.includes("vomiting"))
            hits.push("vomiting");
    }
    return [...new Set(hits)];
}
