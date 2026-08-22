#include <jni.h>
#include <android/log.h>
#include <algorithm>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

#include "llama.h"

#define LOG_TAG "AprendIA-LLM"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {
constexpr int kMaxGeneratedTokens = 96;
constexpr int kContextPadding = 16;

std::mutex g_mutex;
llama_model * g_model = nullptr;
std::string g_model_path;

void throw_illegal_state(JNIEnv * env, const std::string & message) {
    jclass exception_class = env->FindClass("java/lang/IllegalStateException");
    if (exception_class != nullptr) {
        env->ThrowNew(exception_class, message.c_str());
    }
}

bool ensure_model_loaded(const std::string & model_path) {
    if (g_model != nullptr && g_model_path == model_path) {
        return true;
    }

    if (g_model != nullptr) {
        llama_model_free(g_model);
        g_model = nullptr;
        g_model_path.clear();
    }

    ggml_backend_load_all();
    LOGI("Loading model: %s", model_path.c_str());

    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0;

    g_model = llama_model_load_from_file(model_path.c_str(), model_params);
    if (g_model == nullptr) {
        LOGE("Could not load model: %s", model_path.c_str());
        return false;
    }

    g_model_path = model_path;
    LOGI("Model loaded");
    return true;
}

std::vector<llama_token> tokenize_prompt(const llama_vocab * vocab, const std::string & prompt) {
    const int n_prompt = -llama_tokenize(vocab, prompt.c_str(), prompt.size(), nullptr, 0, true, true);
    if (n_prompt <= 0) {
        return {};
    }
    std::vector<llama_token> tokens(n_prompt);
    const int result = llama_tokenize(vocab, prompt.c_str(), prompt.size(), tokens.data(), tokens.size(), true, true);
    if (result < 0) {
        return {};
    }
    return tokens;
}

std::string token_to_text(const llama_vocab * vocab, llama_token token) {
    char buffer[256];
    const int written = llama_token_to_piece(vocab, token, buffer, sizeof(buffer), 0, true);
    if (written <= 0) {
        return "";
    }
    return std::string(buffer, written);
}

std::string generate_response(const std::string & model_path, const std::string & prompt) {
    std::lock_guard<std::mutex> lock(g_mutex);

    if (!ensure_model_loaded(model_path)) {
        throw std::runtime_error("No se pudo cargar el modelo local.");
    }

    const llama_vocab * vocab = llama_model_get_vocab(g_model);
    std::vector<llama_token> prompt_tokens = tokenize_prompt(vocab, prompt);
    if (prompt_tokens.empty()) {
        throw std::runtime_error("No se pudo tokenizar la pregunta.");
    }
    LOGI("Prompt tokens: %zu", prompt_tokens.size());

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = static_cast<uint32_t>(prompt_tokens.size() + kMaxGeneratedTokens + kContextPadding);
    ctx_params.n_batch = ctx_params.n_ctx;
    ctx_params.n_threads = static_cast<int32_t>(std::max(1u, std::min(4u, std::thread::hardware_concurrency())));
    ctx_params.n_threads_batch = ctx_params.n_threads;
    ctx_params.no_perf = true;

    llama_context * ctx = llama_init_from_model(g_model, ctx_params);
    if (ctx == nullptr) {
        throw std::runtime_error("No se pudo iniciar el contexto del modelo.");
    }

    llama_sampler_chain_params sampler_params = llama_sampler_chain_default_params();
    sampler_params.no_perf = true;
    llama_sampler * sampler = llama_sampler_chain_init(sampler_params);
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.2f));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_k(20));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_p(0.85f, 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(42));

    llama_batch batch = llama_batch_get_one(prompt_tokens.data(), prompt_tokens.size());
    if (llama_decode(ctx, batch) != 0) {
        llama_sampler_free(sampler);
        llama_free(ctx);
        throw std::runtime_error("No se pudo procesar el prompt.");
    }
    LOGI("Prompt decoded");

    std::string output;
    for (int i = 0; i < kMaxGeneratedTokens; i += 1) {
        llama_token token = llama_sampler_sample(sampler, ctx, -1);
        if (llama_vocab_is_eog(vocab, token)) {
            break;
        }

        llama_sampler_accept(sampler, token);
        output += token_to_text(vocab, token);

        batch = llama_batch_get_one(&token, 1);
        if (llama_decode(ctx, batch) != 0) {
            break;
        }
    }

    llama_sampler_free(sampler);
    llama_free(ctx);
    LOGI("Generated chars: %zu", output.size());
    return output;
}
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_aprendia_app_llm_LlamaCppLocalLlmEngine_generateNative(
        JNIEnv * env,
        jobject,
        jstring model_path,
        jstring prompt
) {
    const char * model_path_chars = env->GetStringUTFChars(model_path, nullptr);
    const char * prompt_chars = env->GetStringUTFChars(prompt, nullptr);

    std::string model_path_string(model_path_chars == nullptr ? "" : model_path_chars);
    std::string prompt_string(prompt_chars == nullptr ? "" : prompt_chars);

    if (model_path_chars != nullptr) {
        env->ReleaseStringUTFChars(model_path, model_path_chars);
    }
    if (prompt_chars != nullptr) {
        env->ReleaseStringUTFChars(prompt, prompt_chars);
    }

    try {
        std::string response = generate_response(model_path_string, prompt_string);
        return env->NewStringUTF(response.c_str());
    } catch (const std::exception & exception) {
        throw_illegal_state(env, exception.what());
        return nullptr;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_aprendia_app_llm_LlamaCppLocalLlmEngine_loadModelNative(
        JNIEnv * env,
        jobject,
        jstring model_path
) {
    const char * model_path_chars = env->GetStringUTFChars(model_path, nullptr);
    std::string model_path_string(model_path_chars == nullptr ? "" : model_path_chars);
    if (model_path_chars != nullptr) {
        env->ReleaseStringUTFChars(model_path, model_path_chars);
    }

    try {
        std::lock_guard<std::mutex> lock(g_mutex);
        if (!ensure_model_loaded(model_path_string)) {
            throw std::runtime_error("No se pudo cargar el modelo local.");
        }
    } catch (const std::exception & exception) {
        throw_illegal_state(env, exception.what());
    }
}
