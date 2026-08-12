import os
os.environ.setdefault("TF_CPP_MIN_LOG_LEVEL", "3")
if os.environ.get("SWASTHAI_GPU"):
    os.environ["CUDA_VISIBLE_DEVICES"] = "0"
else:
    os.environ.setdefault("CUDA_VISIBLE_DEVICES", "")
import json
import sys
import time
import numpy as np
from tensorflow import keras
from keras.saving import register_keras_serializable
from tensorflow.keras import layers
from sklearn.metrics import (
    roc_auc_score, precision_score, recall_score, f1_score,
    accuracy_score, confusion_matrix
)

CONFIGS = {
    "breast": {
        "data": "data/breastmnist.npz", "channels": 1, "binary": True,
        "labels": ["malignant", "normal/benign"], "target": "malignant",
        "pos_label": 0, "out": "breast_scan", "name": "Breast Ultrasound",
    },
    "pneumonia": {
        "data": "data/pneumoniamnist.npz", "channels": 1, "binary": True,
        "labels": ["Pneumonia", "Normal"], "target": "Pneumonia",
        "pos_label": 1, "out": "pneumonia_cnn", "name": "Pneumonia",
    },
    "derma": {
        "data": "data/dermamnist.npz", "channels": 3, "binary": False,
        "labels": ["AK/IEC", "BCC", "BKL", "DF", "Melanoma", "Nevi", "Vascular"],
        "out": "skin_lesion", "name": "Skin Lesion",
    },
    "retina": {
        "data": "data/retinamnist.npz", "channels": 3, "binary": True,
        "labels": ["no DR", "DR present"], "target": "DR", "pos_label": "any",
        "out": "retina", "name": "Retina (DR)",
    },
    "oct": {
        "data": "data/octmnist.npz", "channels": 1, "binary": False,
        "labels": ["CNV", "DME", "Drusen", "Normal"],
        "out": "oct_retina", "name": "Retina OCT",
    },
    "blood": {
        "data": "data/bloodmnist.npz", "channels": 3, "binary": False,
        "labels": ["Basophil", "Eosinophil", "Erythroblast", "Immature Granulo",
                   "Lymphocyte", "Monocyte", "Neutrophil", "Platelet"],
        "out": "blood_cell", "name": "Blood Cell",
    },
    "path": {
        "data": "data/pathmnist.npz", "channels": 3, "binary": False,
        "labels": ["Adipose", "Background", "Debris", "Lymphocytes", "Mucus",
                   "Smooth Muscle", "Normal Mucosa", "CA Stroma", "AdenoCA"],
        "out": "colon_path", "name": "Colon Pathology",
    },
    "chest": {
        "data": "data/chestmnist.npz", "channels": 1, "multilabel": True,
        "labels": ["Atelectasis", "Cardiomegaly", "Effusion", "Infiltration",
                   "Mass", "Nodule", "Pneumonia", "Pneumothorax", "Consolidation",
                   "Edema", "Emphysema", "Fibrosis", "Pleural", "Hernia"],
        "out": "chest_xray", "name": "Chest X-Ray",
    },
    "tissue": {
        "data": "data/tissuemnist.npz", "channels": 1, "binary": False,
        "labels": ["Collecting Duct/Conn Tubule", "Distal Convoluted Tubule",
                   "Glomerular Endothelial", "Interstitial Endothelial",
                   "Leukocytes", "Podocytes", "Proximal Tubule", "Thick Ascending Limb"],
        "out": "tissue_kidney", "name": "Kidney Tissue",
    },
    "organa": {
        "data": "data/organamnist.npz", "channels": 1, "binary": False,
        "labels": ["Bladder", "Femur-L", "Femur-R", "Heart", "Kidney-L",
                   "Kidney-R", "Liver", "Lung-L", "Lung-R", "Pancreas", "Spleen"],
        "out": "organ_axial", "name": "CT Organ (Axial)",
    },
    "organc": {
        "data": "data/organcmnist.npz", "channels": 1, "binary": False,
        "labels": ["Bladder", "Femur-L", "Femur-R", "Heart", "Kidney-L",
                   "Kidney-R", "Liver", "Lung-L", "Lung-R", "Pancreas", "Spleen"],
        "out": "organ_coronal", "name": "CT Organ (Coronal)",
    },
    "organs": {
        "data": "data/organsmnist.npz", "channels": 1, "binary": False,
        "labels": ["Bladder", "Femur-L", "Femur-R", "Heart", "Kidney-L",
                   "Kidney-R", "Liver", "Lung-L", "Lung-R", "Pancreas", "Spleen"],
        "out": "organ_sagittal", "name": "CT Organ (Sagittal)",
    },
}


