import os
os.environ.setdefault("TF_CPP_MIN_LOG_LEVEL", "3")
os.environ.setdefault("CUDA_VISIBLE_DEVICES", "")
import json
import numpy as np
import tensorflow as tf
from tensorflow import keras
from sklearn.metrics import accuracy_score, roc_auc_score, f1_score, precision_score, recall_score

import train_screening  # registers WeightedBCE so chest_xray model can load

# (dataset key, npz, out tflite name, binary?, channels, labels, binarize target)
MODELS = [
    ("breast_scan", "data/breastmnist.npz", "breast_scan.tflite", True, 1,
     ["malignant", "normal/benign"], "label0"),
    ("retina", "data/retinamnist.npz", "retina_dr.tflite", True, 3,
     ["no DR", "DR present"], "any"),
    ("skin_lesion", "data/dermamnist.npz", "skin_lesion.tflite", False, 3,
     ["AK/IEC", "BCC", "BKL", "DF", "Melanoma", "Nevi", "Vascular"], None),
    ("oct_retina", "data/octmnist.npz", "oct_retina.tflite", False, 1,
     ["CNV", "DME", "Drusen", "Normal"], None),
    ("blood_cell", "data/bloodmnist.npz", "blood_cell.tflite", False, 3,
     ["Basophil", "Eosinophil", "Erythroblast", "Immature Granulo",
      "Lymphocyte", "Monocyte", "Neutrophil", "Platelet"], None),
    ("colon_path", "data/pathmnist.npz", "colon_path.tflite", False, 3,
     ["Adipose", "Background", "Debris", "Lymphocytes", "Mucus",
      "Smooth Muscle", "Normal Mucosa", "CA Stroma", "AdenoCA"], None),
    ("chest_xray", "data/chestmnist.npz", "chest_xray.tflite", "ml", 1,
     ["Atelectasis", "Cardiomegaly", "Effusion", "Infiltration",
      "Mass", "Nodule", "Pneumonia", "Pneumothorax", "Consolidation",
      "Edema", "Emphysema", "Fibrosis", "Pleural", "Hernia"], None),
    ("tissue_kidney", "data/tissuemnist.npz", "tissue_kidney.tflite", False, 1,
     ["Collecting Duct/Conn Tubule", "Distal Convoluted Tubule",
      "Glomerular Endothelial", "Interstitial Endothelial",
      "Leukocytes", "Podocytes", "Proximal Tubule", "Thick Ascending Limb"], None),
    ("organ_axial", "data/organamnist.npz", "organ_axial.tflite", False, 1,
     ["Bladder", "Femur-L", "Femur-R", "Heart", "Kidney-L",
      "Kidney-R", "Liver", "Lung-L", "Lung-R", "Pancreas", "Spleen"], None),
    ("organ_coronal", "data/organcmnist.npz", "organ_coronal.tflite", False, 1,
     ["Bladder", "Femur-L", "Femur-R", "Heart", "Kidney-L",
      "Kidney-R", "Liver", "Lung-L", "Lung-R", "Pancreas", "Spleen"], None),
    ("organ_sagittal", "data/organsmnist.npz", "organ_sagittal.tflite", False, 1,
     ["Bladder", "Femur-L", "Femur-R", "Heart", "Kidney-L",
      "Kidney-R", "Liver", "Lung-L", "Lung-R", "Pancreas", "Spleen"], None),
]

# Phone-capturable models retrained at 224px (MedMNIST+). Format:
#   keras model name, npz, tflite out, binary?, channels, labels, target, dataset key
HD_224 = [
    ("retina_224", "data/224/retinamnist_224.npz", "retina_dr_224.tflite", True, 3,
     ["no DR", "DR present"], "any", "retina"),
    ("pneumonia_cnn_224", "data/224/pneumoniamnist_224.npz", "pneumonia_cnn_224.tflite", True, 1,
     ["Pneumonia", "Normal"], "label1", "pneumonia"),
    ("breast_scan_224", "data/224/breastmnist_224.npz", "breast_scan_224.tflite", True, 1,
     ["malignant", "normal/benign"], "label0", "breast"),
    ("skin_lesion_224", "data/224/dermamnist_224.npz", "skin_lesion_224.tflite", False, 3,
     ["AK/IEC", "BCC", "BKL", "DF", "Melanoma", "Nevi", "Vascular"], None, "derma"),
    ("chest_xray_224", "data/224/chestmnist_224.npz", "chest_xray_224.tflite", "ml", 1,
     ["Atelectasis", "Cardiomegaly", "Effusion", "Infiltration",
      "Mass", "Nodule", "Pneumonia", "Pneumothorax", "Consolidation",
      "Edema", "Emphysema", "Fibrosis", "Pleural", "Hernia"], None, "chest"),
]

