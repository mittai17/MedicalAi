package com.swasthai.app.ai.engine

import com.swasthai.app.domain.model.MedicalAdvice

/**
 * AI Doctor knowledge base.
 *
 * Maps every condition the screening models can predict (image findings,
 * tissue/cell types, symptom-based diagnoses) to the three answers the
 * doctor must give: WHY it happens (cause), WHAT to do (remedy) and
 * WHO to see (doctor to consult).
 *
 * This is screening-suggestion content, not a diagnosis. Every entry is
 * framed to guide a patient toward proper care rather than to replace a
 * clinician.
 */
object DiseaseKnowledgeBase {

    private val entries: Map<String, MedicalAdvice> = buildMap {
        // ── Respiratory / Chest ──
        put("pneumonia", MedicalAdvice(
            condition = "Pneumonia",
            cause = "An infection of the air sacs in the lungs, usually bacterial or viral. Bacteria like Streptococcus pneumoniae and respiratory viruses are the most common triggers; risk rises with age, smoking and chronic lung disease.",
            remedy = "Complete the full course of prescribed antibiotics (if bacterial), rest, drink warm fluids, and take fever medication as advised. Do not self-medicate — a chest X-ray and clinical exam are needed to confirm and to rule out complications.",
            doctorToConsult = "General Physician or Pulmonologist",
            urgencyHint = "Seek urgent care if you have trouble breathing, chest pain, or bluish lips."
        ))
        put("atelectasis", MedicalAdvice(
            condition = "Atelectasis",
            cause = "Collapse of part of a lung, often after surgery, from a mucus plug, or due to a blocked airway. Shallow breathing after anaesthesia is a common cause.",
            remedy = "Deep-breathing exercises, incentive spirometry, and early mobility help re-expand the lung. Treat the underlying cause — mucus clearing or a blocked airway must be addressed by a clinician.",
            doctorToConsult = "Pulmonologist or General Physician",
            urgencyHint = "Sudden shortness of breath or chest pain needs urgent evaluation."
        ))
        put("cardiomegaly", MedicalAdvice(
            condition = "Cardiomegaly",
            cause = "An enlarged heart, which is a sign rather than a disease itself. Common causes are high blood pressure, heart valve disease, heart failure, or cardiomyopathy.",
            remedy = "Control blood pressure, follow a low-salt diet, and avoid smoking and alcohol. An ECG, echocardiogram and blood tests are needed to find the cause — do not ignore it.",
            doctorToConsult = "Cardiologist",
            urgencyHint = "Chest pain, breathlessness or leg swelling warrant urgent cardiac review."
        ))
        put("effusion", MedicalAdvice(
            condition = "Pleural Effusion",
            cause = "Fluid collecting between the lung and chest wall. It can result from infection (pneumonia/TB), heart failure, cancer, or kidney/liver disease.",
            remedy = "The cause must be investigated — a chest X-ray, ultrasound and sometimes fluid sampling. Treatment targets the underlying cause; large effusions may need drainage.",
            doctorToConsult = "Pulmonologist",
            urgencyHint = "Breathlessness that is getting worse needs urgent evaluation."
        ))
        put("infiltration", MedicalAdvice(
            condition = "Pulmonary Infiltration",
            cause = "Fluid, cells or inflammation seen on an X-ray within the lung tissue, most often from infection (pneumonia, including viral), but also possible with TB, fluid overload or interstitial disease.",
            remedy = "Usually investigated with blood tests and follow-up imaging. If infection is suspected, antibiotics/antivirals may be prescribed; hydration and rest help recovery.",
            doctorToConsult = "Pulmonologist or General Physician",
            urgencyHint = "Worsening breathlessness or high fever requires urgent care."
        ))
        put("mass", MedicalAdvice(
            condition = "Pulmonary Mass",
            cause = "A solid growth in the lung larger than 3 cm. It may be benign, but it needs evaluation because it can be malignant; smoking history raises concern.",
            remedy = "Do not ignore. A CT scan, biopsy and specialist review are required to determine whether the mass is benign or malignant.",
            doctorToConsult = "Pulmonologist or Onco-Pulmonologist",
            urgencyHint = "Coughing blood, weight loss or persistent chest pain need urgent attention."
        ))
        put("nodule", MedicalAdvice(
            condition = "Pulmonary Nodule",
            cause = "A small rounded spot in the lung (under 3 cm), often found incidentally. Most are benign (old infection scar, inflammation), but a few can be early cancer.",
            remedy = "Follow-up imaging is usually advised to check the nodule does not grow. Size, appearance and risk factors decide whether to repeat the scan or proceed to biopsy.",
            doctorToConsult = "Pulmonologist",
            urgencyHint = "Usually followed over time; new or growing nodules need review."
        ))
        put("pneumothorax", MedicalAdvice(
            condition = "Pneumothorax",
            cause = "Air leaking into the space between the lung and chest wall, causing the lung to collapse. It can follow injury, lung disease, or happen spontaneously.",
            remedy = "A collapsed lung can be a medical emergency. Small ones may resolve with rest and oxygen; larger ones need a tube to remove the air. Seek medical care immediately.",
            doctorToConsult = "Emergency Physician / Pulmonologist",
            urgencyHint = "Sudden sharp chest pain with breathlessness is an emergency — get help now."
        ))
        put("consolidation", MedicalAdvice(
            condition = "Lung Consolidation",
            cause = "Lung tissue filled with liquid instead of air, almost always from pneumonia (bacterial or viral). It shows as a white area on the X-ray.",
            remedy = "Antibiotics or antiviral treatment directed at the likely germ, plenty of fluids and rest. Recheck imaging if symptoms persist after treatment.",
            doctorToConsult = "General Physician or Pulmonologist",
            urgencyHint = "Difficulty breathing or high fever needs urgent care."
        ))
        put("edema", MedicalAdvice(
            condition = "Pulmonary Edema",
            cause = "Fluid building up in the lungs, most commonly from heart failure — the heart cannot pump well enough. Kidney disease and fluid overload can also cause it.",
            remedy = "This is potentially serious. Medical treatment relieves the fluid load (diuretics, heart medication) and treats the cause. Hospital evaluation is often needed.",
            doctorToConsult = "Cardiologist or Emergency Physician",
            urgencyHint = "Breathlessness while lying flat, or gasping for air — seek emergency care."
        ))
        put("emphysema", MedicalAdvice(
            condition = "Emphysema",
            cause = "Long-term damage to the air sacs in the lungs, overwhelmingly caused by smoking. It is a form of COPD and develops over many years.",
            remedy = "Stop smoking completely — the single most important step. Breathing exercises, inhalers and vaccines (flu/pneumonia) help slow progression and prevent flare-ups.",
            doctorToConsult = "Pulmonologist",
            urgencyHint = "Worsening breathlessness or a chest infection requires prompt review."
        ))
        put("fibrosis", MedicalAdvice(
            condition = "Pulmonary Fibrosis",
            cause = "Scarring of lung tissue that stiffens the lungs. Causes include long-term exposure to dusts (asbestosis, silicosis), autoimmune disease, or unknown (idiopathic) causes.",
            remedy = "Avoid the offending dust or fumes, and manage underlying illness. Treatment may include medication to slow scarring and oxygen therapy — specialist care is essential.",
            doctorToConsult = "Pulmonologist (Interstitial Lung Disease clinic)",
            urgencyHint = "Progressive breathlessness or dry cough needs specialist review."
        ))
        put("pleural", MedicalAdvice(
            condition = "Pleural Thickening",
            cause = "Scarring of the membrane around the lungs, often from past infection (especially TB), asbestos exposure, or previous pleural disease.",
            remedy = "Usually monitored with imaging; no treatment is needed if stable and symptom-free. Chest pain or breathlessness warrants pulmonology review.",
            doctorToConsult = "Pulmonologist",
            urgencyHint = "New or worsening breathlessness needs evaluation."
        ))
        put("hernia", MedicalAdvice(
            condition = "Diaphragmatic Hernia",
            cause = "A weakness or opening in the diaphragm allowing abdominal contents to move into the chest. It may be present from birth or develop after injury.",
            remedy = "Some hernias are watched; others need surgical repair if they cause symptoms or risk strangulation. Evaluation by a surgeon is required.",
            doctorToConsult = "General or Thoracic Surgeon",
            urgencyHint = "Severe pain, vomiting, or inability to pass gas needs emergency care."
        ))

        // ── Skin ──
        put("actinic keratosis", MedicalAdvice(
            condition = "Actinic Keratosis",
            cause = "Rough scaly patches caused by long-term sun exposure damaging the skin. It is a pre-cancerous growth, not yet cancer, but can progress over years.",
            remedy = "Protect skin from the sun and have it examined. Treatment options include freezing, creams, or laser — done by a dermatologist. Regular skin checks are advised.",
            doctorToConsult = "Dermatologist",
            urgencyHint = "A patch that bleeds, grows, or changes shape needs prompt review."
        ))
        put("basal cell carcinoma", MedicalAdvice(
            condition = "Basal Cell Carcinoma",
            cause = "The most common skin cancer, caused mainly by cumulative sun exposure. It grows slowly and rarely spreads, but can damage surrounding tissue if untreated.",
            remedy = "Do not delay — a dermatologist confirms it with a biopsy and removes it surgically. Treated early, the outlook is excellent.",
            doctorToConsult = "Dermatologist / Skin Cancer Specialist",
            urgencyHint = "A sore that does not heal, bleeds, or grows needs evaluation soon."
        ))
        put("benign keratosis", MedicalAdvice(
            condition = "Benign Keratosis",
            cause = "A harmless non-cancerous skin growth, common with age and sun exposure. It is not malignant and requires no treatment.",
            remedy = "No treatment is needed. See a dermatologist if it becomes irritated, bleeds, or you want it removed for cosmetic reasons.",
            doctorToConsult = "Dermatologist (if concerned)",
            urgencyHint = "No urgency; monitor for change."
        ))
        put("dermatofibroma", MedicalAdvice(
            condition = "Dermatofibroma",
            cause = "A firm, harmless fibrous nodule in the skin, often appearing after a minor injury or insect bite. It is benign.",
            remedy = "Usually left alone. Consult a dermatologist if it grows, changes, or is painful.",
            doctorToConsult = "Dermatologist",
            urgencyHint = "No urgency."
        ))
        put("melanoma", MedicalAdvice(
            condition = "Melanoma",
            cause = "The most serious form of skin cancer, arising from pigment-producing cells. Risk factors include sun exposure, fair skin, and a history of sunburns or moles.",
            remedy = "This needs urgent specialist evaluation — a biopsy is essential to confirm. Surgical removal in the early stage can be curative, so do not delay.",
            doctorToConsult = "Dermatologist / Onco-Dermatologist",
            urgencyHint = "A mole with uneven colour, irregular edges, growing or bleeding — urgent review."
        ))
        put("melanocytic nevus", MedicalAdvice(
            condition = "Melanocytic Nevus (Mole)",
            cause = "A common benign collection of pigment cells (a mole). Most are harmless, though some can change over time.",
            remedy = "No treatment needed. Watch for the ABCDE signs (Asymmetry, Border, Colour, Diameter, Evolving); any change warrants a skin check.",
            doctorToConsult = "Dermatologist (if changes)",
            urgencyHint = "No urgency; monitor for change."
        ))
        put("vascular lesion", MedicalAdvice(
            condition = "Vascular Lesion",
            cause = "A growth of blood vessels in the skin (e.g., angioma, birthmark). Most are harmless, though a few types need evaluation.",
            remedy = "Usually harmless and left alone. Dermatology review is helpful if it bleeds, grows rapidly, or is cosmetically concerning.",
            doctorToConsult = "Dermatologist",
            urgencyHint = "Rapid growth or bleeding needs review."
        ))

        // ── Eye ──
        put("diabetic retinopathy", MedicalAdvice(
            condition = "Diabetic Retinopathy",
            cause = "Damage to the light-sensitive retina caused by long-term high blood sugar. High sugar weakens tiny blood vessels in the eye, which can leak or bleed.",
            remedy = "Control blood sugar, blood pressure and cholesterol. Early diabetic retinopathy can be managed; advanced cases may need laser or injections. Regular eye exams are vital.",
            doctorToConsult = "Ophthalmologist (Retina Specialist)",
            urgencyHint = "Sudden vision loss, floaters, or dark areas need urgent eye care."
        ))
        put("cnv", MedicalAdvice(
            condition = "Choroidal Neovascularization (CNV)",
            cause = "Abnormal blood vessels growing under the retina, often in wet age-related macular degeneration. The vessels leak and can damage central vision quickly.",
            remedy = "Prompt treatment is important — usually anti-VEGF injections into the eye. Early treatment protects remaining vision, so act fast.",
            doctorToConsult = "Ophthalmologist (Retina Specialist)",
            urgencyHint = "Sudden wavy or blurred central vision — urgent review."
        ))
        put("dme", MedicalAdvice(
            condition = "Diabetic Macular Edema (DME)",
            cause = "Swelling of the central retina (macula) from fluid leakage, a complication of diabetic retinopathy. It blurs central vision.",
            remedy = "Treatments include anti-VEGF injections or laser, combined with strict blood sugar control. Regular retina follow-up is essential.",
            doctorToConsult = "Ophthalmologist (Retina Specialist)",
            urgencyHint = "Worsening central blur needs prompt review."
        ))
        put("drusen", MedicalAdvice(
            condition = "Drusen",
            cause = "Yellow deposits under the retina, an early sign of age-related macular degeneration. Many people with drusen keep good vision for years.",
            remedy = "Regular retina checks, eye-healthy diet, and not smoking. Some cases benefit from supplements — your ophthalmologist will advise.",
            doctorToConsult = "Ophthalmologist",
            urgencyHint = "Any new vision change needs review."
        ))

        // ── Blood ──
        put("basophil", MedicalAdvice(
            condition = "Basophilia (Raised Basophils)",
            cause = "Basophils are white blood cells that rise in allergic reactions, chronic inflammation, or occasionally blood disorders.",
            remedy = "A doctor interprets the full blood count — a single high value usually needs a repeat test and clinical correlation before any conclusion.",
            doctorToConsult = "General Physician / Hematologist",
            urgencyHint = "Usually not urgent."
        ))
        put("eosinophil", MedicalAdvice(
            condition = "Eosinophilia (Raised Eosinophils)",
            cause = "High eosinophils often reflect allergy, asthma, parasitic infection, or drug reactions.",
            remedy = "Evaluate possible allergens or infections with your doctor; treat the underlying cause.",
            doctorToConsult = "General Physician / Allergist",
            urgencyHint = "Not urgent unless symptoms are severe."
        ))
        put("erythroblast", MedicalAdvice(
            condition = "Erythroblast (Nucleated Red Cell)",
            cause = "Immature red blood cells in the blood, sometimes seen when the bone marrow is stressed by anaemia, infection, or blood disorders.",
            remedy = "Requires medical interpretation with the full blood count; further tests may be needed.",
            doctorToConsult = "Hematologist / General Physician",
            urgencyHint = "Needs review, usually non-urgent."
        ))
        put("immature granulocyte", MedicalAdvice(
            condition = "Immature Granulocytes",
            cause = "Young white blood cells that can appear in infection, inflammation, or bone marrow disorders.",
            remedy = "Interpreted with the full blood count and clinical picture; repeat testing is often advised.",
            doctorToConsult = "General Physician / Hematologist",
            urgencyHint = "Depends on the clinical picture."
        ))
        put("lymphocyte", MedicalAdvice(
            condition = "Lymphocytosis (Raised Lymphocytes)",
            cause = "High lymphocytes commonly follow viral infections; they can also reflect other conditions.",
            remedy = "Usually resolves with the infection; a doctor reviews persistent or very high counts.",
            doctorToConsult = "General Physician / Hematologist",
            urgencyHint = "Usually not urgent."
        ))
        put("monocyte", MedicalAdvice(
            condition = "Monocytosis (Raised Monocytes)",
            cause = "Raised monocytes can follow infection, chronic inflammation, or recovery from illness.",
            remedy = "Review with full blood count; repeat testing is often the first step.",
            doctorToConsult = "General Physician",
            urgencyHint = "Usually not urgent."
        ))
        put("neutrophil", MedicalAdvice(
            condition = "Neutrophilia (Raised Neutrophils)",
            cause = "High neutrophils usually mean bacterial infection or inflammation.",
            remedy = "Treat the underlying infection/inflammation; antibiotics may be needed if bacterial.",
            doctorToConsult = "General Physician",
            urgencyHint = "With fever and severe symptoms, seek care promptly."
        ))
        put("platelet", MedicalAdvice(
            condition = "Thrombocytosis (Raised Platelets)",
            cause = "Raised platelets can follow inflammation, iron deficiency, bleeding, or infection; rarely a marrow disorder.",
            remedy = "Interpreted with the full blood count and clinical history; follow-up testing is common.",
            doctorToConsult = "General Physician / Hematologist",
            urgencyHint = "Usually not urgent."
        ))

        // ── Colon / Pathology ──
        put("adenocarcinoma", MedicalAdvice(
            condition = "Adenocarcinoma (Colon)",
            cause = "A cancer arising from glandular cells, most often of the colon. Risk rises with age, family history, diet low in fibre, and inflammatory bowel disease.",
            remedy = "Specialist evaluation with biopsy, staging (CT/colonoscopy) and a treatment plan — surgery, sometimes with chemotherapy. Early detection markedly improves outcome.",
            doctorToConsult = "Gastroenterologist / Surgical Oncologist",
            urgencyHint = "Blood in stool, persistent change in bowel habit, or weight loss — prompt review."
        ))
        put("ca stroma", MedicalAdvice(
            condition = "Carcinoma-associated Stroma",
            cause = "Tissue changes seen around a cancer, indicating a tumour environment. It points to a cancer diagnosis requiring full work-up.",
            remedy = "Specialist oncology evaluation, staging, and a multidisciplinary treatment plan.",
            doctorToConsult = "Oncologist / Pathologist-reviewed clinic",
            urgencyHint = "Needs prompt specialist review."
        ))
        put("normal mucosa", MedicalAdvice(
            condition = "Normal Mucosa",
            cause = "Healthy lining of the colon/bowel — no disease detected in this sample.",
            remedy = "No treatment needed. Maintain routine screening (e.g., colonoscopy) as advised for your age and risk.",
            doctorToConsult = "No specialist needed",
            urgencyHint = "None."
        ))
        put("adipose", MedicalAdvice(
            condition = "Adipose Tissue",
            cause = "Fat tissue — a normal finding in biopsy samples, not a disease.",
            remedy = "No treatment needed; a clinical correlation confirms.",
            doctorToConsult = "No specialist needed",
            urgencyHint = "None."
        ))
        put("mucus", MedicalAdvice(
            condition = "Mucus",
            cause = "Normal mucus-secreting tissue — a benign finding.",
            remedy = "No treatment needed.",
            doctorToConsult = "No specialist needed",
            urgencyHint = "None."
        ))
        put("smooth muscle", MedicalAdvice(
            condition = "Smooth Muscle",
            cause = "Normal muscle layer of the bowel wall — a benign finding.",
            remedy = "No treatment needed.",
            doctorToConsult = "No specialist needed",
            urgencyHint = "None."
        ))

        // ── Kidney tissue ──
        put("collecting duct", MedicalAdvice(
            condition = "Collecting Duct / Connecting Tubule",
            cause = "A normal cell type of the kidney's collecting system, which concentrates urine.",
            remedy = "No treatment needed — this identifies a normal cell type, not a disease.",
            doctorToConsult = "No specialist needed",
            urgencyHint = "None."
        ))
        put("distal convoluted tubule", MedicalAdvice(
            condition = "Distal Convoluted Tubule",
            cause = "A normal kidney cell type involved in salt and water balance.",
            remedy = "No treatment needed — a normal cell type.",
            doctorToConsult = "No specialist needed",
            urgencyHint = "None."
        ))
        put("glomerular endothelial", MedicalAdvice(
            condition = "Glomerular Endothelial Cells",
            cause = "Cells lining the kidney's filtering units (glomeruli) — normal finding.",
            remedy = "No treatment needed.",
            doctorToConsult = "No specialist needed",
            urgencyHint = "None."
        ))
        put("interstitial endothelial", MedicalAdvice(
            condition = "Interstitial Endothelial Cells",
            cause = "Cells lining blood vessels in the kidney tissue — normal finding.",
            remedy = "No treatment needed.",
            doctorToConsult = "No specialist needed",
            urgencyHint = "None."
        ))
        put("podocytes", MedicalAdvice(
            condition = "Podocytes",
            cause = "Specialised cells of the kidney filter — normal finding.",
            remedy = "No treatment needed.",
            doctorToConsult = "No specialist needed",
            urgencyHint = "None."
        ))
        put("proximal tubule", MedicalAdvice(
            condition = "Proximal Tubule",
            cause = "A normal kidney cell type that reabsorbs most filtered nutrients and water.",
            remedy = "No treatment needed — a normal cell type.",
            doctorToConsult = "No specialist needed",
            urgencyHint = "None."
        ))
        put("thick ascending limb", MedicalAdvice(
            condition = "Thick Ascending Limb",
            cause = "A normal segment of the kidney tubule — normal finding.",
            remedy = "No treatment needed.",
            doctorToConsult = "No specialist needed",
            urgencyHint = "None."
        ))
        put("leukocytes", MedicalAdvice(
            condition = "Leukocytes in Kidney Tissue",
            cause = "White blood cells in kidney tissue, which can indicate inflammation or infection of the kidney.",
            remedy = "Review with a nephrologist; urine and blood tests help determine if it is infection, interstitial nephritis, or a normal finding.",
            doctorToConsult = "Nephrologist",
            urgencyHint = "Fever, flank pain or burning urination needs prompt review."
        ))

        // ── Breast ──
        put("malignant", MedicalAdvice(
            condition = "Malignant Finding (Breast)",
            cause = "The ultrasound suggests a growth that may be cancer. Most breast lumps are benign, but a suspicious one must be biopsied to know for sure.",
            remedy = "Do not panic and do not delay. A biopsy (FNAC/core) gives a definitive answer; if positive, early treatment offers the best outcome.",
            doctorToConsult = "Breast Surgeon / Surgical Oncologist",
            urgencyHint = "Book evaluation promptly; a breast lump needs confirmation."
        ))
        put("benign", MedicalAdvice(
            condition = "Benign / Normal (Breast)",
            cause = "No suspicious features seen — most likely a benign finding or normal tissue.",
            remedy = "No treatment needed. Continue routine breast self-exam and age-appropriate screening.",
            doctorToConsult = "No specialist needed",
            urgencyHint = "None."
        ))
        put("normal", MedicalAdvice(
            condition = "No abnormality detected",
            cause = "The screening found no features suggesting the condition it was looking for.",
            remedy = "No treatment needed. If symptoms continue or worsen, still consult a doctor — a normal screen does not rule out every condition.",
            doctorToConsult = "No specialist needed",
            urgencyHint = "None."
        ))
        put("no dr", MedicalAdvice(
            condition = "No Diabetic Retinopathy detected",
            cause = "No signs of retinal damage from diabetes were found in this image.",
            remedy = "Continue regular eye checks (at least yearly) and keep blood sugar under control — retinopathy can develop silently.",
            doctorToConsult = "Ophthalmologist (routine annual check)",
            urgencyHint = "None."
        ))
        put("background", MedicalAdvice(
            condition = "Background / Non-diagnostic tissue",
            cause = "The sample is mostly background tissue with no diagnostic features.",
            remedy = "No treatment implied; a clinician correlates the sample with the clinical picture.",
            doctorToConsult = "Pathologist / Referring Clinician",
            urgencyHint = "None."
        ))
        put("debris", MedicalAdvice(
            condition = "Debris (non-diagnostic)",
            cause = "The sample contains cellular debris without a diagnostic pattern.",
            remedy = "May need a repeat sample if the clinical concern persists.",
            doctorToConsult = "Pathologist / Referring Clinician",
            urgencyHint = "None."
        ))

        // ── Symptom-based diagnoses (text path) ──
        put("respiratory infection", MedicalAdvice(
            condition = "Respiratory Infection",
            cause = "An infection of the airways by viruses or bacteria — common causes include cold viruses, flu, and chest infections.",
            remedy = "Rest, fluids and fever medication as advised. If a bacterial infection is suspected, a doctor may prescribe antibiotics. Avoid smoking and check for worsening symptoms.",
            doctorToConsult = "General Physician",
            urgencyHint = "Difficulty breathing, high persistent fever, or chest pain — seek care."
        ))
        put("influenza", MedicalAdvice(
            condition = "Influenza (Flu)",
            cause = "A contagious viral infection of the nose, throat and lungs, caused by influenza viruses. Spreads through droplets from coughs and sneezes.",
            remedy = "Rest, hydrate and treat fever as advised. Flu is viral — antibiotics do not help. High-risk people (elderly, pregnant, chronic illness) should see a doctor early, especially within 48 hours of symptoms.",
            doctorToConsult = "General Physician",
            urgencyHint = "Breathing difficulty, chest pain, confusion, or dehydration — urgent care."
        ))
        put("covid19", MedicalAdvice(
            condition = "COVID-19 / Viral Pneumonia",
            cause = "An infection caused by the SARS-CoV-2 virus. It affects the airways and can inflame the lungs, causing cough, fever and breathlessness.",
            remedy = "Isolate to avoid spreading, rest, hydrate and monitor oxygen. Seek medical review for breathlessness or if you are in a high-risk group; only a doctor should decide on specific treatment.",
            doctorToConsult = "General Physician / Pulmonologist",
            urgencyHint = "Breathlessness, chest pain, confusion, or oxygen below 94% — emergency."
        ))
        put("food poisoning", MedicalAdvice(
            condition = "Food Poisoning",
            cause = "Illness from eating contaminated food or water — bacteria, toxins or viruses. Onset is usually within hours of the meal.",
            remedy = "Rehydrate with ORS and rest. Most cases resolve in a day or two. Seek care if vomiting or diarrhoea is severe, or if there is blood, high fever or dehydration.",
            doctorToConsult = "General Physician",
            urgencyHint = "Severe dehydration, blood in stool, or inability to keep fluids down — urgent."
        ))
        put("urticaria", MedicalAdvice(
            condition = "Allergic Reaction (Urticaria)",
            cause = "An allergic response releasing histamine, causing itchy hives. Triggers include foods, medicines, insect stings or latex.",
            remedy = "Stop the trigger, take an antihistamine as advised, and cool the skin. Severe or spreading reactions, especially with breathing difficulty, need emergency care.",
            doctorToConsult = "General Physician / Allergist",
            urgencyHint = "Breathing difficulty, lip/tongue swelling, dizziness — emergency."
        ))
        put("migraine", MedicalAdvice(
            condition = "Migraine",
            cause = "A neurological condition causing throbbing headaches, often with nausea and sensitivity to light or sound. Triggers include stress, skipped meals, sleep changes and certain foods.",
            remedy = "Rest in a dark, quiet room, hydrate, and use pain relief/medication as advised. Keeping a trigger diary helps. Recurrent or disabling migraines need a neurologist.",
            doctorToConsult = "Neurologist (if recurrent)",
            urgencyHint = "Sudden worst-ever headache or weakness — urgent."
        ))
        put("dehydration", MedicalAdvice(
            condition = "Dehydration",
            cause = "Loss of more fluid than the body takes in — from fever, vomiting, diarrhoea, heat or inadequate drinking. Children and the elderly are most at risk.",
            remedy = "Rehydrate with ORS or water in small frequent sips. Rest and avoid heat/exertion. Seek care if you cannot drink, are confused, or have reduced urine.",
            doctorToConsult = "General Physician",
            urgencyHint = "Confusion, fainting, no urine for many hours, or inability to drink — urgent."
        ))
        put("anemia", MedicalAdvice(
            condition = "Anemia / Iron Deficiency",
            cause = "Too few healthy red blood cells, most often from iron deficiency — from low iron in the diet, blood loss, or poor absorption. Causes fatigue, pallor and dizziness.",
            remedy = "Eat iron-rich foods (leafy greens, lentils, meat, fortified foods) with vitamin C to improve absorption. A blood count test confirms it; iron supplements only as prescribed.",
            doctorToConsult = "General Physician / Hematologist",
            urgencyHint = "Severe fatigue, breathlessness, chest pain, or fainting — review promptly."
        ))
        put("low blood sugar", MedicalAdvice(
            condition = "Low Blood Sugar",
            cause = "Blood glucose falling too low — common in diabetics who skip meals or over-dose medication, or after fasting hard. Causes dizziness, sweating, shaking and confusion.",
            remedy = "If conscious, take fast sugar (glucose, juice, sugar water) then a small meal. If on diabetes medication, check glucose and seek advice; persistent or severe episodes need medical care.",
            doctorToConsult = "General Physician / Endocrinologist",
            urgencyHint = "Confusion, unconsciousness, or seizure — emergency (glucagon/108)."
        ))
        put("asthma", MedicalAdvice(
            condition = "Asthma",
            cause = "Inflammation and narrowing of the airways causing wheeze, cough and breathlessness. Triggers include allergens, cold air, smoke and exercise.",
            remedy = "Use your prescribed inhaler as directed and keep the reliever with you. Avoid triggers. A personalised asthma action plan from a doctor helps control flare-ups.",
            doctorToConsult = "Pulmonologist",
            urgencyHint = "Severe breathlessness, unable to speak, or no relief from inhaler — emergency."
        ))
        put("dengue", MedicalAdvice(
            condition = "Dengue Fever",
            cause = "A viral infection spread by Aedes mosquitoes, common in tropical areas. Causes high fever, severe body aches, headache and sometimes rash.",
            remedy = "Rest, drink plenty of fluids and monitor temperature. Avoid aspirin/ibuprofen — they increase bleeding risk. A blood count test helps monitor platelet levels. Seek care if bleeding or severe abdominal pain develops.",
            doctorToConsult = "General Physician / Infectious Disease Specialist",
            urgencyHint = "Bleeding, severe abdominal pain, vomiting, or very low platelets — urgent."
        ))
        put("malaria", MedicalAdvice(
            condition = "Malaria",
            cause = "A parasitic infection transmitted by Anopheles mosquitoes. Causes fever, chills and body aches, and can become severe if untreated.",
            remedy = "Malaria is curable with proper anti-malarial treatment — a blood test (smear/RDT) confirms it. Start treatment early; never self-treat without a confirmed diagnosis.",
            doctorToConsult = "General Physician / Infectious Disease Specialist",
            urgencyHint = "High fever with confusion, jaundice, or difficulty breathing — urgent."
        ))
        put("typhoid", MedicalAdvice(
            condition = "Typhoid Fever",
            cause = "A bacterial infection (Salmonella typhi) from contaminated food or water. Causes sustained fever, weakness, headache and abdominal symptoms.",
            remedy = "Needs a doctor's diagnosis (blood/stool test) and a full course of antibiotics. Hydrate and rest; complete the entire antibiotic course even when you feel better.",
            doctorToConsult = "General Physician / Infectious Disease Specialist",
            urgencyHint = "Very high fever, confusion, or severe abdominal pain — urgent."
        ))
        put("high blood pressure", MedicalAdvice(
            condition = "High Blood Pressure",
            cause = "Persistently elevated pressure in the arteries — often from diet, stress, lack of exercise, smoking, or family history. Usually silent until it causes damage.",
            remedy = "Get your blood pressure measured properly (several readings). Reduce salt, exercise regularly, manage stress, limit alcohol, and take medication only as prescribed.",
            doctorToConsult = "General Physician / Cardiologist",
            urgencyHint = "Very high reading with chest pain, severe headache, or breathlessness — urgent."
        ))
        put("oral / mouth infection", MedicalAdvice(
            condition = "Oral / Mouth Infection",
            cause = "Inflammation or infection of the mouth lining — from viruses (e.g., herpes), fungal overgrowth (thrush), or ulcers. Poor oral hygiene and stress can contribute.",
            remedy = "Keep the mouth clean, rinse with warm salt water, and stay hydrated. Avoid very hot/spicy foods. Persistent or painful sores need a doctor's examination.",
            doctorToConsult = "Dentist / General Physician",
            urgencyHint = "Difficulty swallowing, spreading swelling, or high fever — review."
        ))
        put("hypoglycemia", MedicalAdvice(
            condition = "Low Blood Sugar",
            cause = "Blood glucose falling too low — common in diabetics who skip meals or over-dose medication, or after fasting hard. Causes dizziness, sweating, shaking and confusion.",
            remedy = "If conscious, take fast sugar (glucose, juice, sugar water) then a small meal. If on diabetes medication, check glucose and seek advice; persistent or severe episodes need medical care.",
            doctorToConsult = "General Physician / Endocrinologist",
            urgencyHint = "Confusion, unconsciousness, or seizure — emergency (glucagon/108)."
        ))
        put("viral fever", MedicalAdvice(
            condition = "Viral Fever",
            cause = "A viral infection causing fever, body ache and headache. Common viruses include dengue, flu and enteroviruses.",
            remedy = "Rest, hydrate well, and use fever medication as advised. Monitor for danger signs — in dengue-endemic areas a blood count check may be needed.",
            doctorToConsult = "General Physician",
            urgencyHint = "Bleeding, severe abdominal pain, or very high fever needs urgent care."
        ))
        put("dermatitis", MedicalAdvice(
            condition = "Dermatitis",
            cause = "Inflammation of the skin from allergies, irritants (soaps, chemicals), or conditions like eczema.",
            remedy = "Avoid the trigger, keep skin moisturised, and use mild skincare. An antihistamine or steroid cream may help — use under advice.",
            doctorToConsult = "Dermatologist",
            urgencyHint = "Widespread blistering, fever, or swelling needs prompt care."
        ))
        put("gastroenteritis", MedicalAdvice(
            condition = "Gastroenteritis",
            cause = "Inflammation of the stomach and gut, usually from a virus or food-borne infection.",
            remedy = "Rehydrate with ORS and small frequent meals. Avoid dairy and spicy food until better. Seek care if you cannot keep fluids down.",
            doctorToConsult = "General Physician",
            urgencyHint = "Blood in stool, severe dehydration, or persistent vomiting needs urgent care."
        ))
        put("upper respiratory tract infection", MedicalAdvice(
            condition = "Upper Respiratory Tract Infection",
            cause = "Viral infection of the nose and throat — the common cold. It causes cough, sore throat and nasal congestion.",
            remedy = "Rest, warm fluids, salt-water gargles and symptomatic relief. Most resolve in a week without antibiotics.",
            doctorToConsult = "General Physician (if persistent)",
            urgencyHint = "Worsening cough, high fever, or breathlessness — review."
        ))
        put("fever of unknown origin", MedicalAdvice(
            condition = "Fever of Unknown Origin",
            cause = "A persistent fever without an obvious cause after initial evaluation. Possible causes include infections, inflammation or other conditions.",
            remedy = "Needs a doctor's work-up — blood tests and history are essential to find the cause.",
            doctorToConsult = "General Physician / Infectious Disease Specialist",
            urgencyHint = "Persistent high fever warrants evaluation."
        ))
        put("tension headache", MedicalAdvice(
            condition = "Tension Headache",
            cause = "A headache from muscle tension, stress, poor posture, or eye strain. It is the most common headache type and not dangerous.",
            remedy = "Rest, hydration, stress management and simple pain relief as advised. Track triggers and improve sleep.",
            doctorToConsult = "General Physician / Neurologist (if recurrent)",
            urgencyHint = "Sudden severe headache, fever with stiff neck, or weakness — urgent."
        ))
        put("general debility", MedicalAdvice(
            condition = "General Debility",
            cause = "Generalised weakness and fatigue from many possible causes — poor nutrition, anaemia, sleep problems, stress, or underlying illness.",
            remedy = "Balanced diet, hydration, sleep and light exercise. A blood count and check-up can rule out anaemia or other causes.",
            doctorToConsult = "General Physician",
            urgencyHint = "Severe or progressive weakness needs review."
        ))
        put("undifferentiated illness", MedicalAdvice(
            condition = "Undifferentiated Illness",
            cause = "Symptoms that do not clearly point to one condition. This is common early in an illness.",
            remedy = "Monitor symptoms and re-check; see a doctor if symptoms persist, worsen, or localise.",
            doctorToConsult = "General Physician",
            urgencyHint = "Worsening symptoms warrant a visit."
        ))
        put("vertigo", MedicalAdvice(
            condition = "Vertigo (Positional Vertigo)",
            cause = "Benign paroxysmal positional vertigo (BPPV) is one of the most common causes of vertigo — the sudden sensation that you're spinning or that the inside of your head is spinning. Benign paroxysmal positional vertigo causes brief episodes of mild to intense dizziness.",
            remedy = "Take these steps: lie down; avoid sudden change in body; avoid abrupt head movment; relax.",
            doctorToConsult = "General Physician / ENT Specialist",
            urgencyHint = "Sudden dizziness with weakness, slurred speech or difficulty speaking — emergency."
        ))
        put("aids", MedicalAdvice(
            condition = "HIV / AIDS",
            cause = "Acquired immunodeficiency syndrome (AIDS) is a chronic, potentially life-threatening condition caused by the human immunodeficiency virus (HIV). By damaging your immune system, HIV interferes with your body's ability to fight infection and disease.",
            remedy = "Take these steps: avoid open cuts; wear ppe if possible; consult doctor; follow up.",
            doctorToConsult = "Infectious Disease Specialist / General Physician",
            urgencyHint = "High fever, severe weight loss, or any opportunistic infection needs specialist care."
        ))
        put("acne", MedicalAdvice(
            condition = "Acne",
            cause = "Acne vulgaris is the formation of comedones, papules, pustules, nodules, and/or cysts as a result of obstruction and inflammation of pilosebaceous units (hair follicles and their accompanying sebaceous gland). Acne develops on the face and upper trunk. It most often affects adolescents.",
            remedy = "Take these steps: bath twice; avoid fatty spicy food; drink plenty of water; avoid too many products.",
            doctorToConsult = "Dermatologist",
            urgencyHint = "Severe, painful or scarring acne warrants dermatology review."
        ))
        put("alcoholic_hepatitis", MedicalAdvice(
            condition = "Alcoholic hepatitis",
            cause = "Alcoholic hepatitis is a diseased, inflammatory condition of the liver caused by heavy alcohol consumption over an extended period of time. It's also aggravated by binge drinking and ongoing alcohol use. If you develop this condition, you must stop drinking alcohol",
            remedy = "Take these steps: stop alcohol consumption; consult doctor; medication; follow up.",
            doctorToConsult = "Gastroenterologist / Hepatologist",
            urgencyHint = "Jaundice, confusion, or abdominal swelling needs urgent care."
        ))
        put("allergy", MedicalAdvice(
            condition = "Allergy",
            cause = "An allergy is an immune system response to a foreign substance that's not typically harmful to your body.They can include certain foods, pollen, or pet dander. Your immune system's job is to keep you healthy by fighting harmful pathogens.",
            remedy = "Take these steps: apply calamine; cover area with bandage; use ice to compress itching.",
            doctorToConsult = "Allergist / General Physician",
            urgencyHint = "Breathing difficulty, swelling of the face/lips, or dizziness — emergency."
        ))
        put("arthritis", MedicalAdvice(
            condition = "Arthritis",
            cause = "Arthritis is the swelling and tenderness of one or more of your joints. The main symptoms of arthritis are joint pain and stiffness, which typically worsen with age. The most common types of arthritis are osteoarthritis and rheumatoid arthritis.",
            remedy = "Take these steps: exercise; use hot and cold therapy; try acupuncture; massage.",
            doctorToConsult = "Rheumatologist / Orthopaedician",
            urgencyHint = "Sudden joint swelling with fever, or inability to move a joint — prompt care."
        ))
        put("cervical_spondylosis", MedicalAdvice(
            condition = "Cervical spondylosis",
            cause = "Cervical spondylosis is a general term for age-related wear and tear affecting the spinal disks in your neck. As the disks dehydrate and shrink, signs of osteoarthritis develop, including bony projections along the edges of bones (bone spurs).",
            remedy = "Take these steps: use heating pad or cold pack; exercise; take otc pain reliver; consult doctor.",
            doctorToConsult = "Orthopaedician / Neurologist",
            urgencyHint = "Neck pain with arm numbness, weakness, or difficulty walking — review."
        ))
        put("chicken_pox", MedicalAdvice(
            condition = "Chicken pox",
            cause = "Chickenpox is a highly contagious disease caused by the varicella-zoster virus (VZV). It can cause an itchy, blister-like rash. The rash first appears on the chest, back, and face, and then spreads over the entire body, causing between 250 and 500 itchy blisters.",
            remedy = "Take these steps: use neem in bathing ; consume neem leaves; take vaccine; avoid public places.",
            doctorToConsult = "General Physician",
            urgencyHint = "High fever, breathing difficulty, or rash turning very red/painful — urgent."
        ))
        put("chronic_cholestasis", MedicalAdvice(
            condition = "Chronic cholestasis",
            cause = "Chronic cholestatic diseases, whether occurring in infancy, childhood or adulthood, are characterized by defective bile acid transport from the liver to the intestine, which is caused by primary damage to the biliary epithelium in most cases",
            remedy = "Take these steps: cold baths; anti itch medicine; consult doctor; eat healthy.",
            doctorToConsult = "Gastroenterologist / Hepatologist",
            urgencyHint = "Persistent itching, yellowing of skin/eyes, or dark urine — prompt review."
        ))
        put("common_cold", MedicalAdvice(
            condition = "Common Cold",
            cause = "The common cold is a viral infection of your nose and throat (upper respiratory tract). It's usually harmless, although it might not feel that way. Many types of viruses can cause a common cold.",
            remedy = "Take these steps: drink vitamin c rich drinks; take vapour; avoid cold food; keep fever in check.",
            doctorToConsult = "General Physician (if persistent)",
            urgencyHint = "Worsening cough, high fever, or breathlessness — review."
        ))
        put("diabetes", MedicalAdvice(
            condition = "Diabetes",
            cause = "Diabetes is a disease that occurs when your blood glucose, also called blood sugar, is too high. Blood glucose is your main source of energy and comes from the food you eat. Insulin, a hormone made by the pancreas, helps glucose from food get into your cells to be used for energy.",
            remedy = "Take these steps: have balanced diet; exercise; consult doctor; follow up.",
            doctorToConsult = "Endocrinologist / General Physician",
            urgencyHint = "Very high sugar with vomiting, confusion, or deep breathing — emergency."
        ))
        put("piles", MedicalAdvice(
            condition = "Piles (Hemorrhoids)",
            cause = "A condition identified from the reported symptoms. The exact cause needs clinical evaluation.",
            remedy = "Take these steps: avoid fatty spicy food; consume witch hazel; warm bath with epsom salt; consume alovera juice.",
            doctorToConsult = "General / Colorectal Surgeon",
            urgencyHint = "Heavy bleeding, large clots, or severe pain — prompt review."
        ))
        put("drug_reaction", MedicalAdvice(
            condition = "Drug Reaction",
            cause = "An adverse drug reaction (ADR) is an injury caused by taking medication. ADRs may occur following a single dose or prolonged administration of a drug or result from the combination of two or more drugs.",
            remedy = "Take these steps: stop irritation; consult nearest hospital; stop taking drug; follow up.",
            doctorToConsult = "Dermatologist / General Physician",
            urgencyHint = "Face/lip swelling, breathing difficulty, or widespread blistering — emergency."
        ))
        put("fungal_infection", MedicalAdvice(
            condition = "Fungal infection",
            cause = "In humans, fungal infections occur when an invading fungus takes over an area of the body and is too much for the immune system to handle. Fungi can live in the air, soil, water, and plants. There are also some fungi that live naturally in the human body. Like many microbes, there are helpful fungi and harmful fungi.",
            remedy = "Take these steps: bath twice; use detol or neem in bathing water; keep infected area dry; use clean cloths.",
            doctorToConsult = "Dermatologist",
            urgencyHint = "Widespread rash or secondary bacterial infection — prompt review."
        ))
        put("gerd", MedicalAdvice(
            condition = "GERD",
            cause = "Gastroesophageal reflux disease, or GERD, is a digestive disorder that affects the lower esophageal sphincter (LES), the ring of muscle between the esophagus and stomach. Many people, including pregnant women, suffer from heartburn or acid indigestion caused by GERD.",
            remedy = "Take these steps: avoid fatty spicy food; avoid lying down after eating; maintain healthy weight; exercise.",
            doctorToConsult = "Gastroenterologist / General Physician",
            urgencyHint = "Chest pain with sweating or breathlessness (not just heartburn) — emergency."
        ))
        put("heart_attack", MedicalAdvice(
            condition = "Heart attack",
            cause = "The death of heart muscle due to the loss of blood supply. The loss of blood supply is usually caused by a complete blockage of a coronary artery, one of the arteries that supplies blood to the heart muscle.",
            remedy = "Take these steps: call ambulance; chew or swallow asprin; keep calm.",
            doctorToConsult = "Emergency Physician / Cardiologist",
            urgencyHint = "Chest pain, sweating, or breathlessness — call emergency services now."
        ))
        put("hepatitis_b", MedicalAdvice(
            condition = "Hepatitis B",
            cause = "Hepatitis B is an infection of your liver. It can cause scarring of the organ, liver failure, and cancer. It can be fatal if it isn't treated. It's spread when people come in contact with the blood, open sores, or body fluids of someone who has the hepatitis B virus.",
            remedy = "Take these steps: consult nearest hospital; vaccination; eat healthy; medication.",
            doctorToConsult = "Hepatologist / Infectious Disease Specialist",
            urgencyHint = "Severe fatigue, jaundice, or abdominal pain — prompt review."
        ))
        put("hepatitis_c", MedicalAdvice(
            condition = "Hepatitis C",
            cause = "Inflammation of the liver due to the hepatitis C virus (HCV), which is usually spread via blood transfusion (rare), hemodialysis, and needle sticks. The damage hepatitis C does to the liver can lead to cirrhosis and its complications as well as cancer.",
            remedy = "Take these steps: Consult nearest hospital; vaccination; eat healthy; medication.",
            doctorToConsult = "Hepatologist / Infectious Disease Specialist",
            urgencyHint = "Jaundice, fluid in the abdomen, or bleeding tendency — urgent."
        ))
        put("hepatitis_d", MedicalAdvice(
            condition = "Hepatitis D",
            cause = "Hepatitis D, also known as the hepatitis delta virus, is an infection that causes the liver to become inflamed. This swelling can impair liver function and cause long-term liver problems, including liver scarring and cancer. The condition is caused by the hepatitis D virus (HDV).",
            remedy = "Take these steps: consult doctor; medication; eat healthy; follow up.",
            doctorToConsult = "Hepatologist / Infectious Disease Specialist",
            urgencyHint = "Rapid jaundice or liver failure signs — urgent specialist care."
        ))
        put("hepatitis_e", MedicalAdvice(
            condition = "Hepatitis E",
            cause = "A rare form of liver inflammation caused by infection with the hepatitis E virus (HEV). It is transmitted via food or drink handled by an infected person or through infected water supplies in areas where fecal matter may get into the water. Hepatitis E does not cause chronic liver disease.",
            remedy = "Take these steps: stop alcohol consumption; rest; consult doctor; medication.",
            doctorToConsult = "General Physician / Hepatologist",
            urgencyHint = "Jaundice, especially in pregnancy — urgent care."
        ))
        put("hyperthyroidism", MedicalAdvice(
            condition = "Hyperthyroidism",
            cause = "Hyperthyroidism (overactive thyroid) occurs when your thyroid gland produces too much of the hormone thyroxine. Hyperthyroidism can accelerate your body's metabolism, causing unintentional weight loss and a rapid or irregular heartbeat.",
            remedy = "Take these steps: eat healthy; massage; use lemon balm; take radioactive iodine treatment.",
            doctorToConsult = "Endocrinologist",
            urgencyHint = "Racing heart, high fever, or severe agitation — emergency."
        ))
        put("hypothyroidism", MedicalAdvice(
            condition = "Hypothyroidism",
            cause = "Hypothyroidism, also called underactive thyroid or low thyroid, is a disorder of the endocrine system in which the thyroid gland does not produce enough thyroid hormone.",
            remedy = "Take these steps: reduce stress; exercise; eat healthy; get proper sleep.",
            doctorToConsult = "Endocrinologist",
            urgencyHint = "Severe fatigue, cold intolerance, or swelling — review."
        ))
        put("impetigo", MedicalAdvice(
            condition = "Impetigo",
            cause = "Impetigo (im-puh-TIE-go) is a common and highly contagious skin infection that mainly affects infants and children. Impetigo usually appears as red sores on the face, especially around a child's nose and mouth, and on hands and feet. The sores burst and develop honey-colored crusts.",
            remedy = "Take these steps: soak affected area in warm water; use antibiotics; remove scabs with wet compressed cloth; consult doctor.",
            doctorToConsult = "Dermatologist",
            urgencyHint = "Spreading sores with fever — prompt care."
        ))
        put("jaundice", MedicalAdvice(
            condition = "Jaundice",
            cause = "Yellow staining of the skin and sclerae (the whites of the eyes) by abnormally high blood levels of the bile pigment bilirubin. The yellowing extends to other tissues and body fluids. Jaundice was once called the \"morbus regius\" (the regal disease) in the belief that only the touch of a king could cure it",
            remedy = "Take these steps: drink plenty of water; consume milk thistle; eat fruits and high fiberous food; medication.",
            doctorToConsult = "Gastroenterologist / Hepatologist",
            urgencyHint = "Yellowing with confusion, bleeding, or severe pain — urgent."
        ))
        put("osteoarthritis", MedicalAdvice(
            condition = "Osteoarthritis",
            cause = "Osteoarthritis is the most common form of arthritis, affecting millions of people worldwide. It occurs when the protective cartilage that cushions the ends of your bones wears down over time.",
            remedy = "Take these steps: acetaminophen; consult nearest hospital; follow up; salt baths.",
            doctorToConsult = "Orthopaedician",
            urgencyHint = "Severe pain limiting daily activity — review."
        ))
        put("paralysis", MedicalAdvice(
            condition = "Paralysis (Suspected Stroke)",
            cause = "Intracerebral hemorrhage (ICH) is when blood suddenly bursts into brain tissue, causing damage to your brain. Symptoms usually appear suddenly during ICH. They include headache, weakness, confusion, and paralysis, particularly on one side of your body.",
            remedy = "Take these steps: massage; eat healthy; exercise; consult doctor.",
            doctorToConsult = "Emergency Physician / Neurologist",
            urgencyHint = "Sudden weakness on one side, facial droop, or speech difficulty — call 108 now."
        ))
        put("peptic_ulcer", MedicalAdvice(
            condition = "Peptic Ulcer Disease",
            cause = "Peptic ulcer disease (PUD) is a break in the inner lining of the stomach, the first part of the small intestine, or sometimes the lower esophagus. An ulcer in the stomach is called a gastric ulcer, while one in the first part of the intestines is a duodenal ulcer.",
            remedy = "Take these steps: avoid fatty spicy food; consume probiotic food; eliminate milk; limit alcohol.",
            doctorToConsult = "Gastroenterologist",
            urgencyHint = "Black stools, vomiting blood, or severe stomach pain — emergency."
        ))
        put("psoriasis", MedicalAdvice(
            condition = "Psoriasis",
            cause = "Psoriasis is a common skin disorder that forms thick, red, bumpy patches covered with silvery scales. They can pop up anywhere, but most appear on the scalp, elbows, knees, and lower back. Psoriasis can't be passed from person to person. It does sometimes happen in members of the same family.",
            remedy = "Take these steps: wash hands with warm soapy water; stop bleeding using pressure; consult doctor; salt baths.",
            doctorToConsult = "Dermatologist",
            urgencyHint = "Widespread flare or joint pain — dermatology review."
        ))
        put("tuberculosis", MedicalAdvice(
            condition = "Tuberculosis",
            cause = "Tuberculosis (TB) is an infectious disease usually caused by Mycobacterium tuberculosis (MTB) bacteria. Tuberculosis generally affects the lungs, but can also affect other parts of the body. Most infections show no symptoms, in which case it is known as latent tuberculosis.",
            remedy = "Take these steps: cover mouth; consult doctor; medication; rest.",
            doctorToConsult = "Pulmonologist / TB Specialist",
            urgencyHint = "Coughing blood, high fever, or severe weight loss — urgent."
        ))
        put("urinary_tract_infection", MedicalAdvice(
            condition = "Urinary Tract Infection",
            cause = "Urinary tract infection: An infection of the kidney, ureter, bladder, or urethra. Abbreviated UTI. Not everyone with a UTI has symptoms, but common symptoms include a frequent urge to urinate and pain or burning when urinating.",
            remedy = "Take these steps: drink plenty of water; increase vitamin c intake; drink cranberry juice; take probiotics.",
            doctorToConsult = "Urologist / General Physician",
            urgencyHint = "Fever with flank pain, or blood in urine — prompt care."
        ))
        put("varicose_veins", MedicalAdvice(
            condition = "Varicose veins",
            cause = "A vein that has enlarged and twisted, often appearing as a bulging, blue blood vessel that is clearly visible through the skin. Varicose veins are most common in older adults, particularly women, and occur especially on the legs.",
            remedy = "Take these steps: lie down flat and raise the leg high; use oinments; use vein compression; dont stand still for long.",
            doctorToConsult = "Vascular Surgeon",
            urgencyHint = "Sudden leg swelling, redness, or chest pain after a long journey — urgent."
        ))
        put("hepatitis_a", MedicalAdvice(
            condition = "Hepatitis A",
            cause = "Hepatitis A is a highly contagious liver infection caused by the hepatitis A virus. The virus is one of several types of hepatitis viruses that cause inflammation and affect your liver's ability to function.",
            remedy = "Take these steps: Consult nearest hospital; wash hands through; avoid fatty spicy food; medication.",
            doctorToConsult = "General Physician / Hepatologist",
            urgencyHint = "Yellowing skin/eyes, dark urine, or persistent vomiting — prompt review."
        ))
    }