# MedMNIST+ (size>28) npz files are named by the source dataset, not the
# output model name used in this repo.
SOURCE_NAME = {
    "breast": "breastmnist", "derma": "dermamnist", "retina": "retinamnist",
    "oct": "octmnist", "blood": "bloodmnist", "path": "pathmnist",
    "chest": "chestmnist", "tissue": "tissuemnist",
    "organa": "organamnist", "organc": "organcmnist", "organs": "organsmnist",
    "pneumonia": "pneumoniamnist",
}


def data_path(dataset, size):
    """Resolve the .npz path for a dataset at a given input size.

    28 -> legacy data/<name>.npz, 224 -> data/224/<source>_224.npz (MedMNIST+).
    """
    if size == 28:
        return CONFIGS[dataset]["data"]
    return "data/%d/%s_%d.npz" % (size, SOURCE_NAME[dataset], size)


def load(dataset, binary, size=28):
    """Load a dataset. For size >= 128 returns uint8 arrays to keep RAM low;
    the caller must normalize per-batch (see batch_generator)."""
    d = np.load(data_path(dataset, size))
    x_tr, x_va, x_te = d["train_images"], d["val_images"], d["test_images"]
    if CONFIGS[dataset].get("multilabel"):
        y_tr = d["train_labels"].astype(np.int64)
        y_va = d["val_labels"].astype(np.int64)
        y_te = d["test_labels"].astype(np.int64)
    else:
        y_tr = d["train_labels"].astype(np.int64).ravel()
        y_va = d["val_labels"].astype(np.int64).ravel()
        y_te = d["test_labels"].astype(np.int64).ravel()
    if binary:
        pos_label = CONFIGS[dataset].get("pos_label", 1)
        if pos_label == "any":
            y_tr = (y_tr > 0).astype(np.int64)
            y_va = (y_va > 0).astype(np.int64)
            y_te = (y_te > 0).astype(np.int64)
        else:
            y_tr = (y_tr == pos_label).astype(np.int64)
            y_va = (y_va == pos_label).astype(np.int64)
            y_te = (y_te == pos_label).astype(np.int64)
    if size >= 128:
        # uint8 stays in RAM; float32 only per-batch
        return x_tr, x_va, x_te, y_tr, y_va, y_te
    # normalize to [0,1] (legacy small datasets)
    x_tr = x_tr.astype(np.float32) / 255.0
    x_va = x_va.astype(np.float32) / 255.0
    x_te = x_te.astype(np.float32) / 255.0
    return x_tr, x_va, x_te, y_tr, y_va, y_te


def batch_generator(x, y, batch, shuffle=True, channels=None):
    """Yield (normalized float32 batch, labels). x stays uint8."""
    n = len(x)
    idx = np.arange(n)
    while True:
        if shuffle:
            np.random.shuffle(idx)
        for i in range(0, n, batch):
            b = idx[i:i + batch]
            yield (x[b].astype(np.float32) / 255.0), y[b]


def predict_in_batches(model, x, batch, channels=None):
    """Predict in normalized batches to avoid loading float32 of large data."""
    n = len(x)
    out = None
    for i in range(0, n, batch):
        xb = x[i:i + batch].astype(np.float32) / 255.0
        p = model.predict(xb, verbose=0)
        if out is None:
            out = np.empty((n,) + p.shape[1:], dtype=np.float32)
        out[i:i + batch] = p
    return out


