import os
os.environ.setdefault("TF_CPP_MIN_LOG_LEVEL", "2")

import json
import time

import numpy as np
import tensorflow as tf
from sklearn.metrics import (
    accuracy_score,
    confusion_matrix,
    f1_score,
    precision_score,
    recall_score,
    roc_auc_score,
)
from tensorflow import keras
from tensorflow.keras import layers

SEED = 42
tf.random.set_seed(SEED)
np.random.seed(SEED)

BATCH = 64
EPOCHS = 25
CLASS_NAMES = ["Normal", "Pneumonia"]

t0 = time.time()


def log(msg):
    print(f"[{time.time()-t0:7.1f}s] {msg}", flush=True)


log("loading data")
data = np.load("data/pneumoniamnist.npz")
x_train = data["train_images"].astype(np.float32) / 255.0
y_train = data["train_labels"].ravel()
x_val = data["val_images"].astype(np.float32) / 255.0
y_val = data["val_labels"].ravel()
x_test = data["test_images"].astype(np.float32) / 255.0
y_test = data["test_labels"].ravel()
log(f"train={x_train.shape} val={x_val.shape} test={x_test.shape}")

inputs = keras.Input((28, 28, 1))
x = layers.Conv2D(32, 3, padding="same")(inputs)
x = layers.BatchNormalization()(x)
x = layers.ReLU()(x)
x = layers.MaxPooling2D(2)(x)
x = layers.Conv2D(64, 3, padding="same")(x)
x = layers.BatchNormalization()(x)
x = layers.ReLU()(x)
x = layers.MaxPooling2D(2)(x)
x = layers.Conv2D(128, 3, padding="same")(x)
x = layers.BatchNormalization()(x)
x = layers.ReLU()(x)
x = layers.GlobalAveragePooling2D()(x)
x = layers.Dropout(0.3)(x)
out = layers.Dense(1, activation="sigmoid", name="pneumonia_prob")(x)

model = keras.Model(inputs, out)
model.summary()
log(f"total params: {model.count_params()/1e6:.2f}M")

model.compile(
    optimizer=keras.optimizers.Adam(1e-3),
    loss="binary_crossentropy",
    metrics=[
        "accuracy",
        keras.metrics.Precision(name="precision"),
        keras.metrics.Recall(name="recall"),
        keras.metrics.AUC(name="auc"),
    ],
)

callbacks = [
    keras.callbacks.EarlyStopping(
        monitor="val_auc", mode="max", patience=6, restore_best_weights=True
    ),
    keras.callbacks.ReduceLROnPlateau(patience=3, factor=0.5, verbose=0),
]

model.fit(
    x_train,
    y_train,
    validation_data=(x_val, y_val),
    batch_size=BATCH,
    epochs=EPOCHS,
    callbacks=callbacks,
    verbose=1,
)

log("saving float model")
model.save("models/pneumonia_cnn_float.keras")

log("calibrating operating threshold on validation set (recall-first)")
val_prob = model.predict(x_val, batch_size=64, verbose=0).ravel()
thresholds = np.arange(0.10, 0.95, 0.05)
best_t, best_f1, best_recall = 0.5, 0.0, 0.0
for t in thresholds:
    r = recall_score(y_val, (val_prob >= t).astype(int))
    f1 = f1_score(y_val, (val_prob >= t).astype(int))
    if r >= 0.90 and f1 > best_f1:
        best_t, best_f1, best_recall = float(t), f1, r
log(f"chosen threshold={best_t} (val recall={best_recall:.3f}, f1={best_f1:.3f})")

log("evaluating on held-out test set")
y_prob = model.predict(x_test, batch_size=64, verbose=0).ravel()
y_pred = (y_prob >= best_t).astype(int)

results = {
    "model": "pneumonia_cnn_scratch",
    "input": (28, 28, 1),
    "classes": CLASS_NAMES,
    "threshold": best_t,
    "accuracy": float(accuracy_score(y_test, y_pred)),
    "precision": float(precision_score(y_test, y_pred, zero_division=0)),
    "recall_sensitivity": float(recall_score(y_test, y_pred, zero_division=0)),
    "f1": float(f1_score(y_test, y_pred, zero_division=0)),
    "auroc": float(roc_auc_score(y_test, y_prob)),
    "confusion_matrix": confusion_matrix(y_test, y_pred).tolist(),
    "prob_stats": {
        "mean": float(y_prob.mean()),
        "std": float(y_prob.std()),
        "min": float(y_prob.min()),
        "max": float(y_prob.max()),
    },
    "train_time_s": round(time.time() - t0, 1),
}
print(json.dumps(results, indent=2))
with open("models/pneumonia_cnn_metrics.json", "w") as f:
    json.dump(results, f, indent=2)
log("done")