for out_name, npz, tflite_name, binary, channels, labels, target, _ in HD_224:
    print(f"\n=== [224] {out_name} -> {tflite_name} ===", flush=True)
    d = np.load(npz)
    x_te = d["test_images"].astype(np.float32) / 255.0
    if binary == "ml":
        y_te = d["test_labels"].astype(np.int64)
    else:
        y_te = d["test_labels"].astype(np.int64).ravel()
    if target == "any":
        y_te = (y_te > 0).astype(np.int64)
    elif target == "label0":
        y_te = (y_te == 0).astype(np.int64)
    elif target == "label1":
        y_te = (y_te == 1).astype(np.int64)

    model = keras.models.load_model(f"models/{out_name}_float.keras")

    k = model.predict(x_te, verbose=0)
    if binary is True:
        k = k.ravel()

    c = tf.lite.TFLiteConverter.from_keras_model(model)
    c.optimizations = [tf.lite.Optimize.DEFAULT]
    c.target_spec.supported_types = [tf.float16]
    tfl = c.convert()
    open(f"models/{tflite_name}", "wb").write(tfl)

    interp = tf.lite.Interpreter(model_path=f"models/{tflite_name}",
        experimental_op_resolver_type=tf.lite.experimental.OpResolverType.BUILTIN_WITHOUT_DEFAULT_DELEGATES)
    interp.allocate_tensors()
    in_d = interp.get_input_details()[0]
    out_d = interp.get_output_details()[0]
    probs = np.zeros((len(x_te), len(labels)), dtype=np.float32)
    for i in range(len(x_te)):
        interp.set_tensor(in_d["index"], x_te[i].reshape(1, *in_d["shape"][1:]))
        interp.invoke()
        probs[i] = interp.get_tensor(out_d["index"]).ravel()

    if binary is True:
        p = probs[:, 0]
        print(f"  kAUROC={roc_auc_score(y_te, k):.4f} tAUROC={roc_auc_score(y_te, p):.4f} maxdiff={np.abs(p-k).max():.5f}")
    elif binary == "ml":
        print(f"  k macroF1={f1_score(y_te, k>=0.5, average='macro', zero_division=0):.4f} t macroF1={f1_score(y_te, probs>=0.5, average='macro', zero_division=0):.4f}")
        print(f"  flips: {(k>=0.5 != probs>=0.5).sum()} maxdiff={np.abs(probs-k).max():.5f}")
    else:
        print(f"  k acc={accuracy_score(y_te, k.argmax(axis=1)):.4f} t acc={accuracy_score(y_te, probs.argmax(axis=1)):.4f}")
        print(f"  flips: {(k.argmax(axis=1) != probs.argmax(axis=1)).sum()}/{len(y_te)} maxdiff={np.abs(probs-k).max():.5f}")
    print(f"  size: {os.path.getsize(f'models/{tflite_name}')/1e6:.2f} MB  input={in_d['shape'][1:]}")

for out_name, npz, tflite_name, binary, channels, labels, target in MODELS:
    print(f"\n=== {out_name} -> {tflite_name} ===", flush=True)
    d = np.load(npz)
    x_te = d["test_images"].astype(np.float32) / 255.0
    if binary == "ml":
        y_te = d["test_labels"].astype(np.int64)
    else:
        y_te = d["test_labels"].astype(np.int64).ravel()
    if target == "any":
        y_te = (y_te > 0).astype(np.int64)
    elif target == "label0":
        y_te = (y_te == 0).astype(np.int64)

    model = keras.models.load_model(f"models/{out_name}_float.keras")

    k = model.predict(x_te, verbose=0)
    if binary is True:
        k = k.ravel()

    # FP16 conversion
    c = tf.lite.TFLiteConverter.from_keras_model(model)
    c.optimizations = [tf.lite.Optimize.DEFAULT]
    c.target_spec.supported_types = [tf.float16]
    tfl = c.convert()
    open(f"models/{tflite_name}", "wb").write(tfl)

    # verify
    interp = tf.lite.Interpreter(model_path=f"models/{tflite_name}",
        experimental_op_resolver_type=tf.lite.experimental.OpResolverType.BUILTIN_WITHOUT_DEFAULT_DELEGATES)
    interp.allocate_tensors()
    in_d = interp.get_input_details()[0]
    out_d = interp.get_output_details()[0]
    probs = np.zeros((len(x_te), len(labels)), dtype=np.float32)
    for i in range(len(x_te)):
        interp.set_tensor(in_d["index"], x_te[i].reshape(1, *in_d["shape"][1:]))
        interp.invoke()
        probs[i] = interp.get_tensor(out_d["index"]).ravel()

    if binary is True:
        p = probs[:, 0]
        thr = json.load(open(f"models/{out_name}_metrics.json"))["threshold"]
        print(f"  binary thr={thr:.2f} kAUROC={roc_auc_score(y_te, k):.4f} tAUROC={roc_auc_score(y_te, p):.4f}")
        print(f"  maxdiff={np.abs(p-k).max():.5f} mean={np.abs(p-k).mean():.5f}")
    elif binary == "ml":
        yp_t = probs >= 0.5
        yp_k = k >= 0.5
        print(f"  k macroF1={f1_score(y_te, yp_k, average='macro', zero_division=0):.4f} t macroF1={f1_score(y_te, yp_t, average='macro', zero_division=0):.4f}")
        print(f"  flips: {(yp_k != yp_t).sum()}  maxdiff={np.abs(probs-k).max():.5f}")
    else:
        yp_t = probs.argmax(axis=1)
        yp_k = k.argmax(axis=1)
        print(f"  k acc={accuracy_score(y_te, yp_k):.4f} t acc={accuracy_score(y_te, yp_t):.4f}")
        print(f"  flips (argmax differs): {(yp_k != yp_t).sum()}/{len(y_te)}  maxprob diff={np.abs(probs-k).max():.5f}")
    print(f"  size: {os.path.getsize(f'models/{tflite_name}')/1e6:.2f} MB")

print("\nDONE")