def build_model(channels, n_classes, scale=1.0, multilabel=False, input_size=28):
    c1 = max(8, int(32 * scale))
    c2 = max(16, int(64 * scale))
    c3 = max(32, int(128 * scale))
    inp = keras.Input((input_size, input_size, channels))
    x = layers.Conv2D(c1, 3, padding="same")(inp)
    x = layers.BatchNormalization()(x)
    x = layers.ReLU()(x)
    x = layers.MaxPooling2D(2)(x)
    x = layers.Conv2D(c2, 3, padding="same")(x)
    x = layers.BatchNormalization()(x)
    x = layers.ReLU()(x)
    x = layers.MaxPooling2D(2)(x)
    x = layers.Conv2D(c3, 3, padding="same")(x)
    x = layers.BatchNormalization()(x)
    x = layers.ReLU()(x)
    x = layers.GlobalAveragePooling2D()(x)
    x = layers.Dropout(0.3)(x)
    if n_classes == 1:
        out = layers.Dense(1, activation="sigmoid", name="disease_prob")(x)
    elif multilabel:
        out = layers.Dense(n_classes, activation="sigmoid", name="class_probs")(x)
    else:
        out = layers.Dense(n_classes, activation="softmax", name="class_probs")(x)
    return keras.Model(inp, out)


def calibrate_binary(pred, y):
    # sensitivity-first: threshold with recall >= 0.90, then highest F1
    thr = 0.5
    best = None
    for t in np.arange(0.01, 0.99, 0.01):
        r = recall_score(y, pred >= t)
        if r >= 0.90:
            f1 = f1_score(y, pred >= t)
            if best is None or f1 > best[1]:
                best = (t, f1)
    return best[0] if best else 0.5


def calibrate_multiclass(probs, y):
    # confidence gating: report accuracy & coverage as confidence rises
    conf = probs.max(axis=1)
    yp = probs.argmax(axis=1)
    grid = {}
    for t in [0.0, 0.3, 0.5, 0.7, 0.8, 0.9]:
        mask = conf >= t
        if mask.sum() > 0:
            grid[str(t)] = {
                "coverage": round(float(mask.mean()), 4),
                "accuracy": round(float(accuracy_score(y[mask], yp[mask])), 4),
            }
    return grid


def calibrate_multilabel(probs, y):
    # per-label confidence gating: coverage & macro-F1 as max-prob rises
    conf = probs.max(axis=1)
    grid = {}
    for t in [0.0, 0.3, 0.5, 0.7, 0.8, 0.9]:
        mask = conf >= t
        if mask.sum() > 0:
            yp = (probs[mask] >= 0.5).astype(np.int64)
            grid[str(t)] = {
                "coverage": round(float(mask.mean()), 4),
                "macro_f1": round(float(f1_score(y[mask], yp, average="macro", zero_division=0)), 4),
            }
    return grid


@register_keras_serializable(package="SwasthAI", name="WeightedBCE")
class WeightedBCE(keras.losses.Loss):
    def __init__(self, weights=None, **kwargs):
        super().__init__(**kwargs)
        self.weights = (
            np.asarray(weights, dtype=np.float32)
            if weights is not None else None
        )

    def call(self, y_true, y_pred):
        bce = keras.backend.binary_crossentropy(y_true, y_pred)
        if self.weights is not None:
            bce = bce * keras.backend.constant(self.weights, dtype="float32")
        return keras.backend.mean(bce)

    def get_config(self):
        cfg = super().get_config()
        cfg["weights"] = self.weights.tolist() if self.weights is not None else None
        return cfg


def make_multilabel_loss(y_pos):
    w = (y_pos.shape[0] / np.maximum(y_pos.sum(axis=0), 1)).astype(np.float32)
    w = w / w.mean()
    return WeightedBCE(weights=w)


