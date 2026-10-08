@file:Suppress("MagicNumber") // ColorMatrix rows are color-science constants.
package com.example.auplayer.reader

import com.example.auplayer.core.ReaderFilter
import com.example.auplayer.core.ReaderModeSetting

/** Tap-zone actions (MS-038). Preview + persistence reuse this mapping. */
enum class TapAction {
    PREV,
    NEXT,
    MENU
}

data class TapZoneConfig(
    val left: TapAction = TapAction.PREV,
    val center: TapAction = TapAction.MENU,
    val right: TapAction = TapAction.NEXT
)

enum class ReaderTapZone {
    LEFT,
    CENTER,
    RIGHT
}

fun actionFor(zone: ReaderTapZone, config: TapZoneConfig): TapAction {
    return when (zone) {
        ReaderTapZone.LEFT -> config.left
        ReaderTapZone.CENTER -> config.center
        ReaderTapZone.RIGHT -> config.right
    }
}

fun readerProgress(current: Int, total: Int): Float {
    if (total <= 0) return NO_PROGRESS
    return (current + 1).toFloat() / total
}

/** Filter → ColorMatrix rows (MS-038). Grayscale/invert/sepia exact. */
fun filterMatrix(filter: ReaderFilter): FloatArray {
    return when (filter) {
        ReaderFilter.NONE -> floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        ReaderFilter.GRAYSCALE -> floatArrayOf(
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        ReaderFilter.INVERT -> floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        )
        ReaderFilter.SEPIA -> floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }
}

/** Whether the mode pages discretely (pager) or scrolls (webtoon). */
fun isPagedMode(mode: ReaderModeSetting): Boolean {
    return when (mode) {
        ReaderModeSetting.WEBTOON -> false
        ReaderModeSetting.RTL -> true
        ReaderModeSetting.LTR -> true
        ReaderModeSetting.VERTICAL_PAGED -> true
        ReaderModeSetting.DOUBLE_PAGE -> true
    }
}

private const val NO_PROGRESS = 0f
