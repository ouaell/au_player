package app.auplayer.player

import app.auplayer.aucore.Diag

/**
 * Own JNI bindings to the self-built libmpv (P0/Push B1). API usage mirrors
 * libmpv `client.h`/`render.h` (resolved against OUR pinned headers at
 * compile time — no hardcoded ABI numbers except mpv event NAMES, which are
 * stable strings). Render + event-loop entry points live in [MpvRender] and
 * [MpvEvents] (function-count gate). Full path is E-verified (E1/E3); until
 * then every function compiles in T1 and fails closed when native is absent.
 */
object MpvNative {
    var loaded: Boolean = false
        private set

    /** Last load failure (pure string: safe to read from JVM tests too). */
    var lastError: String? = null
        private set

    fun load(): Boolean {
        if (loaded) return true
        loaded = try {
            System.loadLibrary("mpv")
            System.loadLibrary("au_mpv")
            true
        } catch (error: UnsatisfiedLinkError) {
            // No android.util.Log here: this runs on plain JVM unit tests too.
            lastError = error.message
            Diag.record("mpv", "native missing: " + error.message.toString())
            false
        }
        return loaded
    }

    /**
     * Hands FFmpeg the application Context so its `android_content` protocol
     * can read `content://` URIs. Without this mpv reports "application
     * context is not set" and every MediaStore item fails to open. Typed as
     * Any so this file stays usable from plain JVM unit tests.
     */
    fun setAppContext(context: Any?) {
        if (!load() || context == null) return
        try {
            nativeSetAppContext(context)
        } catch (error: UnsatisfiedLinkError) {
            lastError = error.message
            Diag.record("mpv", "app context rejected: " + error.message.toString())
        }
    }

    external fun nativeSetAppContext(context: Any)
    external fun nativeCreate(): Long
    external fun nativeInit(handle: Long): Int
    external fun nativeSetOption(handle: Long, key: String, value: String): Int
    external fun nativeCommand(handle: Long, args: Array<String>): Int
    external fun nativeGetProperty(handle: Long, name: String): String?
    external fun nativeObserve(handle: Long, id: Long, name: String)
    external fun nativeUnobserve(handle: Long, id: Long)
    external fun nativeErrorString(code: Int): String
    external fun nativeDestroy(handle: Long)
}
