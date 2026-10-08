package com.example.auplayer.ui.components

import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/** Pill chip used for controller + enhance sheet rows (MS-012/MS-029 build on it). */
@Composable
fun AuChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(text = label) })
}

/** Translucent panel for controller overlays (blur on API 31+ lands in MS-012). */
@Composable
fun AuGlassPanel(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = GLASS_ALPHA),
        shape = MaterialTheme.shapes.medium,
        content = content
    )
}

private const val GLASS_ALPHA = 0.72f
