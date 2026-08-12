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

IMG_SIZE = 128
BATCH = 64
HEAD_EPOCHS = 10
FT_EPOCHS = 15
CLASS_NAMES = ["Normal", "Pneumonia"]

t0 = time.time()


def log(msg):
    print(f"[{time.time()-t0:7.1f}s] {msg}", flush=True)


log("loading data")
data = np.load("data/pneumoniamnist.npz")
x_train = data["train_images"]
y_train = data["train_labels"].ravel()
x_val = data["val_images"]
y_val = data["val_labels"].ravel()
x_test = data["test_images"]
y_test = data["test_labels"].ravel()
log(f"train={x_train.shape} val={x_val.shape} test={x_test.shape}")
log(f"positive rate train={y_train.mean():.3f} test={y_test.mean():.3f}")

# NOTE: pneumonia is the MAJORITY class here (74%). No inverse-frequency
# weights — we WANT the model biased toward the disease for high sensitivity.
class_weight = None

# model: MobileNetV3-Small backbone (ImageNet init) + classification head.
# Input pipeline resizes the 28x28 grayscale DICOM-derived X-rays to 128x128 RGB.
inputs = keras.Input((28, 28, 1), name="chest_xray")
x = layers.Resizing(IMG_SIZE, IMG_SIZE)(inputs)
x = layers.Rescaling(1 / 255.0)(x)
x = layers.Concatenate(axis=-1)([x, x, x])

base = keras.applications.MobileNetV3Small(
    include_top=False,
    weights="imagenet",
    input_shape=(IMG_SIZE, IMG_SIZE, 3),
    pooling="avg",
)
base.trainable = False

x = base(x)
x = layers.Dropout(0.3)(x)
out = layers.Dense(1, activation="sigmoid", name="pneumonia_prob")(x)

model = keras.Model(inputs, out)
model.summary()
log(f"total params: {model.count_params()/1e6:.2f}M")

model.compile(
    optimizer=keras.optimizers.Adam(HEAD_LR := 1e-3),
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
        monitor="val_auc", mode="max", patience=5, restore_best_weights=True
    ),
    keras.callbacks.ReduceLROnPlateau(patience=3, factor=0.5, verbose=0),
]

log("phase 1: training head on frozen backbone")
model.fit(
    x_train,
    y_train,
    validation_data=(x_val, y_val),
    batch_size=BATCH,
    epochs=HEAD_EPOCHS,
    class_weight=class_weight,
    callbacks=callbacks,
    verbose=1,
)

log("phase 2: fine-tuning last 15 layers")
for layer in base.layers[-15:]:
    layer.trainable = True
model.compile(
    optimizer=keras.optimizers.Adam(1e-4),
    loss="binary_crossentropy",
    metrics=[
        "accuracy",
        keras.metrics.Precision(name="precision"),
        keras.metrics.Recall(name="recall"),
        keras.metrics.AUC(name="auc"),
    ],
)
model.fit(
    x_train,
    y_train,
    validation_data=(x_val, y_val),
    batch_size=BATCH,
    epochs=FT_EPOCHS,
    class_weight=class_weight,
    callbacks=callbacks,
    verbose=1,
)

log("saving float model")
model.save("models/pneumonia_mobilenetv3_float.keras")

log("calibrating operating threshold on validation set (recall-first)")
val_prob = model.predict(x_val, batch_size=64, verbose=0).ravel()
thresholds = np.arange(0.10, 0.95, 0.05)
best_t, best_f1, best_recall = 0.5, 0.0, 0.0
for t in thresholds:
    r = recall_score(y_val, (val_prob >= t).astype(int))
    f1 = f1_score(y_val, (val_prob >= t).astype(int))
    if r >= 0.90 and f1 > best_f1:  # high-recall operating point
        best_t, best_f1, best_recall = float(t), f1, r
if best_t == 0.5:
    best_t = 0.5  # fallback
log(f"chosen threshold={best_t} (val recall={best_recall:.3f}, f1={best_f1:.3f})")

log("evaluating on held-out test set")
y_prob = model.predict(x_test, batch_size=64, verbose=0).ravel()
y_pred = (y_prob >= best_t).astype(int)

results = {
    "model": "mobilenet_v3_small_ft",
    "input": (28, 28, 1),
    "resize": IMG_SIZE,
    "classes": CLASS_NAMES,
    "threshold": best_t,
    "accuracy": float(accuracy_score(y_test, y_pred)),
    "precision": float(precision_score(y_test, y_pred, zero_division=0)),
    "recall_sensitivity": float(recall_score(y_test, y_pred, zero_division=0)),
    "f1": float(f1_score(y_test, y_pred, zero_division=0)),
    "auroc": float(roc_auc_score(y_test, y_prob)),
    "confusion_matrix": confusion_matrix(y_test, y_pred).tolist(),
    "train_time_s": round(time.time() - t0, 1),
}
print(json.dumps(results, indent=2))
with open("models/pneumonia_metrics.json", "w") as f:
    json.dump(results, f, indent=2)
log("done")
