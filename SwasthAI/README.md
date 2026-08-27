# SwasthAI

## Offline Edge AI-Powered Medical Diagnosis & Rural Healthcare Platform

SwasthAI is a production-ready Android application that provides AI-assisted medical diagnosis, disease prediction, risk assessment, and clinical decision support for rural communities. It operates offline-first, leveraging on-device TensorFlow Lite models to enable diagnostics in areas with limited or no internet connectivity.

---

## 🛠️ Tech Stack

### 📱 Android Application (SwasthAI)
* **Language:** [Kotlin](https://kotlinlang.org/) (modern, type-safe language optimized for Android)
* **UI Framework:** [Jetpack Compose](https://developer.android.com/compose) (declarative UI toolkit) with **Material Design 3**
* **Architecture:** MVVM (Model-View-ViewModel) + Clean Architecture + Repository Pattern
* **On-Device Edge AI Inference:** [TensorFlow Lite (TFLite)](https://www.tensorflow.org/lite) with GPU acceleration and support libraries
* **Local Database & Security:** [Room](https://developer.android.com/training/data-storage/room) ORM encrypted via [SQLCipher](https://www.zetetic.net/sqlcipher/) for HIPAA/GDPR-compliant local data security
* **Local Settings & Preferences:** [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore) (Preferences)
* **Dependency Injection:** [Dagger-Hilt](https://developer.android.com/training/dependency-injection/hilt-android) (including Hilt Integration for WorkManager and Compose Navigation)
* **Asynchronous / Background Tasks:** [Kotlin Coroutines & Flow](https://kotlinlang.org/docs/coroutines-overview.html) + [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) for robust, deferrable background synchronization
* **Networking:** [Retrofit 2](https://square.github.io/retrofit/) + [OkHttp 3](https://square.github.io/okhttp/) (with Logging Interceptor) + [Moshi](https://github.com/square/moshi) for JSON serialization
* **Image Loading:** [Coil](https://coil-kt.github.io/coil/) (Kotlin Image Loading library)
* **Other Utilities:** Google Fonts, Accompanist (Permissions & System UI Controller), Android Splash Screen API

### 🧠 Machine Learning Engine (ml)
* **Framework:** [TensorFlow 2.x](https://www.tensorflow.org/)/[Keras](https://keras.io/)
* **Languages & Libraries:** Python 3, NumPy, Scikit-learn
* **Datasets:** MedMNIST (BreastMNIST, RetinaMNIST, DermaMNIST, OctMNIST, BloodMNIST, PathMNIST, ChestMNIST, TissueMNIST, OrganAMNIST/OrganCMNIST/OrganSMNIST)
* **Model Formats:** Keras SavedModel format (`.h5`/`.keras`) converted to optimized flatbuffers (`.tflite`) with optional float16 or dynamic range quantization.

### ⚙️ Backend (backend)
* **Framework:** [FastAPI](https://fastapi.tiangolo.com/) (modern, fast, high-performance web framework for building APIs)
* **Database / ORM:** SQLite with [SQLAlchemy](https://www.sqlalchemy.org/)
* **Validation:** [Pydantic v2](https://docs.pydantic.dev/) for data schema enforcement and serialization

---

## 🚀 Getting Started & How to Run

### 1. 📱 Running the Android App (SwasthAI)

#### Prerequisites
* **Android Studio:** Ladybug (2024.2.1) or later recommended.
* **JDK:** Java Development Kit 17.
* **Device / Emulator:** Android 8.0 (API level 26) or higher.

#### Steps to Run
1. **Clone the Repository** and open the `SwasthAI` folder in Android Studio.
2. **Sync Gradle:** Allow Android Studio to download dependencies and sync the build configuration (`build.gradle.kts`).
3. **Build the Project:** Select `Build` > `Make Project` in the menu.
4. **Choose a Target:** Connect a physical Android device via USB debugging or start an Android Virtual Device (AVD) emulator.
5. **Run the App:** Click the green **Run** button (`Shift + F10`) in Android Studio.

---

### 2. 🧠 Running the Machine Learning Pipeline (ml)

The Python scripts in the `ml/` directory train medical diagnostic models and compile them into `.tflite` format for use in the Android app.

#### Prerequisites
* **Python:** 3.9 to 3.11 recommended.
* Install dependencies:
  ```bash
  pip install tensorflow numpy scikit-learn
  ```
* Place the required MedMNIST datasets (e.g., `pneumoniamnist.npz`, `breastmnist.npz`, etc.) inside the `ml/data/` directory.

#### Training Models
To train a model for a specific diagnostic category (e.g., Pneumonia classification or Skin Lesion detection):
```bash
# General training command (e.g., 28x28 input resolution):
python train_screening.py <dataset_key> [size]

# Example: Train Retina DR model at 28x28
python train_screening.py retina 28

# Example: Train Pneumonia model at 224x224 (for higher-res camera inputs)
python train_screening.py pneumonia 224
```
*Available dataset keys:* `breast`, `pneumonia`, `derma`, `retina`, `oct`, `blood`, `path`, `chest`.

Alternatively, to train the lightweight CNN model specifically for Pneumonia:
```bash
python train_cnn.py
```

#### Converting to TensorFlow Lite (TFLite)
Once training is complete, convert models into optimized `.tflite` files:
```bash
# Convert a specific model
python convert_tflite.py

# Convert all trained models in one command
python convert_all.py
```
This outputs optimized flatbuffer files (e.g. `retina_dr.tflite`, `chest_xray.tflite`) under the `ml/models/` directory. Copy these outputs into the Android app's `assets/` folder to deploy updates.

---

### 3. ⚙️ Running the Python FastAPI Backend (backend)

The FastAPI server provides backend endpoints for user authentication, remote patient management, diagnostic logs upload, and batch synchronization.

#### Prerequisites
* **Python:** 3.9+
* Install backend dependencies:
  ```bash
  pip install -r backend/requirements.txt
  ```

#### Running the Server
From the root of the project, start the Uvicorn development server:
```bash
python -m uvicorn backend.main:app --reload --port 8000
```

Once running, you can access:
* **Interactive API Documentation (Swagger UI):** [http://127.0.0.1:8000/docs](http://127.0.0.1:8000/docs)
* **Local Backend URL:** `http://127.0.0.1:8000/api/v1` (which matches the Android emulator's network bridge alias `http://10.0.2.2:8000/api/v1/`)


