#include <jni.h>
#include <whisper.h>
#include <algorithm>
#include <atomic>
#include <cmath>
#include <cstdint>
#include <memory>
#include <mutex>
#include <string>
#include <unordered_map>
#include <vector>

namespace {
struct Engine {
    whisper_context * context;
    std::mutex inference;
    std::atomic<int64_t> generation{0};
    std::atomic<bool> closed{false};
    explicit Engine(whisper_context * value) : context(value) {}
    ~Engine() { whisper_free(context); }
};
struct AbortRequest { Engine * engine; int64_t generation; };
std::mutex registry_mutex;
std::unordered_map<int64_t, std::shared_ptr<Engine>> registry;
std::atomic<int64_t> next_id{1};

void throw_java(JNIEnv * env, const char * type, const char * message) {
    if (!env->ExceptionCheck()) env->ThrowNew(env->FindClass(type), message);
}

std::shared_ptr<Engine> lookup(int64_t id) {
    std::lock_guard<std::mutex> lock(registry_mutex);
    auto found = registry.find(id);
    return found == registry.end() ? nullptr : found->second;
}

bool aborted(void * data) {
    auto * request = static_cast<AbortRequest *>(data);
    return request->engine->closed.load() || request->engine->generation.load() != request->generation;
}

// Neither model paths nor recognized speech are logged by Nuraya's native engine.
void quiet_log(enum ggml_log_level, const char *, void *) {}

jbyteArray bytes(JNIEnv * env, const std::string & value) {
    auto result = env->NewByteArray(static_cast<jsize>(value.size()));
    if (result != nullptr && !value.empty()) {
        env->SetByteArrayRegion(result, 0, static_cast<jsize>(value.size()),
                                reinterpret_cast<const jbyte *>(value.data()));
    }
    return result;
}
} // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_nuraya_app_recitation_ai_QuranWhisperEngine_nativeCreate(JNIEnv * env, jobject, jstring path) {
    try {
        const char * model_path = env->GetStringUTFChars(path, nullptr);
        if (model_path == nullptr) return 0;
        std::string model(model_path);
        env->ReleaseStringUTFChars(path, model_path);
        whisper_log_set(quiet_log, nullptr);
        auto params = whisper_context_default_params();
        params.use_gpu = false;
        auto * context = whisper_init_from_file_with_params(model.c_str(), params);
        if (context == nullptr) {
            throw_java(env, "java/io/IOException", "Unable to load the Quran speech model");
            return 0;
        }
        auto engine = std::make_shared<Engine>(context);
        auto id = next_id.fetch_add(1);
        std::lock_guard<std::mutex> lock(registry_mutex);
        registry.emplace(id, std::move(engine));
        return id;
    } catch (const std::bad_alloc &) {
        throw_java(env, "java/io/IOException", "Not enough memory for the Quran speech model");
    } catch (...) {
        throw_java(env, "java/io/IOException", "Unable to initialize the Quran speech model");
    }
    return 0;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_nuraya_app_recitation_ai_QuranWhisperEngine_nativeGeneration(JNIEnv *, jobject, jlong id) {
    auto engine = lookup(id);
    return engine ? engine->generation.load() : -1;
}

extern "C" JNIEXPORT void JNICALL
Java_com_nuraya_app_recitation_ai_QuranWhisperEngine_nativeCancel(JNIEnv *, jobject, jlong id) {
    if (auto engine = lookup(id)) engine->generation.fetch_add(1);
}

extern "C" JNIEXPORT void JNICALL
Java_com_nuraya_app_recitation_ai_QuranWhisperEngine_nativeRelease(JNIEnv *, jobject, jlong id) {
    std::shared_ptr<Engine> engine;
    {
        std::lock_guard<std::mutex> lock(registry_mutex);
        auto found = registry.find(id);
        if (found == registry.end()) return;
        engine = found->second;
        registry.erase(found);
    }
    engine->closed.store(true);
    engine->generation.fetch_add(1);
    // A running inference owns another shared_ptr. Its context is freed only after it exits.
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_nuraya_app_recitation_ai_QuranWhisperEngine_nativeTranscribe(
        JNIEnv * env, jobject, jlong id, jfloatArray input, jlong generation, jint threads) {
    auto engine = lookup(id);
    if (!engine) {
        throw_java(env, "java/lang/IllegalStateException", "Quran speech engine is closed");
        return nullptr;
    }
    try {
        std::lock_guard<std::mutex> inference_lock(engine->inference);
        AbortRequest request{engine.get(), generation};
        if (aborted(&request)) {
            throw_java(env, "java/util/concurrent/CancellationException", "Quran speech recognition cancelled");
            return nullptr;
        }
        const jsize count = env->GetArrayLength(input);
        if (count < 1600 || count > 16000 * 90) {
            throw_java(env, "java/lang/IllegalArgumentException", "Expected 0.1 to 90 seconds of 16 kHz mono PCM");
            return nullptr;
        }
        std::vector<float> samples(static_cast<size_t>(count));
        env->GetFloatArrayRegion(input, 0, count, samples.data());
        if (env->ExceptionCheck()) return nullptr;
        double energy = 0.0;
        float peak = 0.0f;
        for (float value : samples) {
            if (!std::isfinite(value) || value < -1.0f || value > 1.0f) {
                throw_java(env, "java/lang/IllegalArgumentException", "PCM samples must be finite values between -1 and 1");
                return nullptr;
            }
            energy += value * value;
            peak = std::max(peak, std::abs(value));
        }
        // Avoid treating digital silence as speech. This is not a pronunciation score.
        if (peak < 0.01f && std::sqrt(energy / count) < 0.001) return bytes(env, "");
        auto params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
        params.n_threads = std::clamp(static_cast<int>(threads), 1, 4);
        params.language = "ar";
        params.translate = false;
        params.detect_language = false;
        params.no_context = true;
        params.no_timestamps = true;
        params.print_special = false;
        params.print_progress = false;
        params.print_realtime = false;
        params.print_timestamps = false;
        params.suppress_nst = true;
        params.temperature = 0.0f;
        params.temperature_inc = 0.0f;
        params.greedy.best_of = 1;
        params.initial_prompt = nullptr;
        params.prompt_tokens = nullptr;
        params.prompt_n_tokens = 0;
        params.abort_callback = aborted;
        params.abort_callback_user_data = &request;
        const int status = whisper_full(engine->context, params, samples.data(), count);
        if (aborted(&request)) {
            throw_java(env, "java/util/concurrent/CancellationException", "Quran speech recognition cancelled");
            return nullptr;
        }
        if (status != 0) {
            throw_java(env, "java/io/IOException", "Quran speech recognition did not complete");
            return nullptr;
        }
        std::string recognized;
        for (int i = 0; i < whisper_full_n_segments(engine->context); ++i) {
            const char * segment = whisper_full_get_segment_text(engine->context, i);
            if (segment != nullptr) recognized += segment;
        }
        return bytes(env, recognized);
    } catch (const std::bad_alloc &) {
        throw_java(env, "java/io/IOException", "Not enough memory to recognize this recording");
    } catch (...) {
        throw_java(env, "java/io/IOException", "Quran speech recognition failed");
    }
    return nullptr;
}

