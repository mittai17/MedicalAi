import os
os.environ.setdefault("TF_CPP_MIN_LOG_LEVEL", "2")

import json
import time

import numpy as np
from sklearn.metrics import (
    accuracy_score,
    confusion_matrix,
    f1_score,
    precision_score,
    recall_score,
    roc_auc_score,
)
from tensorflow import keras
import tensorflow as tf

t0 = time.time()


def log(msg):
    print(f"[{time.time()-t0:7.1f}s] {msg}", flush=True)


log("loading float model and data")
model = keras.models.load_model("models/pneumonia_cnn_float.keras")
data = np.load("data/pneumoniamnist.npz")
x_test = data["test_images"]
y_test = data["test_labels"].ravel()
with open("models/pneumonia_cnn_metrics.json") as f:
    metrics = json.load(f)
threshold = metrics["threshold"]
log(f"threshold={threshold}")

inputs = keras.Input((28, 28, 1))
outputs = model(inputs)
keras_model = keras.Model(inputs, outputs)

log("converting INT8 (full integer, representative dataset)")
def representative():
    for img in data["train_images"][:200]:
        yield [img.reshape(1, 28, 28, 1).astype(np.float32) / 255.0]

converter8 = tf.lite.TFLiteConverter.from_keras_model(keras_model)
converter8.optimizations = [tf.lite.Optimize.DEFAULT]
converter8.representative_dataset = representative
converter8.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
converter8.inference_input_type = tf.uint8
converter8.inference_output_type = tf.uint8
tflite8 = converter8.convert()
with open("models/mobilenet_v3.tflite", "wb") as f:
    f.write(tflite8)
log(f"INT8 size: {len(tflite8)/1e6:.2f} MB")

log("converting FP16")
converter16 = tf.lite.TFLiteConverter.from_keras_model(keras_model)
converter16.optimizations = [tf.lite.Optimize.DEFAULT]
tflite16 = converter16.convert()
with open("models/mobilenet_v3_fp16.tflite", "wb") as f:
    f.write(tflite16)
log(f"FP16 size: {len(tflite16)/1e6:.2f} MB")

log("float model size: %.2f MB" % (os.path.getsize("models/pneumonia_cnn_float.keras") / 1e6))


def predict_with(path, x):
    interpreter = tf.lite.Interpreter(
        model_path=path,
        experimental_op_resolver_type=tf.lite.experimental.OpResolverType.BUILTIN_WITHOUT_DEFAULT_DELEGATES,
    )
    interpreter.allocate_tensors()
    in_det = interpreter.get_input_details()[0]
    out_det = interpreter.get_output_details()[0]
    n = len(x)
    probs = np.zeros(n, dtype=np.float32)
    for i in range(n):
        img = x[i].reshape(1, 28, 28, 1).astype(np.float32)
        if in_det["dtype"] == np.uint8:
            scale = in_det["quantization_parameters"]["scales"][0]
            zero = in_det["quantization_parameters"]["zero_points"][0]
            img = (img / scale + zero).astype(np.uint8)
        interpreter.set_tensor(in_det["index"], img)
        interpreter.invoke()
        out = interpreter.get_tensor(out_det["index"]).ravel()
        if out_det["dtype"] == np.uint8:
            scale = out_det["quantization_parameters"]["scales"][0]
            zero = out_det["quantization_parameters"]["zero_points"][0]
            out = (out.astype(np.float32) - zero) * scale
        probs[i] = out[0]
    return probs


def report(name, y_prob):
    y_pred = (y_prob >= threshold).astype(int)
    print(f"--- {name} ---")
    print(
        json.dumps(
            {
                "accuracy": float(accuracy_score(y_test, y_pred)),
                "precision": float(precision_score(y_test, y_pred, zero_division=0)),
                "recall_sensitivity": float(recall_score(y_test, y_pred, zero_division=0)),
                "f1": float(f1_score(y_test, y_pred, zero_division=0)),
                "auroc": float(roc_auc_score(y_test, y_prob)),
                "confusion_matrix": confusion_matrix(y_test, y_pred).tolist(),
            },
            indent=2,
        )
    )
    return y_prob


log("verifying on test set")
keras_prob = keras_model.predict(x_test, batch_size=64, verbose=0).ravel()
keras_report = report("Keras float (baseline)", keras_prob)

for path, name in [("models/mobilenet_v3.tflite", "TFLite INT8"), ("models/mobilenet_v3_fp16.tflite", "TFLite FP16")]:
    tflite_prob = predict_with(path, x_test)
    report(name, tflite_prob)
    diff = np.abs(tflite_prob - keras_prob)
    flipped = int(np.sum((tflite_prob >= threshold) != (keras_prob >= threshold)))
    print(f"  max|pred diff| vs float: {diff.max():.4f}, mean: {diff.mean():.4f}, label flips: {flipped}/{len(y_test)}")

log("done")
