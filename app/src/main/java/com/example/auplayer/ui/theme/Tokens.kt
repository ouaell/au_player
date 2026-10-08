@file:Suppress("MagicNumber") // ARGB hex literals are the token values themselves.
package com.example.auplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Design tokens §10 (semantic names kept; values tunable in MS-005).
val bg = Color(0xFF000000)
val surface = Color(0xFF101015)
val surfaceHigh = Color(0xFF1A1A22)
val outline = Color(0xFF2A2A35)
val onSurface = Color(0xFFECECF2)
val muted = Color(0xFF9A9AAB)
val accent = Color(0xFF7C5CFF)
val accent2 = Color(0xFF00D1FF)
val danger = Color(0xFFFF5C6C)
val success = Color(0xFF3DDC97)

private val dark = darkColorScheme(
    background = bg,
    surface = surface,
    surfaceContainerHigh = surfaceHigh,
    outline = outline,
    onSurface = onSurface,
    onSurfaceVariant = muted,
    primary = accent,
    secondary = accent2,
    error = danger
)
private val light = lightColorScheme(
    primary = accent,
    secondary = accent2,
    error = danger
)

@Composable
fun AuTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) dark else light,
        typography = auType,
        shapes = auShapes,
        content = content
    )
}
