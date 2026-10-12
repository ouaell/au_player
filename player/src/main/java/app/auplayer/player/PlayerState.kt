package app.auplayer.player

/** Playback state machine (§5.2). Illegal transitions throw (tested). */
enum class PlayerState {
    Idle,
    Loading,
    Ready,
    Playing,
    Paused,
    Buffering,
    Error,
    Released,
    ;

    /** True only while frames are actually advancing. */
    val isPlaying: Boolean get() = this == Playing
}

class PlayerStateMachine {
    var state: PlayerState = PlayerState.Idle
        private set

    fun transition(to: PlayerState) {
        check(allowed(state).contains(to)) { "illegal transition $state -> $to" }
        state = to
    }

    companion object {
        fun allowed(from: PlayerState): Set<PlayerState> = when (from) {
            PlayerState.Idle -> setOf(PlayerState.Loading)
            PlayerState.Loading -> setOf(PlayerState.Ready, PlayerState.Error)
            PlayerState.Ready ->
                setOf(PlayerState.Playing, PlayerState.Error, PlayerState.Released)
            PlayerState.Playing -> setOf(
                PlayerState.Paused,
                PlayerState.Buffering,
                PlayerState.Error,
                PlayerState.Released,
            )
            PlayerState.Paused -> setOf(
                PlayerState.Playing,
                PlayerState.Buffering,
                PlayerState.Error,
                PlayerState.Released,
            )
            PlayerState.Buffering -> setOf(
                PlayerState.Playing,
                PlayerState.Paused,
                PlayerState.Error,
                PlayerState.Released,
            )
            PlayerState.Error -> setOf(PlayerState.Loading, PlayerState.Released)
            PlayerState.Released -> emptySet()
        }
    }
}

/** Single immutable UI snapshot (§5.2: UI reads one StateFlow of this). */
data class PlayerUiState(
    val state: PlayerState = PlayerState.Idle,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val speed: Float = DEFAULT_SPEED,
    val locked: Boolean = false,
) {
    val progress: Float
        get() = if (durationMs <= 0) {
            NO_PROGRESS
        } else {
            positionMs.toFloat() / durationMs
        }
}

private const val NO_PROGRESS = 0f
private const val DEFAULT_SPEED = 1f
