package com.example.auplayer.player

import android.util.Log
import com.example.auplayer.core.Diag
import com.example.auplayer.core.MpvCommandException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executors

/**
 * Single mpv owner for the whole app (§3.4: wrapper is process-wide; exactly
 * one engine lives in [AppContainer][com.example.auplayer.AppContainer]).
 * All bridge calls go through one single-thread dispatcher; native events
 * are re-emitted on [events]. State machine + init options land in
 * MS-011/MS-008; surface lifecycle in MS-009.
 */
class PlayerEngine(
    val bridge: MpvBridge,
    dispatcher: CoroutineDispatcher = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "mpv-cmd").apply { isDaemon = true }
    }.asCoroutineDispatcher(),
    /** Bound on waiting for a surface before loadfile (S1-001). */
    private val openSurfaceTimeoutMs: Long = OPEN_SURFACE_TIMEOUT_MS,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val pending = MutableSharedFlow<MpvEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<MpvEvent> = pending.asSharedFlow()

    /** Completes when the bridge initialized; commands wait for it (no races). */
    private val ready = CompletableDeferred<Unit>()

    init {
        bridge.setEventListener { event ->
            scope.launch { pending.emit(event) }
        }
        scope.launch {
            try {
                bridge.init()
                ready.complete(Unit)
            } catch (error: MpvCommandException) {
                Diag.record("mpv", "init failed: ${error.message}")
                ready.completeExceptionally(error)
            }
        }
    }

    fun init() {
        scope.launch { bridge.init() }
    }

    private fun cmd(block: suspend () -> Unit) {
        scope.launch {
            try {
                ready.await()
                block()
            } catch (error: MpvCommandException) {
                // JVM-safe: only executes on real command failures (never in unit tests).
                Log.e("AuPlayer", "command dropped: " + error.message)
                Diag.record("player", "command dropped: ${error.message}")
            }
        }
    }

    fun open(uri: String) {
        cmd {
            // Never let vo init run without a live surface: loadfile is what
            // triggers mpv's vo init, and a wid pointing at a deleted ref
            // aborts the process (S1-001). Bounded wait, then proceed anyway
            // (wid=0 degrades to audio-only, never crashes).
            if (!surfaceArmed) {
                Log.i("AuPlayer", "open waiting for surface")
                val armed = withTimeoutOrNull(openSurfaceTimeoutMs) {
                    while (!surfaceArmed) delay(OPEN_SURFACE_POLL_MS)
                    true
                } ?: false
                if (!armed) {
                    Log.w("AuPlayer", "open proceeding without surface")
                    Diag.record("player", "open without surface")
                }
            }
            bridge.command(listOf("loadfile", uri))
        }
    }

    fun play() {
        cmd { bridge.command(listOf("set", "pause", "no")) }
    }

    fun pause() {
        cmd { bridge.command(listOf("set", "pause", "yes")) }
    }

    fun seekTo(seconds: Double) {
        cmd { bridge.command(listOf("seek", seconds.toString(), "absolute")) }
    }

    fun attach(token: Any?) {
        cmd {
            bridge.attach(token)
            surfaceArmed = token != null
        }
    }

    fun detach() {
        cmd {
            bridge.detach()
            surfaceArmed = false
        }
    }

    /** Track/prop/subtitle-file commands route here (MS-017/018); mapping lives
     *  in [TrackList], subtitleProps and delayProps so it stays JVM-tested. */
    fun send(args: List<String>) {
        cmd { bridge.command(args) }
    }

    fun release() {
        scope.cancel()
        bridge.destroy()
    }

    /**
     * Written only inside [cmd] blocks (single thread, ordered); read there
     * too. Volatile for test dispatchers that resume on other threads.
     */
    @Volatile
    private var surfaceArmed = false
}

private const val OPEN_SURFACE_TIMEOUT_MS = 5_000L
private const val OPEN_SURFACE_POLL_MS = 50L

/** [PlayerController] backed by the real engine (Fake covers tests). */
class EnginePlayerController(private val engine: PlayerEngine) : PlayerController {
    override fun play() = engine.play()
    override fun pause() = engine.pause()
}
