package com.example.auplayer.enhance

/**
 * Internal safety governor (§6.4, MS-030): deterministic, never steps up
 * within a video. Counters are mpv totals (`frame-drop-count`,
 * `decoder-frame-drop-count`); thermal follows PowerManager levels with
 * MODERATE = 2 (API 29+; below that the caller passes NONE = 0).
 */
class EnhanceGovernor(private val maxRung: Int = 1) {
    var rung: Int = 0
        private set
    var lowered: Boolean = false
        private set

    private var badStreak = 0
    private var lastDropped = 0L
    private var lastDecoderDropped = 0L

    /** New video → top rung again (one lower on Battery Saver). */
    fun startVideo(batterySaver: Boolean) {
        rung = if (batterySaver) 1 else 0
        if (rung > maxRung) rung = maxRung
        lowered = rung > 0
        badStreak = 0
    }

    fun sample(droppedTotal: Long, decoderDroppedTotal: Long, thermal: Int): GovernorDecision {
        val drops = (droppedTotal - lastDropped) + (decoderDroppedTotal - lastDecoderDropped)
        lastDropped = droppedTotal
        lastDecoderDropped = decoderDroppedTotal
        if (thermal >= THERMAL_MODERATE) {
            return stepDown()
        }
        if (drops >= DROPS_PER_SAMPLE) {
            badStreak++
        } else {
            badStreak = 0
        }
        if (badStreak >= BAD_SAMPLES) {
            return stepDown()
        }
        return GovernorDecision.STAY
    }

    private fun stepDown(): GovernorDecision {
        badStreak = 0
        if (rung >= maxRung) return GovernorDecision.AT_BOTTOM
        rung++
        lowered = true
        return GovernorDecision.STEP_DOWN
    }
}

enum class GovernorDecision {
    STAY,
    STEP_DOWN,
    AT_BOTTOM
}

private const val DROPS_PER_SAMPLE = 3L
private const val BAD_SAMPLES = 3
private const val THERMAL_MODERATE = 2
