package com.example.auplayer.core

/** Window size classes (§17.7-5 large screens). Phones use compact. */
enum class WindowClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

fun windowClass(widthDp: Int): WindowClass {
    return when {
        widthDp < COMPACT_MAX -> WindowClass.COMPACT
        widthDp < MEDIUM_MAX -> WindowClass.MEDIUM
        else -> WindowClass.EXPANDED
    }
}

/** Keep-screen-on policy (MS-070): only while foreground playback. */
fun shouldKeepScreenOn(playing: Boolean, inPip: Boolean, inBackground: Boolean): Boolean {
    return playing && !inPip && !inBackground
}

private const val COMPACT_MAX = 600
private const val MEDIUM_MAX = 840
