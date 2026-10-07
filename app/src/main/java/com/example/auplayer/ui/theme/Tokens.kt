package com.example.auplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Design tokens §10 (semantic names kept; values tunable in MS-005).
val Bg = Color(0xFF000000)
val Surface = Color(0xFF101015)
val SurfaceHigh = Color(0xFF1A1A22)
val Outline = Color(0xFF2A2A35)
val OnSurface = Color(0xFFECECF2)
val Muted = Color(0xFF9A9AAB)
val Accent = Color(0xFF7C5CFF)
val Accent2 = Color(0xFF00D1FF)
val Danger = Color(0xFFFF5C6C)
val Success = Color(0xFF3DDC97)

private val Dark = darkColorScheme(
    background = Bg,
    surface = Surface,
    surfaceContainerHigh = SurfaceHigh,
    outline = Outline,
    onSurface = OnSurface,
    onSurfaceVariant = Muted,
    primary = Accent,
    secondary = Accent2,
    error = Danger
)
private val Light = lightColorScheme(
    primary = Accent,
    secondary = Accent2,
    error = Danger
)

@Composable
fun AuTheme(dark: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
