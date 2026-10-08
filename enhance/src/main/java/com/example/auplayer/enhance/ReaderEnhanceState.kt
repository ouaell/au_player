package com.example.auplayer.enhance

/**
 * Reader enhance UI state + guards (MS-049). Per-page "enhancing…"
 * indicators ride [enhancingPages]; thermal/battery pauses new work.
 * Hold-to-compare reuses the snapshot pattern (MS-031).
 */
data class ReaderEnhanceState(
    val enabled: Boolean = false,
    val enhancingPages: Set<Int> = emptySet()
)

/** Pause new enhancement when hot or saving battery (§9). */
fun shouldPauseEnhance(thermal: Int, batterySaver: Boolean): Boolean {
    return batterySaver || thermal >= THERMAL_MODERATE
}

private const val THERMAL_MODERATE = 2
