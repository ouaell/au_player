package com.example.auplayer.player

/** Aspect modes (§5.1). Chip cycles in declaration order. */
enum class AspectMode {
    FIT,
    FILL,
    STRETCH,
    SIXTEEN_NINE,
    FOUR_THREE,
    CUSTOM
}

fun cycleAspect(current: AspectMode): AspectMode {
    val modes = AspectMode.entries
    return modes[(modes.indexOf(current) + 1) % modes.size]
}

/** Display size for [mode]; CUSTOM uses [customRatio] (w/h). */
fun aspectSize(mode: AspectMode, videoW: Int, videoH: Int, customRatio: Float): Pair<Int, Int> {
    if (videoW <= 0 || videoH <= 0) return Pair(0, 0)
    val ratio = when (mode) {
        AspectMode.FIT, AspectMode.FILL -> videoW.toFloat() / videoH
        AspectMode.STRETCH -> videoW.toFloat() / videoH
        AspectMode.SIXTEEN_NINE -> RATIO_16_9
        AspectMode.FOUR_THREE -> RATIO_4_3
        AspectMode.CUSTOM -> customRatio.coerceAtLeast(MIN_RATIO)
    }
    return Pair((videoH * ratio).toInt(), videoH)
}

/** Orientation preference (§5.1); phones only, tablets handled in MS-069. */
enum class OrientationPref {
    AUTO,
    LANDSCAPE,
    PORTRAIT,
    FOLLOW_VIDEO
}

enum class EffectiveOrientation {
    UNSPECIFIED,
    LANDSCAPE,
    PORTRAIT
}

fun effectiveOrientation(pref: OrientationPref, videoW: Int, videoH: Int): EffectiveOrientation {
    return when (pref) {
        OrientationPref.AUTO -> EffectiveOrientation.UNSPECIFIED
        OrientationPref.LANDSCAPE -> EffectiveOrientation.LANDSCAPE
        OrientationPref.PORTRAIT -> EffectiveOrientation.PORTRAIT
        OrientationPref.FOLLOW_VIDEO -> if (videoW >= videoH) {
            EffectiveOrientation.LANDSCAPE
        } else {
            EffectiveOrientation.PORTRAIT
        }
    }
}

/** Pinch-zoom state; resets on video change (MS-015). */
class ZoomState(private val config: GestureConfig = GestureConfig()) {
    var factor: Float = NO_ZOOM
        private set

    fun apply(pinchFactor: Float) {
        factor = pinchFactor.coerceIn(config.pinchMinZoom, config.pinchMaxZoom)
    }

    fun reset() {
        factor = NO_ZOOM
    }
}

private const val RATIO_16_9 = 16f / 9f
private const val RATIO_4_3 = 4f / 3f
private const val MIN_RATIO = 0.1f
private const val NO_ZOOM = 1f