def train(dataset, size=28):
    cfg = CONFIGS[dataset]
    t0 = time.time()
    log = lambda m: print("[%6.1fs] %s" % (time.time() - t0, m), flush=True)

    log("loading %s (size=%d)" % (dataset, size))
    x_tr, x_va, x_te, y_tr, y_va, y_te = load(dataset, cfg.get("binary", False), size=size)

    if cfg.get("multilabel"):
        pos = y_tr.sum(axis=0)
        log("multilabel n_labels=%d per-class pos=%s" % (len(cfg["labels"]), pos.tolist()))
        ml_loss = make_multilabel_loss(y_tr)
        cw = None
    elif cfg.get("binary"):
        # class weights to counter imbalance (sigmoid)
        pos = (y_tr == 1).sum()
        neg = (y_tr == 0).sum()
        cw = {0: 1.0, 1: neg / max(pos, 1)}
        log("binary pos=%d neg=%d cw=%s" % (pos, neg, cw))
        ml_loss = None
    else:
        cw = None
        ml_loss = None
        vals, counts = np.unique(y_tr, return_counts=True)
        log("class dist: %s" % dict(zip(vals.tolist(), counts.tolist())))

    n_out = 1 if cfg.get("binary") else len(cfg["labels"])
    model = build_model(cfg["channels"], n_out,
                        scale=0.5 if len(x_tr) < 2000 else 1.0,
                        multilabel=cfg.get("multilabel"),
                        input_size=size)

    if cfg.get("binary"):
        metrics = [keras.metrics.AUC(name="auc"), keras.metrics.Precision(name="prec"),
                   keras.metrics.Recall(name="rec"), "accuracy"]
    elif cfg.get("multilabel"):
        metrics = [keras.metrics.AUC(name="auc", multi_label=True, num_thresholds=100)]
    else:
        metrics = ["accuracy"]

    if cfg.get("multilabel"):
        loss = ml_loss
    elif cfg.get("binary"):
        loss = "binary_crossentropy"
    else:
        loss = "sparse_categorical_crossentropy"

    model.compile(optimizer=keras.optimizers.Adam(1e-3), loss=loss, metrics=metrics)

    cbs = [keras.callbacks.ReduceLROnPlateau(monitor="val_loss", patience=12, factor=0.5)]

    # large datasets: fewer epochs, bigger batch (CPU-bound); large images shrink batch
    n = len(x_tr)
    epochs = 30 if n > 50000 else 100
    batch = 128 if n > 50000 else 32
    if size >= 224:
        batch = max(8, batch // 4)
    elif size >= 128:
        batch = max(16, batch // 2)

    use_gen = size >= 128
    if use_gen:
        steps = int(np.ceil(n / batch))
        val_steps = int(np.ceil(len(x_va) / batch))
        model.fit(
            batch_generator(x_tr, y_tr, batch, channels=cfg["channels"]),
            steps_per_epoch=steps,
            epochs=epochs, validation_data=batch_generator(x_va, y_va, batch, shuffle=False, channels=cfg["channels"]),
            validation_steps=val_steps,
            verbose=0, callbacks=cbs,
        )
    else:
        model.fit(
            x_tr, y_tr, validation_data=(x_va, y_va),
            epochs=epochs, batch_size=batch, verbose=0,
            callbacks=cbs,
        )

    os.makedirs("models", exist_ok=True)
    tag = "" if size == 28 else "_%d" % size
    kpath = "models/%s%s_float.keras" % (cfg["out"], tag)
    model.save(kpath)
    log("saved %s (%.2f MB)" % (kpath, os.path.getsize(kpath) / 1e6))

    # ---- evaluation ----
    if cfg.get("binary"):
        if use_gen:
            p_tr = predict_in_batches(model, x_tr, batch).ravel()
            p_va = predict_in_batches(model, x_va, batch).ravel()
            p_te = predict_in_batches(model, x_te, batch).ravel()
        else:
            p_tr = model.predict(x_tr, verbose=0).ravel()
            p_va = model.predict(x_va, verbose=0).ravel()
            p_te = model.predict(x_te, verbose=0).ravel()
        thr = calibrate_binary(p_va, y_va)
        yp = p_te >= thr
        result = {
            "task": "binary", "threshold": float(thr),
            "auroc_test": float(roc_auc_score(y_te, p_te)),
            "sensitivity": float(recall_score(y_te, yp)),
            "precision": float(precision_score(y_te, yp)),
            "f1": float(f1_score(y_te, yp)),
            "accuracy": float(accuracy_score(y_te, yp)),
            "confusion_matrix": confusion_matrix(y_te, yp).tolist(),
            "prob_range": [float(p_te.min()), float(p_te.max())],
            "prob_std": float(p_te.std()),
        }
    elif cfg.get("multilabel"):
        if use_gen:
            probs = predict_in_batches(model, x_te, batch)
            pva = predict_in_batches(model, x_va, batch)
        else:
            probs = model.predict(x_te, verbose=0)
            pva = model.predict(x_va, verbose=0)
        per_label_auc = {}
        per_label_thr = {}
        for i, name in enumerate(cfg["labels"]):
            try:
                per_label_auc[name] = float(roc_auc_score(y_te[:, i], probs[:, i]))
            except ValueError:
                per_label_auc[name] = None
            best_t, best_f1 = 0.5, -1
            for t in np.arange(0.05, 0.99, 0.05):
                f = f1_score(y_va[:, i], pva[:, i] >= t, zero_division=0)
                if f > best_f1:
                    best_t, best_f1 = t, f
            per_label_thr[name] = float(best_t)
        thr_arr = np.array([per_label_thr[n] for n in cfg["labels"]])
        yp = (probs >= thr_arr).astype(np.int64)
        per_class_f1 = [float(x) for x in f1_score(y_te, yp, average=None, zero_division=0)]
        result = {
            "task": "multilabel", "n_labels": len(cfg["labels"]),
            "macro_auroc": float(np.mean([v for v in per_label_auc.values() if v is not None])),
            "per_label_auroc": per_label_auc,
            "per_label_thresholds": per_label_thr,
            "macro_f1": float(f1_score(y_te, yp, average="macro", zero_division=0)),
            "per_class_f1": per_class_f1,
            "per_label_precision": [float(x) for x in precision_score(y_te, yp, average=None, zero_division=0)],
            "per_label_recall": [float(x) for x in recall_score(y_te, yp, average=None, zero_division=0)],
            "pos_prev_test": [float(y_te[:, i].mean()) for i in range(len(cfg["labels"]))],
            "confidence_gating": calibrate_multilabel(probs, y_te),
        }
    else:
        probs = predict_in_batches(model, x_te, batch) if use_gen else model.predict(x_te, verbose=0)
        yp = probs.argmax(axis=1)
        # macro metrics
        result = {
            "task": "multiclass", "n_classes": len(cfg["labels"]),
            "accuracy": float(accuracy_score(y_te, yp)),
            "macro_precision": float(precision_score(y_te, yp, average="macro", zero_division=0)),
            "macro_recall": float(recall_score(y_te, yp, average="macro", zero_division=0)),
            "macro_f1": float(f1_score(y_te, yp, average="macro", zero_division=0)),
            "per_class_f1": [float(x) for x in f1_score(y_te, yp, average=None, zero_division=0)],
            "confusion_matrix": confusion_matrix(y_te, yp).tolist(),
            "confidence_gating": calibrate_multiclass(probs, y_te),
        }

    result["labels"] = cfg["labels"]
    result["name"] = cfg["name"]
    result["input_size"] = size
    mpath = "models/%s%s_metrics.json" % (cfg["out"], tag)
    with open(mpath, "w") as f:
        json.dump(result, f, indent=2)
    log("metrics -> %s" % mpath)
    print(json.dumps(result, indent=2))
    log("done in %.1fs" % (time.time() - t0))


if __name__ == "__main__":
    size = int(sys.argv[2]) if len(sys.argv) > 2 else 28
    train(sys.argv[1], size=size)
