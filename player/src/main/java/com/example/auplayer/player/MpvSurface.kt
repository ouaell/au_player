package com.example.auplayer.player

/**
 * Android Surface -> mpv `wid` plumbing (E1: vo/gpu android-context reads the
 * Surface jobject address from the WinID option at vo init — "Missing surface
 * pointer" otherwise). attach stores a global ref and returns its address for
 * `set wid <addr>`; detach releases it. Split file per the function-count gate.
 */
object MpvSurface {
    external fun nativeAttachSurface(handle: Long, surface: Any?): Long
    external fun nativeDetachSurface(handle: Long)
}
