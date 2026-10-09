package com.example.auplayer.player

import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.auplayer.core.EnhanceMode
import com.example.auplayer.enhance.EnhanceController
import kotlinx.coroutines.delay

/**
 * Real player screen: SurfaceView (vo presents directly, `wid` at attach),
 * tap-toggles-controls with auto-hide, play/pause + seek + time, back, and
 * the Enhance sheet. UI state flows from engine events (§5.2); gesture
 * drags beyond tap land next (overlay model already exists).
 */
@Composable
@Suppress("LongMethod")
fun PlayerScreen(
    uri: String,
    engine: PlayerEngine,
    enhance: EnhanceController,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onProgress: (positionMs: Long, durationMs: Long) -> Unit = { _, _ -> }
) {
    val surfaces = remember(engine) { SurfaceHost(engine) }
    var surfaceReady by remember { mutableStateOf(false) }
    var controls by remember { mutableStateOf(true) }
    var showSheet by remember { mutableStateOf(false) }
    var enhanceMode by remember { mutableStateOf(EnhanceMode.OFF) }
    var videoHeight by remember { mutableStateOf(0) }
    val autoHide = remember { AutoHide() }
    val uiState by rememberPlayerUiState(engine)
    val playing = uiState.state == PlayerState.Playing
    val controller = remember(engine) { EnginePlayerController(engine) }

    LaunchedEffect(uri, surfaceReady) {
        if (surfaceReady) {
            Log.i("AuPlayer", "open $uri")
            engine.open(uri)
        }
    }
    LaunchedEffect(Unit) {
        autoHide.interact(System.currentTimeMillis())
        while (true) {
            delay(AUTO_TICK_MS)
            controls = autoHide.visible(System.currentTimeMillis())
        }
    }
    LaunchedEffect(showSheet) {
        if (showSheet) {
            videoHeight = (engine.bridge as? LibmpvBridge)?.property("height")?.toIntOrNull() ?: 0
        }
    }
    ReportProgress(uri = uri, uiState = uiState, onProgress = onProgress)
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    autoHide.interact(System.currentTimeMillis())
                    controls = true
                }
            }
    ) {
        AndroidView(
            factory = { context ->
                SurfaceView(context).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            surfaces.onSurfaceCreated(holder.surface)
                            surfaceReady = usableFrame(holder) != null
                        }

                        override fun surfaceChanged(
                            holder: SurfaceHolder,
                            format: Int,
                            width: Int,
                            height: Int
                        ) {
                            surfaces.onSurfaceCreated(holder.surface)
                            surfaceReady = usableFrame(holder) != null
                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            surfaceReady = false
                            surfaces.onSurfaceDestroyed()
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        if (controls) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onBack) { Text("Back") }
                    Text(
                        text = uri.substringAfterLast('/'),
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showSheet = true }) {
                        Text(text = "Enhance: ${enhanceMode.name}")
                    }
                }
                Box(modifier = Modifier.weight(1f))
                PlayerControls(
                    state = uiState,
                    playing = playing,
                    controller = controller,
                    onSeek = { fraction ->
                        val durationSec = uiState.durationMs / MILLIS_PER_SECOND.toFloat()
                        if (durationSec > 0) {
                            engine.seekTo((fraction * durationSec).toDouble())
                        }
                    }
                )
            }
        }
        if (showSheet) {
            EnhanceSheetOverlay(
                selected = enhanceMode,
                lowered = enhance.currentRung > 0,
                sourceHeightPx = videoHeight,
                onSelect = { mode ->
                    enhanceMode = mode
                    enhance.apply(mode, videoHeight)
                    showSheet = false
                },
                onDismiss = { showSheet = false }
            )
        }
    }
}

/** Throttled resume-point reporting (MS-019): every 5 s of progress + on dispose. */
@Composable
private fun ReportProgress(
    uri: String,
    uiState: PlayerUiState,
    onProgress: (Long, Long) -> Unit
) {
    var lastSaved by remember(uri) { mutableStateOf(0L) }
    LaunchedEffect(uiState.positionMs, uiState.durationMs) {
        if (uiState.durationMs > 0 && uiState.positionMs - lastSaved >= SAVE_EVERY_MS) {
            lastSaved = uiState.positionMs
            onProgress(uiState.positionMs, uiState.durationMs)
        }
    }
    DisposableEffect(uri) {
        onDispose {
            if (uiState.durationMs > 0 && uiState.positionMs > 0) {
                onProgress(uiState.positionMs, uiState.durationMs)
            }
        }
    }
}

private const val SAVE_EVERY_MS = 5_000L

/** Collects engine events into one UI snapshot (§5.2). */
@Composable
fun rememberPlayerUiState(engine: PlayerEngine): State<PlayerUiState> {
    val state = remember(engine) { mutableStateOf(PlayerUiState()) }
    LaunchedEffect(engine) {
        var paused = false
        engine.events.collect { event ->
            val current = state.value
            state.value = when (event) {
                is MpvEvent.TimePos -> current.copy(
                    positionMs = (event.seconds * MILLIS_PER_SECOND).toLong(),
                    state = if (paused) PlayerState.Paused else PlayerState.Playing
                )
                is MpvEvent.Duration -> current.copy(
                    durationMs = (event.seconds * MILLIS_PER_SECOND).toLong()
                )
                is MpvEvent.Paused -> {
                    paused = event.paused
                    current.copy(
                        state = if (event.paused) PlayerState.Paused else PlayerState.Playing
                    )
                }
                is MpvEvent.Eof -> current.copy(state = PlayerState.Idle, positionMs = 0)
                is MpvEvent.Error -> current.copy(state = PlayerState.Error)
            }
        }
    }
    return state
}

@Composable
private fun EnhanceSheetOverlay(
    selected: EnhanceMode,
    lowered: Boolean,
    sourceHeightPx: Int,
    onSelect: (EnhanceMode) -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        EnhanceSheet(
            selected = selected,
            lowered = lowered,
            sourceHeightPx = sourceHeightPx,
            onSelect = onSelect
        )
        Button(onClick = onDismiss) { Text("Close") }
    }
}

private data class Frame(val surface: Surface, val width: Int, val height: Int)

private fun usableFrame(holder: SurfaceHolder): Frame? {
    val surface = holder.surface
    if (surface == null || !surface.isValid) return null
    return sizedFrame(holder, surface)
}

private fun sizedFrame(holder: SurfaceHolder, surface: Surface): Frame? {
    val frame = holder.surfaceFrame
    if (frame.width() <= 0 || frame.height() <= 0) return null
    return Frame(surface, frame.width(), frame.height())
}

private const val AUTO_TICK_MS = 250L
private const val MILLIS_PER_SECOND = 1000L
