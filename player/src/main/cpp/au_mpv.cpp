// au_mpv.cpp — own JNI bindings to the self-built libmpv (Apache-2.0, ours).
//
// Uses libmpv client.h/render.h symbols resolved against OUR pinned headers
// at compile time (no hardcoded ABI numbers). Event names cross JNI as
// stable strings (mpv_event_name), never enum ordinals.

#include <jni.h>

#include <atomic>
#include <string>
#include <thread>
#include <vector>

#include <EGL/egl.h>
#include <mpv/client.h>
#include <mpv/render.h>
#include <mpv/render_gl.h>

namespace {

struct Ctx {
    mpv_handle* mpv = nullptr;
    mpv_render_context* render = nullptr;
    JavaVM* jvm = nullptr;
    jobject callback = nullptr;
    jmethodID onEvent = nullptr;
    std::thread loop;
    std::atomic<bool> running{false};
};

Ctx* fromHandle(jlong handle) {
    return reinterpret_cast<Ctx*>(handle);
}

std::string jstr(JNIEnv* env, jstring value) {
    if (!value) {
        return {};
    }
    const char* chars = env->GetStringUTFChars(value, nullptr);
    std::string out(chars != nullptr ? chars : "");
    if (chars != nullptr) {
        env->ReleaseStringUTFChars(value, chars);
    }
    return out;
}

void* gl_get_proc_address(void*, const char* name) {
    return reinterpret_cast<void*>(eglGetProcAddress(name));
}

void eventLoop(Ctx* ctx) {
    JNIEnv* env = nullptr;
    ctx->jvm->AttachCurrentThread(&env, nullptr);
    while (ctx->running.load()) {
        mpv_event* event = mpv_wait_event(ctx->mpv, 0.25);
        if (event == nullptr || event->event_id == MPV_EVENT_NONE) {
            continue;
        }
        const char* name = mpv_event_name(event->event_id);
        jstring jname = env->NewStringUTF(name != nullptr ? name : "unknown");
        env->CallVoidMethod(ctx->callback, ctx->onEvent, jname,
                            static_cast<jlong>(event->reply_userdata));
        env->DeleteLocalRef(jname);
        if (env->ExceptionCheck() != 0) {
            env->ExceptionClear();
        }
    }
    ctx->jvm->DetachCurrentThread();
}

}  // namespace

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_example_auplayer_player_MpvNative_nativeCreate(JNIEnv* env, jobject) {
    auto* ctx = new (std::nothrow) Ctx();
    if (ctx == nullptr) {
        return 0;
    }
    ctx->mpv = mpv_create();
    if (ctx->mpv == nullptr) {
        delete ctx;
        return 0;
    }
    env->GetJavaVM(&ctx->jvm);
    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT jint JNICALL
Java_com_example_auplayer_player_MpvNative_nativeInit(JNIEnv*, jobject, jlong handle) {
    Ctx* ctx = fromHandle(handle);
    if (ctx == nullptr || ctx->mpv == nullptr) {
        return MPV_ERROR_UNINITIALIZED;
    }
    return mpv_initialize(ctx->mpv);
}

JNIEXPORT jint JNICALL
Java_com_example_auplayer_player_MpvNative_nativeSetOption(
    JNIEnv* env, jobject, jlong handle, jstring key, jstring value) {
    Ctx* ctx = fromHandle(handle);
    if (ctx == nullptr || ctx->mpv == nullptr) {
        return MPV_ERROR_UNINITIALIZED;
    }
    const std::string k = jstr(env, key);
    const std::string v = jstr(env, value);
    return mpv_set_option_string(ctx->mpv, k.c_str(), v.c_str());
}

JNIEXPORT jint JNICALL
Java_com_example_auplayer_player_MpvNative_nativeCommand(
    JNIEnv* env, jobject, jlong handle, jobjectArray args) {
    Ctx* ctx = fromHandle(handle);
    if (ctx == nullptr || ctx->mpv == nullptr) {
        return MPV_ERROR_UNINITIALIZED;
    }
    const jsize count = env->GetArrayLength(args);
    std::vector<std::string> owned;
    owned.reserve(static_cast<size_t>(count));
    for (jsize i = 0; i < count; i++) {
        auto* item = static_cast<jstring>(env->GetObjectArrayElement(args, i));
        owned.push_back(jstr(env, item));
        env->DeleteLocalRef(item);
    }
    std::vector<const char*> argv;
    argv.reserve(owned.size() + 1);
    for (const auto& item : owned) {
        argv.push_back(item.c_str());
    }
    argv.push_back(nullptr);
    return mpv_command(ctx->mpv, argv.data());
}

JNIEXPORT jstring JNICALL
Java_com_example_auplayer_player_MpvNative_nativeGetProperty(
    JNIEnv* env, jobject, jlong handle, jstring name) {
    Ctx* ctx = fromHandle(handle);
    if (ctx == nullptr || ctx->mpv == nullptr) {
        return nullptr;
    }
    const std::string key = jstr(env, name);
    char* value = mpv_get_property_string(ctx->mpv, key.c_str());
    if (value == nullptr) {
        return nullptr;
    }
    jstring out = env->NewStringUTF(value);
    mpv_free(value);
    return out;
}

JNIEXPORT void JNICALL
Java_com_example_auplayer_player_MpvNative_nativeObserve(
    JNIEnv* env, jobject, jlong handle, jlong id, jstring name) {
    Ctx* ctx = fromHandle(handle);
    if (ctx == nullptr || ctx->mpv == nullptr) {
        return;
    }
    const std::string key = jstr(env, name);
    mpv_observe_property(ctx->mpv, static_cast<uint64_t>(id), key.c_str(), MPV_FORMAT_STRING);
}

JNIEXPORT void JNICALL
Java_com_example_auplayer_player_MpvNative_nativeUnobserve(
    JNIEnv*, jobject, jlong handle, jlong id) {
    Ctx* ctx = fromHandle(handle);
    if (ctx == nullptr || ctx->mpv == nullptr) {
        return;
    }
    mpv_unobserve_property(ctx->mpv, static_cast<uint64_t>(id));
}

JNIEXPORT jint JNICALL
Java_com_example_auplayer_player_MpvNative_nativeRenderInit(
    JNIEnv*, jobject, jlong handle) {
    Ctx* ctx = fromHandle(handle);
    if (ctx == nullptr || ctx->mpv == nullptr || ctx->render != nullptr) {
        return MPV_ERROR_UNINITIALIZED;
    }
    mpv_opengl_init_params gl = {gl_get_proc_address, nullptr, nullptr};
    mpv_render_param params[] = {
        {MPV_RENDER_PARAM_API_TYPE, reinterpret_cast<void*>(MPV_RENDER_API_TYPE_OPENGL)},
        {MPV_RENDER_PARAM_OPENGL_INIT_PARAMS, &gl},
        {MPV_RENDER_PARAM_INVALID, nullptr},
    };
    return mpv_render_context_create(&ctx->render, ctx->mpv, params);
}

JNIEXPORT jboolean JNICALL
Java_com_example_auplayer_player_MpvNative_nativeCheckUpdate(
    JNIEnv*, jobject, jlong handle) {
    Ctx* ctx = fromHandle(handle);
    if (ctx == nullptr || ctx->render == nullptr) {
        return JNI_FALSE;
    }
    const uint64_t flags = mpv_render_context_update(ctx->render);
    return (flags & MPV_RENDER_UPDATE_FRAME) != 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_com_example_auplayer_player_MpvNative_nativeRender(
    JNIEnv*, jobject, jlong handle, jint width, jint height) {
    Ctx* ctx = fromHandle(handle);
    if (ctx == nullptr || ctx->render == nullptr) {
        return MPV_ERROR_UNINITIALIZED;
    }
    mpv_opengl_fbo fbo = {0, width, height, 0};
    int flip = 1;
    mpv_render_param params[] = {
        {MPV_RENDER_PARAM_OPENGL_FBO, &fbo},
        {MPV_RENDER_PARAM_FLIP_Y, &flip},
        {MPV_RENDER_PARAM_INVALID, nullptr},
    };
    return mpv_render_context_render(ctx->render, params);
}

JNIEXPORT void JNICALL
Java_com_example_auplayer_player_MpvNative_nativeStartEvents(
    JNIEnv* env, jobject, jlong handle, jobject callback) {
    Ctx* ctx = fromHandle(handle);
    if (ctx == nullptr || ctx->jvm == nullptr || ctx->running.load()) {
        return;
    }
    ctx->callback = env->NewGlobalRef(callback);
    jclass cls = env->GetObjectClass(callback);
    ctx->onEvent = env->GetMethodID(cls, "onEvent", "(Ljava/lang/String;J)V");
    env->DeleteLocalRef(cls);
    if (ctx->onEvent == nullptr) {
        env->DeleteGlobalRef(ctx->callback);
        ctx->callback = nullptr;
        return;
    }
    ctx->running = true;
    ctx->loop = std::thread(eventLoop, ctx);
}

JNIEXPORT void JNICALL
Java_com_example_auplayer_player_MpvNative_nativeStopEvents(
    JNIEnv* env, jobject, jlong handle) {
    Ctx* ctx = fromHandle(handle);
    if (ctx == nullptr) {
        return;
    }
    ctx->running = false;
    if (ctx->loop.joinable()) {
        ctx->loop.join();
    }
    if (ctx->callback != nullptr) {
        env->DeleteGlobalRef(ctx->callback);
        ctx->callback = nullptr;
    }
}

JNIEXPORT jstring JNICALL
Java_com_example_auplayer_player_MpvNative_nativeErrorString(
    JNIEnv* env, jobject, jint code) {
    return env->NewStringUTF(mpv_error_string(code));
}

JNIEXPORT void JNICALL
Java_com_example_auplayer_player_MpvNative_nativeDestroy(
    JNIEnv* env, jobject, jlong handle) {
    Ctx* ctx = fromHandle(handle);
    if (ctx == nullptr) {
        return;
    }
    ctx->running = false;
    if (ctx->loop.joinable()) {
        ctx->loop.join();
    }
    if (ctx->callback != nullptr) {
        env->DeleteGlobalRef(ctx->callback);
    }
    if (ctx->render != nullptr) {
        mpv_render_context_free(ctx->render);
    }
    if (ctx->mpv != nullptr) {
        mpv_terminate_destroy(ctx->mpv);
    }
    delete ctx;
}

}  // extern "C"
