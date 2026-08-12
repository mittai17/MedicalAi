package com.swasthai.app.ai.engine

/**
 * Screening model registry.
 *
 * Maps each supported screening type to its on-device TFLite asset,
 * input format, output labels, and reporting gate. Binary models emit a
 * single sigmoid (positive-class probability); multiclass models emit a
 * softmax over [labels]; multilabel models emit a sigmoid per label,
 * gated by [perLabelThresholds].
 */
enum class ScanType(
    val modelFile: String,
    val displayName: String,
    val description: String,
    val isBinary: Boolean,
    val labels: List<String>,
    val inputChannels: Int,
    val reportGate: Float,
    val isMultilabel: Boolean = false,
    val perLabelThresholds: List<Float>? = null,
    val inputSize: Int = 28
) {
    PNEUMONIA(
        modelFile = "pneumonia_cnn_224.tflite",
        displayName = "Chest X-Ray",
        description = "Detect pneumonia from a chest X-ray",
        isBinary = true,
        labels = listOf("Pneumonia", "Normal"),
        inputChannels = 1,
        reportGate = 0.85f,
        inputSize = 224
    ),
    BREAST_SCAN(
        modelFile = "breast_scan.tflite",
        displayName = "Breast Scan",
        description = "Screen breast ultrasound for malignancy",
        isBinary = true,
        labels = listOf("Malignant", "Benign / Normal"),
        inputChannels = 1,
        reportGate = 0.09f
    ),
    RETINA(
        modelFile = "retina_dr_224.tflite",
        displayName = "Retina (DR)",
        description = "Screen retinal images for diabetic retinopathy",
        isBinary = true,
        labels = listOf("Diabetic Retinopathy", "No DR"),
        inputChannels = 3,
        reportGate = 0.19f,
        inputSize = 224
    ),
    SKIN_LESION(
        modelFile = "skin_lesion.tflite",
        displayName = "Skin Lesion",
        description = "Classify skin lesion into 7 categories",
        isBinary = false,
        labels = listOf(
            "Actinic Keratosis / IEC", "Basal Cell Carcinoma", "Benign Keratosis",
            "Dermatofibroma", "Melanoma", "Melanocytic Nevus", "Vascular Lesion"
        ),
        inputChannels = 3,
        reportGate = 0.6f
    ),
    OCT_RETINA(
        modelFile = "oct_retina.tflite",
        displayName = "Retina OCT",
        description = "Classify OCT scan of the retina",
        isBinary = false,
        labels = listOf("CNV", "DME", "Drusen", "Normal"),
        inputChannels = 1,
        reportGate = 0.7f
    ),
    BLOOD_CELL(
        modelFile = "blood_cell.tflite",
        displayName = "Blood Cell",
        description = "Classify blood cell type",
        isBinary = false,
        labels = listOf(
            "Basophil", "Eosinophil", "Erythroblast", "Immature Granulocyte",
            "Lymphocyte", "Monocyte", "Neutrophil", "Platelet"
        ),
        inputChannels = 3,
        reportGate = 0.7f
    ),
    COLON_PATH(
        modelFile = "colon_path.tflite",
        displayName = "Colon Pathology",
        description = "Classify colon tissue pathology",
        isBinary = false,
        labels = listOf(
            "Adipose", "Background", "Debris", "Lymphocytes", "Mucus",
            "Smooth Muscle", "Normal Mucosa", "CA Stroma", "Adenocarcinoma"
        ),
        inputChannels = 3,
        reportGate = 0.7f
    ),
    CHEST_XRAY(
        modelFile = "chest_xray.tflite",
        displayName = "Chest X-Ray (14 Findings)",
        description = "Detect up to 14 chest findings from an X-ray",
        isBinary = false,
        labels = listOf(
            "Atelectasis", "Cardiomegaly", "Effusion", "Infiltration",
            "Mass", "Nodule", "Pneumonia", "Pneumothorax", "Consolidation",
            "Edema", "Emphysema", "Fibrosis", "Pleural", "Hernia"
        ),
        inputChannels = 1,
        reportGate = 0.35f,
        isMultilabel = true,
        perLabelThresholds = listOf(
            0.15f, 0.05f, 0.15f, 0.20f, 0.10f, 0.05f, 0.05f,
            0.15f, 0.05f, 0.05f, 0.10f, 0.05f, 0.05f, 0.80f
        )
    ),
    KIDNEY_TISSUE(
        modelFile = "tissue_kidney.tflite",
        displayName = "Kidney Tissue",
        description = "Classify kidney cortex cell types",
        isBinary = false,
        labels = listOf(
            "Collecting Duct / Conn Tubule", "Distal Convoluted Tubule",
            "Glomerular Endothelial", "Interstitial Endothelial",
            "Leukocytes", "Podocytes", "Proximal Tubule", "Thick Ascending Limb"
        ),
        inputChannels = 1,
        reportGate = 0.55f
    ),
    CT_ORGAN_AXIAL(
        modelFile = "organ_axial.tflite",
        displayName = "CT Organ (Axial)",
        description = "Identify body organ in axial CT slice",
        isBinary = false,
        labels = listOf(
            "Bladder", "Femur-L", "Femur-R", "Heart", "Kidney-L",
            "Kidney-R", "Liver", "Lung-L", "Lung-R", "Pancreas", "Spleen"
        ),
        inputChannels = 1,
        reportGate = 0.6f
    ),
    CT_ORGAN_CORONAL(
        modelFile = "organ_coronal.tflite",
        displayName = "CT Organ (Coronal)",
        description = "Identify body organ in coronal CT slice",
        isBinary = false,
        labels = listOf(
            "Bladder", "Femur-L", "Femur-R", "Heart", "Kidney-L",
            "Kidney-R", "Liver", "Lung-L", "Lung-R", "Pancreas", "Spleen"
        ),
        inputChannels = 1,
        reportGate = 0.6f
    ),
    CT_ORGAN_SAGITTAL(
        modelFile = "organ_sagittal.tflite",
        displayName = "CT Organ (Sagittal)",
        description = "Identify body organ in sagittal CT slice",
        isBinary = false,
        labels = listOf(
            "Bladder", "Femur-L", "Femur-R", "Heart", "Kidney-L",
            "Kidney-R", "Liver", "Lung-L", "Lung-R", "Pancreas", "Spleen"
        ),
        inputChannels = 1,
        reportGate = 0.55f
    )
}
