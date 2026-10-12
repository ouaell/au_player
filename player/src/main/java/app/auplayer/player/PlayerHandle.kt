package app.auplayer.player

import app.auplayer.enhance.EnhanceController

/**
 * Player wiring bundle (keeps the screen signature small): engine, enhance
 * controller, back navigation, resume-point reporting.
 */
data class PlayerHandle(
    val engine: PlayerEngine,
    val enhance: EnhanceController,
    val onBack: () -> Unit,
    val onProgress: (positionMs: Long, durationMs: Long) -> Unit = { _, _ -> }
)
