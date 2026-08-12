import os
os.environ.setdefault("TF_CPP_MIN_LOG_LEVEL", "3")
import numpy as np
import tensorflow as tf
from tensorflow import keras
from sklearn.metrics import roc_auc_score, accuracy_score, precision_score, recall_score, confusion_matrix

np.set_printoptions(precision=4, suppress=True)

data = np.load("data/pneumoniamnist.npz")
x_test = data["test_images"]  # uint8 [0,255]
y_test = data["test_labels"].ravel()
x_norm = x_test.astype(np.float32) / 255.0  # [0,1] float

model = keras.models.load_model("models/pneumonia_cnn_float.keras")
k = model.predict(x_norm, batch_size=64, verbose=0).ravel()
print("keras float AUROC:", roc_auc_score(y_test, k))


def predict_with(path, x, input_is_uint8):
    interp = tf.lite.Interpreter(
        model_path=path,
        experimental_op_resolver_type=tf.lite.experimental.OpResolverType.BUILTIN_WITHOUT_DEFAULT_DELEGATES,
    )
    interp.allocate_tensors()
    in_det = interp.get_input_details()[0]
    out_det = interp.get_output_details()[0]
    print(path, "input dtype:", in_det["dtype"], "quant:", in_det["quantization"])
    probs = np.zeros(len(x), dtype=np.float32)
    for i in range(len(x)):
        img = x[i].reshape(1, 28, 28, 1)
        if in_det["dtype"] == np.uint8:
            scale = in_det["quantization_parameters"]["scales"][0]
            zero = in_det["quantization_parameters"]["zero_points"][0]
            img = (img.astype(np.float32) / scale + zero).astype(np.uint8)
        else:
            img = img.astype(np.float32)
        interp.set_tensor(in_det["index"], img)
        interp.invoke()
        out = interp.get_tensor(out_det["index"]).ravel()
        if out_det["dtype"] == np.uint8:
            scale = out_det["quantization_parameters"]["scales"][0]
            zero = out_det["quantization_parameters"]["zero_points"][0]
            out = (out.astype(np.float32) - zero) * scale
        probs[i] = out[0]
    return probs


# INT8: feed raw uint8 pixels (quant maps to [0,1]); FP16: feed normalized float [0,1]
t8 = predict_with("models/mobilenet_v3.tflite", x_test, input_is_uint8=True)
t16 = predict_with("models/mobilenet_v3_fp16.tflite", x_norm, input_is_uint8=False)

for name, p in [("INT8", t8), ("FP16", t16)]:
    thr = 0.85
    yp = (p >= thr).astype(int)
    print(f"{name}: AUROC={roc_auc_score(y_test, p):.4f} "
          f"recall={recall_score(y_test, yp):.4f} prec={precision_score(y_test, yp):.4f} "
          f"acc={accuracy_score(y_test, yp):.4f} CM={confusion_matrix(y_test, yp).tolist()}")
    print(f"  vs float: maxdiff={np.abs(p-k).max():.5f} mean={np.abs(p-k).mean():.5f} "
          f"flips={(p>=thr)!=(k>=thr)}.sum()")

print("prob range:", k.min(), k.max(), "std:", k.std())
