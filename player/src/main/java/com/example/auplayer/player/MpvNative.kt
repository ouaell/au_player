package com.example.auplayer.player

/**
 * Own JNI bindings to the self-built libmpv (P0/Push B1). API usage mirrors
 * libmpv `client.h`/`render.h` (resolved against OUR pinned headers at
 * compile time — no hardcoded ABI numbers except mpv event NAMES, which are
 * stable strings). Full render path is E-verified (E1/E3); until then every
 * function compiles in T1 and fails closed at runtime when native is absent.
 */
object MpvNative {
    var loaded: Boolean = false
        private set

    fun load(): Boolean {
        if (loaded) return true
        loaded = try {
            System.loadLibrary("mpv")
            System.loadLibrary("au_mpv")
            true
        } catch (error: UnsatisfiedLinkError) {
            false
        }
        return loaded
    }

    external fun nativeCreate(): Long
    external fun nativeInit(handle: Long): Int
    external fun nativeSetOption(handle: Long, key: String, value: String): Int
    external fun nativeCommand(handle: Long, args: Array<String>): Int
    external fun nativeGetProperty(handle: Long, name: String): String?
    external fun nativeObserve(handle: Long, id: Long, name: String)
    external fun nativeUnobserve(handle: Long, id: Long)
    external fun nativeRenderInit(handle: Long): Int
    external fun nativeCheckUpdate(handle: Long): Boolean
    external fun nativeRender(handle: Long, width: Int, height: Int): Int
    external fun nativeStartEvents(handle: Long, callback: MpvEventCallback)
    external fun nativeStopEvents(handle: Long)
    external fun nativeErrorString(code: Int): String
    external fun nativeDestroy(handle: Long)

    interface MpvEventCallback {
        fun onEvent(name: String, replyUserdata: Long)
    }
}