    /**
     * Expose all knowledge-base entries for on-device RAG indexing.
     * Returns (normalized condition key, MedicalAdvice) pairs.
     */
    fun allEntries(): List<Pair<String, MedicalAdvice>> =
        entries.map { (key, advice) -> key to advice }

    /**
     * Look up medical advice for a predicted condition.
     * Matching is case-insensitive and tolerant of the small label variants
     * used across models (e.g. "Adenocarcinoma" vs "CA Stroma").
     */
    fun adviceFor(condition: String): MedicalAdvice {        val key = condition.trim().lowercase().replace('_', ' ')
        val direct = entries[key] ?: entries[key.replace(' ', '_')]
        if (direct != null) return direct

        // Substring fallback for variant labels.
        for ((entryKey, advice) in entries) {
            val normalized = entryKey.replace('_', ' ')
            if (key.contains(normalized) || normalized.contains(key)) return advice
        }
        return fallback(condition)
    }

    /**
     * Organ identification (CT organ models) names an organ region, not a
     * disease. Return honest, non-diagnostic guidance for those labels.
     */
    fun organAdvice(organ: String): MedicalAdvice = MedicalAdvice(
        condition = organ,
        cause = "The CT model identified the organ region shown in the scan. This identifies anatomy only — it does not by itself indicate a disease.",
        remedy = "No treatment is implied by organ identification alone. Discuss the scan with the clinician who ordered it; they will interpret findings in context.",
        doctorToConsult = "Radiologist / Referring Clinician",
        urgencyHint = "Follow the guidance of the clinician who requested the scan."
    )

    private fun fallback(condition: String): MedicalAdvice = MedicalAdvice(
        condition = condition,
        cause = "This result needs clinical interpretation. The screening model has limited information and cannot explain the finding alone.",
        remedy = "Consult a doctor with this result for proper evaluation — do not self-diagnose or self-treat.",
        doctorToConsult = "General Physician",
        urgencyHint = "Seek medical advice to interpret this result."
    )
}
