package app.auplayer.player

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.auplayer.aucore.EnhanceMode
import app.auplayer.enhance.EnhanceController
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
    handle: PlayerHandle,
    modifier: Modifier = Modifier
) {
    val engine = handle.engine
    val enhance = handle.enhance
    val surfaces = remember(engine) { SurfaceHost(engine) }
    var surfaceReady by remember { mutableStateOf(false) }
    var openedUri by remember { mutableStateOf<String?>(null) }
    var controls by remember { mutableStateOf(true) }
    var showSheet by remember { mutableStateOf(false) }
    var enhanceMode by remember { mutableStateOf(EnhanceMode.OFF) }
    var videoHeight by remember { mutableStateOf(0) }
    val autoHide = remember { AutoHide() }
    val uiState by rememberPlayerUiState(engine)
    val playing = uiState.state == PlayerState.Playing
    val controller = remember(engine) { EnginePlayerController(engine) }

    LaunchedEffect(uri, surfaceReady) {
        // Open once per surface generation: a transient destroy/create cycle
        // re-arms via openedUri=null below; created+changed without destroy
        // must NOT double-load (second loadfile interrupts the first).
        if (surfaceReady && openedUri != uri) {
            openedUri = uri
            Log.i("AuPlayer", "open $uri")
            engine.open(uri)
        }
    }
    LaunchedEffect(Unit) {
        autoHide.interact(System.currentTimeMillis())
        while (true) {
            delay(AUTO_TICK_MS)
            val visible = autoHide.visible(System.currentTimeMillis())
            if (visible != controls) {
                Log.i("AuPlayer", "controls tick visible=$visible")
            }
            controls = visible
        }
    }
    LaunchedEffect(showSheet) {
        if (showSheet) {
            videoHeight = (engine.bridge as? LibmpvBridge)?.property("height")?.toIntOrNull() ?: 0
        }
    }
    ReportProgress(uri = uri, uiState = uiState, onProgress = handle.onProgress)
    Box(
        modifier = modifier.fillMaxSize()
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
                            openedUri = null
                            surfaces.onSurfaceDestroyed()
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        // Transparent tap catcher above the native view (which consumes all
        // touches): taps toggle controls. Below the controls so buttons win.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures {
                        Log.i("AuPlayer", "tap: show controls")
                        autoHide.interact(System.currentTimeMillis())
                        controls = true
                    }
                }
        )
        if (controls) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp).statusBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = handle.onBack,
                        modifier = Modifier.semantics(mergeDescendants = true) {
                            contentDescription = BACK_DESCRIPTION

                        }
                    ) { Text(BACK_DESCRIPTION) }
                    Text(
                        text = uri.substringAfterLast('/'),
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { showSheet = true },
                        modifier = Modifier.semantics(mergeDescendants = true) {
                            contentDescription = "Enhance: ${enhanceMode.name}"

                        }
                    ) {
                        Text(text = "Enhance: ${enhanceMode.name}")
                    }
                }
                Box(modifier = Modifier.weight(1f))
                // Above the navigation bar: content under an opaque nav bar is
                // invisible to touches and dropped from the a11y tree (E8).
                Box(modifier = Modifier.navigationBarsPadding()) {
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
    // DisposableEffect closures freeze at last run: track the latest snapshot
    // explicitly or dispose always sees zeros (E9).
    val latest by rememberUpdatedState(uiState)
    DisposableEffect(uri) {
        onDispose {
            Log.i(
                "E9GATE",
                "report dispose pos=${latest.positionMs} dur=${latest.durationMs}"
            )
            if (latest.durationMs > 0 && latest.positionMs > 0) {
                onProgress(latest.positionMs, latest.durationMs)
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
        var lastTickMs = 0L
        engine.events.collect { event ->
            val current = state.value
            state.value = when (event) {
                is MpvEvent.TimePos -> {
                    // 2 Hz UI ticks: per-frame recomposition churns the a11y
                    // tree (flaky dumps) and wastes battery; playback clock
                    // itself is unaffected (trace is unthrottled).
                    val now = System.currentTimeMillis()
                    if (now - lastTickMs >= UI_TICK_MS) {
                        lastTickMs = now
                        current.copy(
                            positionMs = (event.seconds * MILLIS_PER_SECOND).toLong(),
                            state = if (paused) PlayerState.Paused else PlayerState.Playing
                        )
                    } else {
                        current
                    }
                }
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
private const val UI_TICK_MS = 500L
private const val BACK_DESCRIPTION = "Back"
