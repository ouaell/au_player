@file:Suppress("MagicNumber") // Speed presets are the §5.1 spec values (0.25–4×).

package app.auplayer.player

/** Speed presets within §5.1 range 0.25–4×. */
val speedPresets = floatArrayOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 3f, 4f)

fun clampSpeed(value: Float): Float = value.coerceIn(MIN_SPEED, MAX_SPEED)

/** Next preset after [current], wrapping to the first. */
fun cycleSpeed(current: Float): Float {
    val next = speedPresets.firstOrNull { it > current + SPEED_EPSILON }
    return next ?: speedPresets.first()
}

/** A–B repeat segment; B must land after A. */
class ABRepeat {
    var aMs: Long? = null
        private set
    var bMs: Long? = null
        private set

    fun setA(positionMs: Long) {
        aMs = positionMs
        val b = bMs
        if (b != null && b <= positionMs) bMs = null
    }

    /** Returns false (and keeps state) when B does not land after A. */
    fun setB(positionMs: Long): Boolean {
        val valid = aMs != null && positionMs > (aMs ?: 0L)
        if (valid) bMs = positionMs
        return valid
    }

    fun clear() {
        aMs = null
        bMs = null
    }

    /** Seek target while inside the segment, else null. */
    fun loopTarget(positionMs: Long): Long? {
        val a = aMs
        val b = bMs
        return if (a != null && b != null && positionMs >= b) a else null
    }
}

enum class LoopMode {
    OFF,
    ONE,
    ALL,
}

fun cycleLoop(current: LoopMode): LoopMode {
    val modes = LoopMode.entries
    return modes[(modes.indexOf(current) + 1) % modes.size]
}

enum class EofAction {
    STOP,
    REPLAY,
    NEXT,
}

fun afterEof(mode: LoopMode, hasNextInPlaylist: Boolean): EofAction {
    return when (mode) {
        LoopMode.ONE -> EofAction.REPLAY
        LoopMode.ALL -> if (hasNextInPlaylist) EofAction.NEXT else EofAction.REPLAY
        LoopMode.OFF -> if (hasNextInPlaylist) EofAction.NEXT else EofAction.STOP
    }
}

/** Sleep timer as an absolute deadline (clock injected → JVM-tested). */
data class SleepTimer(val endsAtMs: Long) {
    fun remainingMs(nowMs: Long): Long = (endsAtMs - nowMs).coerceAtLeast(NO_TIME_LEFT)
    fun expired(nowMs: Long): Boolean = nowMs >= endsAtMs
}

fun sleepTimer(minutes: Long, nowMs: Long): SleepTimer =
    SleepTimer(nowMs + minutes * MILLIS_PER_MINUTE)

private const val MIN_SPEED = 0.25f
private const val MAX_SPEED = 4f
private const val SPEED_EPSILON = 0.001f
private const val MILLIS_PER_MINUTE = 60_000L
private const val NO_TIME_LEFT = 0L
