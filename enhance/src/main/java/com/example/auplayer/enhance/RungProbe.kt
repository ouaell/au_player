package com.example.auplayer.enhance

/**
 * Probe-first rung selection (P1.2): after forcing redraws of the current
 * frame, pick the highest rung whose summed shader pass time fits the
 * budget — 55% of the frame interval, 35% when the source runs ≥ 50 fps.
 * Pass timings come from mpv `vo-passes` on device (E3); the reactive
 * governor stays the second line. Returns null when no rung fits (→ Off).
 */
object RungProbe {
    fun choose(
        rungMs: Map<Int, Double>,
        frameIntervalMs: Double,
        fps: Double
    ): Int? {
        if (frameIntervalMs <= 0) return null
        val budget = if (fps >= HIGH_FPS) {
            frameIntervalMs * TIGHT_BUDGET
        } else {
            frameIntervalMs * NORMAL_BUDGET
        }
        return rungMs.entries
            .filter { it.value <= budget }
            .maxByOrNull { it.key }
            ?.key
    }
}

private const val NORMAL_BUDGET = 0.55
private const val TIGHT_BUDGET = 0.35
private const val HIGH_FPS = 50.0
