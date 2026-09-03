//
// SwasthAI native AI kernels (hybrid Kotlin + C++/NDK).
//
// Ports the lightweight lexical scoring hot loop of LocalRagRetriever into
// native C++ so the on-device RAG search runs fast even on low-end phones.
// The Kotlin side stays the source of truth for normalization; this kernel
// only counts tokens and computes overlap. If anything is malformed it
// returns -1.0 so the caller falls back to the Kotlin implementation.
//

#include <jni.h>
#include <string>
#include <vector>
#include <unordered_map>
#include <algorithm>
#include <android/log.h>

#define LOG_TAG "SwasthAiNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

namespace {

// Copies a JNI String[] into std::vector<std::string>.
std::vector<std::string> jstringArrayToVector(JNIEnv* env, jobjectArray array) {
    std::vector<std::string> out;
    if (array == nullptr) return out;
    const jsize length = env->GetArrayLength(array);
    if (length <= 0) return out;
    out.reserve(static_cast<size_t>(length));
    for (jsize i = 0; i < length; ++i) {
        auto element = static_cast<jstring>(env->GetObjectArrayElement(array, i));
        if (element == nullptr) continue;
        const char* chars = env->GetStringUTFChars(element, nullptr);
        if (chars != nullptr) {
            out.emplace_back(chars);
            env->ReleaseStringUTFChars(element, chars);
        }
        env->DeleteLocalRef(element);
    }
    return out;
}

}  // namespace

extern "C" JNIEXPORT jfloat JNICALL
Java_com_swasthai_app_ai_engine_AiNative_nativeDocScore(
    JNIEnv* env,
    jobject /* thiz */,
    jobjectArray queryTerms,
    jobjectArray docTokens) {
    try {
        const std::vector<std::string> query = jstringArrayToVector(env, queryTerms);
        const std::vector<std::string> doc = jstringArrayToVector(env, docTokens);
        if (query.empty() || doc.empty()) return -1.0f;

        // Term frequency for the document.
        std::unordered_map<std::string, int> freq;
        freq.reserve(doc.size());
        for (const std::string& token : doc) {
            ++freq[token];
        }

        // Frequency-weighted overlap, mirrored from LocalRagRetriever:
        // per query term present in the doc add (1 + min(docFreq, 3)).
        double overlap = 0.0;
        for (const std::string& term : query) {
            const auto it = freq.find(term);
            if (it != freq.end()) {
                overlap += 1.0 + std::min(it->second, 3);
            }
        }

        // score = overlap / (queryTerms + 0.1 * docTokens) — identical to the
        // Kotlin fallback so results never drift between the two paths.
        const double denominator =
            static_cast<double>(query.size()) + 0.1 * static_cast<double>(doc.size());
        const double score = denominator > 0.0 ? overlap / denominator : 0.0;
        LOGI("nativeDocScore: query=%zu doc=%zu overlap=%.2f score=%.4f",
             query.size(), doc.size(), overlap, score);
        return static_cast<jfloat>(score);
    } catch (const std::exception& e) {
        LOGW("nativeDocScore failed: %s", e.what());
        return -1.0f;
    } catch (...) {
        LOGW("nativeDocScore failed: unknown error");
        return -1.0f;
    }
}