package app.auplayer.player

/** Double-tap zones (§5.1): left/right ±10 s, center pauses. */
enum class TapZone {
    LEFT,
    CENTER,
    RIGHT,
}

/** Pure gesture→action outputs. Screen effects (brightness/volume) bind in MS-014+. */
sealed interface GestureAction {
    data object Ignored : GestureAction
    data object ToggleControls : GestureAction
    data object TogglePlay : GestureAction
    data class SeekBy(val deltaMs: Long) : GestureAction
    data class AdjustBrightness(val deltaFraction: Float) : GestureAction
    data class AdjustVolume(val deltaFraction: Float) : GestureAction
    data class Zoom(val factor: Float) : GestureAction
    data class HoldSpeed(val active: Boolean) : GestureAction
}

data class GestureConfig(
    val doubleTapSeconds: Long = DEFAULT_DOUBLE_TAP_SECONDS,
    val pinchMinZoom: Float = MIN_ZOOM,
    val pinchMaxZoom: Float = MAX_ZOOM,
)

/**
 * Pure gesture mapper (§5.1, MS-013). All gestures are ignored while locked
 * and (by callers) inside system-gesture insets.
 */
object GestureMapper {
    fun horizontalDrag(dxFraction: Float, durationMs: Long, locked: Boolean): GestureAction {
        if (locked) return GestureAction.Ignored
        return GestureAction.SeekBy((dxFraction * durationMs).toLong())
    }

    fun leftVertical(dyFraction: Float, locked: Boolean): GestureAction {
        if (locked) return GestureAction.Ignored
        return GestureAction.AdjustBrightness(-dyFraction)
    }

    fun rightVertical(dyFraction: Float, locked: Boolean): GestureAction {
        if (locked) return GestureAction.Ignored
        return GestureAction.AdjustVolume(-dyFraction)
    }

    fun doubleTap(zone: TapZone, config: GestureConfig, locked: Boolean): GestureAction {
        if (locked) return GestureAction.Ignored
        val jumpMs = config.doubleTapSeconds * MILLIS_PER_SECOND
        return when (zone) {
            TapZone.LEFT -> GestureAction.SeekBy(-jumpMs)
            TapZone.RIGHT -> GestureAction.SeekBy(jumpMs)
            TapZone.CENTER -> GestureAction.TogglePlay
        }
    }

    fun longPress(active: Boolean, locked: Boolean): GestureAction {
        if (locked) return GestureAction.Ignored
        return GestureAction.HoldSpeed(active)
    }

    fun pinch(factor: Float, config: GestureConfig, locked: Boolean): GestureAction {
        if (locked) return GestureAction.Ignored
        return GestureAction.Zoom(factor.coerceIn(config.pinchMinZoom, config.pinchMaxZoom))
    }

    fun singleTap(locked: Boolean): GestureAction {
        if (locked) return GestureAction.Ignored
        return GestureAction.ToggleControls
    }
}

private const val DEFAULT_DOUBLE_TAP_SECONDS = 10L
private const val MIN_ZOOM = 0.5f
private const val MAX_ZOOM = 4f
private const val MILLIS_PER_SECOND = 1_000L
