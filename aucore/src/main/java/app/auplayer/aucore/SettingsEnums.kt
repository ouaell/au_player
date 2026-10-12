package app.auplayer.aucore

/** Final settings list (§10/MS-024). Nothing else ships in 1.0. */
enum class EnhanceMode {
    OFF,
    ANIME,
    STANDARD,
}

/** Decoder selection for the player (hardware first, software fallback). */
enum class DecoderPref {
    HARDWARE,
    SOFTWARE,
}

/** Player orientation policy. */
enum class OrientationSetting {
    AUTO,
    LANDSCAPE,
    PORTRAIT,
    FOLLOW_VIDEO,
}
