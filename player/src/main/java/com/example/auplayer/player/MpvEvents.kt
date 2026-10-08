package com.example.auplayer.player

/** mpv event pump (split from MpvNative by the function-count gate). */
object MpvEvents {
    external fun nativeStartEvents(handle: Long, callback: MpvEventCallback)
    external fun nativeStopEvents(handle: Long)

    interface MpvEventCallback {
        fun onEvent(name: String, replyUserdata: Long)
    }
}
