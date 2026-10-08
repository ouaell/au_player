package com.example.auplayer.player

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/** On-screen gesture feedback kinds (MS-014). */
enum class IndicatorKind {
    SEEK,
    BRIGHTNESS,
    VOLUME,
    SPEED,
    ZOOM
}

/** Pure indicator model; [indicatorText] is JVM-tested, the overlay is T2-tested. */
data class GestureIndicator(val kind: IndicatorKind, val fraction: Float)

fun indicatorText(indicator: GestureIndicator): String {
    return when (indicator.kind) {
        IndicatorKind.SEEK -> {
            val seconds = (indicator.fraction * SECONDS_PER_SCREEN).toLong()
            (if (seconds >= NO_TIME) "+" else "") + seconds.toString() + SECOND_SUFFIX
        }
        IndicatorKind.BRIGHTNESS -> PERCENT_FORMAT.format((indicator.fraction * FULL).toInt())
        IndicatorKind.VOLUME -> PERCENT_FORMAT.format((indicator.fraction * FULL).toInt())
        IndicatorKind.SPEED -> SPEED_LABEL
        IndicatorKind.ZOOM -> ZOOM_FORMAT.format(indicator.fraction)
    }
}

private const val NO_TIME = 0L
private const val SECONDS_PER_SCREEN = 120f
private const val FULL = 100f
private const val SECOND_SUFFIX = "s"
private const val SPEED_LABEL = "2×"
private const val PERCENT_FORMAT = "%d%%"
private const val ZOOM_FORMAT = "%.1fx"

/** Transient OSD bubble driven by [GestureMapper] output. */
@Composable
fun GestureOverlay(indicator: GestureIndicator?) {
    if (indicator == null) return
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = OVERLAY_ALPHA),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(text = indicatorText(indicator))
    }
}

private const val OVERLAY_ALPHA = 0.72f
