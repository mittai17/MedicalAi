---
trigger: always_on
---

# Project Execution and Runner Guidelines

1. **Do NOT Use Waydroid**:
   - Never start, connect to, or deploy into Waydroid unless the user explicitly commands it in the current prompt.

2. **Backend Execution**:
   - To run the SwasthAI FastAPI backend service on the host:
     - Python interpreter / venv: `/home/z/Desktop/MedicalAi/.venv/bin/python` or local venv.
     - Command: `/home/z/Desktop/MedicalAi/.venv/bin/uvicorn backend.main:app --host 0.0.0.0 --port 8000 --reload`
     - Health check: `curl http://localhost:8000/docs` or `http://localhost:8000/api/v1/patients`.

3. **Android Client Verification**:
   - Set environment:
     - `export JAVA_HOME=/home/z/.jdks/jdk-17.0.10+7`
     - `export ANDROID_HOME=$HOME/Android/Sdk`
     - `export PATH=$JAVA_HOME/bin:$PATH:$HOME/Android/Sdk/platform-tools`
   - Run unit tests: `./gradlew testDebugUnitTest` (in `SwasthAI/`).
   - Build debug APK: `./gradlew assembleDebug` (in `SwasthAI/`).
