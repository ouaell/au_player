package com.example.auplayer.player

/** GPU render entry points (split from MpvNative by the function-count gate). */
object MpvRender {
    external fun nativeRenderInit(handle: Long): Int
    external fun nativeCheckUpdate(handle: Long): Boolean
    external fun nativeRender(handle: Long, width: Int, height: Int): Int
}
