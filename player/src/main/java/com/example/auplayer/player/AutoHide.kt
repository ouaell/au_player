package com.example.auplayer.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Auto-hide: controls vanish [AUTO_HIDE_TIMEOUT_MS] after interaction, never
 * while scrubbing (MS-012). Pure logic (JVM-tested); the composable below is
 * thin glue verified by T2 Compose tests.
 */
class AutoHide(private val timeoutMs: Long = AUTO_HIDE_TIMEOUT_MS) {
    private var lastInteractMs = NO_TIME
    private var scrubbing = false

    fun interact(nowMs: Long) {
        lastInteractMs = nowMs
    }

    fun setScrubbing(active: Boolean) {
        scrubbing = active
    }

    fun visible(nowMs: Long): Boolean = scrubbing || nowMs - lastInteractMs < timeoutMs
}

fun formatTime(millis: Long): String {
    val totalSeconds = (millis / MILLIS_PER_SECOND).coerceAtLeast(NO_TIME)
    val hours = totalSeconds / SECONDS_PER_HOUR
    val minutes = totalSeconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    val minutePart = minutes.toString().padStart(PAD_WIDTH, PAD_CHAR)
    val secondPart = seconds.toString().padStart(PAD_WIDTH, PAD_CHAR)
    return if (hours > NO_TIME) {
        hours.toString() + TIME_SEP + minutePart + TIME_SEP + secondPart
    } else {
        minutePart + TIME_SEP + secondPart
    }
}

private const val AUTO_HIDE_TIMEOUT_MS = 3_000L
private const val NO_TIME = 0L
private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L
private const val PAD_WIDTH = 2
private const val PAD_CHAR = '0'
private const val TIME_SEP = ":"

/** Minimal controller (MS-012): play/pause, seek slider, elapsed/total. */
@Composable
fun PlayerControls(
    state: PlayerUiState,
    playing: Boolean,
    controller: PlayerController,
    onSeek: (Float) -> Unit
) {
    Column {
        Slider(
            value = state.progress,
            onValueChange = onSeek,
            modifier = Modifier.semantics { contentDescription = SEEK_DESCRIPTION }
        )
        Row {
            IconButton(
                onClick = { if (playing) controller.pause() else controller.play() },
                modifier = Modifier.semantics {
                    contentDescription = if (playing) PAUSE_DESCRIPTION else PLAY_DESCRIPTION
                }
            ) {
                // Glyphs are interim; Material Symbols land with the icon pass (MS-074).
                Text(text = if (playing) "❚❚" else "▶")
            }
            Text(text = formatTime(state.positionMs) + " / " + formatTime(state.durationMs))
        }
    }
}

private const val SEEK_DESCRIPTION = "Seek position"
private const val PAUSE_DESCRIPTION = "Pause"
private const val PLAY_DESCRIPTION = "Play"
