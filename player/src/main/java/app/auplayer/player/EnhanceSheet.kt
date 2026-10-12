package app.auplayer.player

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import app.auplayer.aucore.EnhanceMode
import app.auplayer.enhance.ShaderChainBuilder

/** Chip/sheet label with live governor state (§10: "Anime · lowered"). */
fun enhanceLabel(mode: EnhanceMode, lowered: Boolean): String {
    return when (mode) {
        EnhanceMode.OFF -> "Off"
        EnhanceMode.ANIME -> if (lowered) "Anime · lowered" else "Anime"
        EnhanceMode.STANDARD -> if (lowered) "Standard · lowered" else "Standard"
    }
}

private fun enabledSuffix(mode: EnhanceMode, enabled: Boolean): String {
    return if (!enabled && mode != EnhanceMode.OFF) " (Already high resolution)" else ""
}

private const val SELECTED_PREFIX = "✓ "

/**
 * Enhance sheet (MS-029): exactly Off/Anime/Standard. Disabled with
 * "Already high resolution" above 1440p (§6.1). Interaction tests are T2.
 */
@Composable
fun EnhanceSheet(
    selected: EnhanceMode,
    lowered: Boolean,
    sourceHeightPx: Int,
    onSelect: (EnhanceMode) -> Unit
) {
    Column {
        EnhanceMode.entries.forEach { mode ->
            val enabled = mode == EnhanceMode.OFF ||
                ShaderChainBuilder.available(mode, sourceHeightPx)
            val label = enhanceLabel(mode, lowered)
            TextButton(
                onClick = { onSelect(mode) },
                enabled = enabled
            ) {
                Text(
                    text = if (mode == selected) {
                        SELECTED_PREFIX + label + enabledSuffix(mode, enabled)
                    } else {
                        label + enabledSuffix(mode, enabled)
                    }
                )
            }
        }
    }
}
