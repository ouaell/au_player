package com.example.auplayer.player

import android.view.Surface

/**
 * Render thread (Push B2): owns EGL + render context for one surface,
 * redraws only when mpv reports a new frame. Started/stopped by the view
 * layer (SurfaceHolder callbacks); E1 drives it headlessly.
 */
class RenderLoop(
    private val bridge: LibmpvBridge,
    private val egl: EglHost = EglHost()
) {
    @Volatile
    private var running = false
    private var thread: Thread? = null

    fun start(surface: Surface, width: Int, height: Int): Boolean {
        if (!egl.init() || !egl.makeWindowSurface(surface)) {
            egl.release()
            return false
        }
        if (bridge.renderInit() != 0) {
            egl.release()
            return false
        }
        running = true
        thread = Thread({
            while (running) {
                if (bridge.checkUpdate()) {
                    if (egl.makeCurrent() && bridge.renderFrame(width, height) == 0) {
                        egl.swapBuffers()
                    }
                } else {
                    try {
                        Thread.sleep(POLL_MS)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                    }
                }
            }
        }, "mpv-render").apply { isDaemon = true; start() }
        return true
    }

    fun stop() {
        running = false
        try {
            thread?.join(JOIN_MS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        thread = null
        egl.release()
    }
}

private const val POLL_MS = 8L
private const val JOIN_MS = 1_000L
