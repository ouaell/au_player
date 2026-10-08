package com.example.auplayer.core

/** Final settings list (§10/MS-024). Nothing else ships in 1.0. */
enum class EnhanceMode {
    OFF,
    ANIME,
    STANDARD
}

enum class DecoderPref {
    HARDWARE,
    SOFTWARE
}

enum class OrientationSetting {
    AUTO,
    LANDSCAPE,
    PORTRAIT,
    FOLLOW_VIDEO
}

enum class AspectSetting {
    FIT,
    FILL,
    STRETCH,
    SIXTEEN_NINE,
    FOUR_THREE,
    CUSTOM
}

enum class ReaderModeSetting {
    RTL,
    LTR,
    VERTICAL_PAGED,
    WEBTOON,
    DOUBLE_PAGE
}

enum class ReaderFilter {
    NONE,
    GRAYSCALE,
    INVERT,
    SEPIA
}

data class AppSettings(
    val resume: Boolean = true,
    val autoNext: Boolean = true,
    val backgroundPlay: Boolean = true,
    val pip: Boolean = true,
    val gestureSeek: Boolean = true,
    val gestureBrightness: Boolean = true,
    val gestureVolume: Boolean = true,
    val doubleTapSeconds: Long = DEFAULT_DOUBLE_TAP_SECONDS,
    val subSizeSp: Int = DEFAULT_SUB_SIZE,
    val subColorArgb: Long = WHITE_ARGB,
    val subOutline: Int = DEFAULT_SUB_OUTLINE,
    val subPosition: Float = DEFAULT_SUB_POSITION,
    val subFont: String? = null,
    val subDelaySec: Double = NO_DELAY,
    val audioBoost: Float = NO_BOOST,
    val audioLanguage: String? = null,
    val downmix: Boolean = true,
    val orientation: OrientationSetting = OrientationSetting.AUTO,
    val defaultAspect: AspectSetting = AspectSetting.FIT,
    val decoder: DecoderPref = DecoderPref.HARDWARE,
    val readerMode: ReaderModeSetting = ReaderModeSetting.LTR,
    val readerKeepScreenOn: Boolean = true,
    val readerFilter: ReaderFilter = ReaderFilter.NONE,
    val readerCropBorders: Boolean = false,
    val enhanceMode: EnhanceMode = EnhanceMode.OFF
)

private const val DEFAULT_DOUBLE_TAP_SECONDS = 10L
private const val DEFAULT_SUB_SIZE = 16
private const val WHITE_ARGB = 0xFFFFFFFFL
private const val DEFAULT_SUB_OUTLINE = 2
private const val DEFAULT_SUB_POSITION = 100f
private const val NO_DELAY = 0.0
private const val NO_BOOST = 1f
