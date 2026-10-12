package app.auplayer.player

/** Audio-focus changes we react to (MS-022). */
enum class AudioFocusChange {
    LOSS,
    TRANSIENT_LOSS,
    DUCK,
    GAIN
}

sealed interface FocusAction {
    data object Pause : FocusAction
    data object Resume : FocusAction
    data object Duck : FocusAction
    data object Unduck : FocusAction
    data object Nothing : FocusAction
}

/**
 * Pure focus policy (§5.2): LOSS/TRANSIENT pause (resume on GAIN only if we
 * paused); DUCK lowers volume until GAIN. JVM-tested with synthetic events.
 */
class AudioFocusHandler {
    private var pausedByUs = false
    private var ducked = false

    fun onChange(change: AudioFocusChange, playing: Boolean): FocusAction {
        return when (change) {
            AudioFocusChange.LOSS -> {
                ducked = false
                if (playing) {
                    pausedByUs = true
                    FocusAction.Pause
                } else {
                    FocusAction.Nothing
                }
            }
            AudioFocusChange.TRANSIENT_LOSS -> {
                if (playing) {
                    pausedByUs = true
                    FocusAction.Pause
                } else {
                    FocusAction.Nothing
                }
            }
            AudioFocusChange.DUCK -> {
                if (playing && !ducked) {
                    ducked = true
                    FocusAction.Duck
                } else {
                    FocusAction.Nothing
                }
            }
            AudioFocusChange.GAIN -> {
                if (pausedByUs) {
                    pausedByUs = false
                    ducked = false
                    FocusAction.Resume
                } else if (ducked) {
                    ducked = false
                    FocusAction.Unduck
                } else {
                    FocusAction.Nothing
                }
            }
        }
    }
}
