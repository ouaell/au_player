package com.example.auplayer.player

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Single mpv owner for the whole app (§3.4: wrapper is process-wide; exactly
 * one engine lives in [AppContainer][com.example.auplayer.AppContainer]).
 * All bridge calls go through one single-thread dispatcher; native events
 * are re-emitted on [events]. State machine + init options land in
 * MS-011/MS-008; surface lifecycle in MS-009.
 */
class PlayerEngine(
    private val bridge: MpvBridge,
    dispatcher: CoroutineDispatcher = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "mpv-cmd").apply { isDaemon = true }
    },
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val pending = MutableSharedFlow<MpvEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<MpvEvent> = pending.asSharedFlow()

    init {
        bridge.setEventListener { event ->
            scope.launch { pending.emit(event) }
        }
    }

    fun init() {
        scope.launch { bridge.init() }
    }

    fun open(uri: String) {
        scope.launch { bridge.command("loadfile", uri) }
    }

    fun play() {
        scope.launch { bridge.command("set", "pause", "no") }
    }

    fun pause() {
        scope.launch { bridge.command("set", "pause", "yes") }
    }

    fun seekTo(seconds: Double) {
        scope.launch { bridge.command("seek", seconds.toString(), "absolute") }
    }

    fun attach(token: Any?) {
        scope.launch { bridge.attach(token) }
    }

    fun detach() {
        scope.launch { bridge.detach() }
    }

    fun release() {
        scope.cancel()
        bridge.destroy()
    }
}

/** [PlayerController] backed by the real engine (Fake covers tests). */
class EnginePlayerController(private val engine: PlayerEngine) : PlayerController {
    override fun play() = engine.play()
    override fun pause() = engine.pause()
}
