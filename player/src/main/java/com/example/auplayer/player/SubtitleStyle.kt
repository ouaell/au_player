package com.example.auplayer.player

/**
 * Subtitle style → mpv props (MS-018). Color order is `#AARRGGBB`
 * ([VERIFY] against the §17.4 subtitle goldens; ASS uses a different order).
 */
data class SubtitleStyle(
    val sizeSp: Int = DEFAULT_SUB_SIZE,
    val colorArgb: Long = WHITE_ARGB,
    val outlineSize: Int = DEFAULT_OUTLINE,
    val position: Float = DEFAULT_POSITION,
    val font: String? = null
)

fun subtitleProps(style: SubtitleStyle): Map<String, String> {
    val props = mutableMapOf(
        "sub-font-size" to style.sizeSp.toString(),
        "sub-color" to mpvColor(style.colorArgb),
        "sub-border-size" to style.outlineSize.toString(),
        "sub-pos" to style.position.toString()
    )
    val font = style.font
    if (font != null) {
        props["sub-font"] = font
    }
    return props
}

/** ARGB Long → mpv `#AARRGGBB`. */
fun mpvColor(argb: Long): String = COLOR_HASH + argb.toString(HEX_RADIX).uppercase().padStart(
    COLOR_WIDTH,
    COLOR_PAD
)

fun delayProps(subDelaySec: Double, audioDelaySec: Double): Map<String, String> = mapOf(
    "sub-delay" to subDelaySec.toString(),
    "audio-delay" to audioDelaySec.toString()
)

private const val DEFAULT_SUB_SIZE = 16
private const val WHITE_ARGB = 0xFFFFFFFFL
private const val DEFAULT_OUTLINE = 2
private const val DEFAULT_POSITION = 100f
private const val COLOR_HASH = "#"
private const val HEX_RADIX = 16
private const val COLOR_WIDTH = 8
private const val COLOR_PAD = '0'
