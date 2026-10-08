package com.example.auplayer.player

import com.example.auplayer.core.Diag

/**
 * Enhance edge cases (MS-032): surface loss, seek, rotate and decoder
 * fallback must leave the engine in a safe, re-attachable state.
 */
class EnhanceEdgeCases(
    private val engine: PlayerEngine,
    private val surfaces: SurfaceHost
) {
    /** Surface lost mid-playback: detach now; next surface re-attaches cleanly. */
    fun onSurfaceLost() {
        surfaces.onSurfaceDestroyed()
        Diag.record("enhance", "surface lost; shaders stay armed for re-attach")
    }

    /** Seek is safe with or without a surface (commands just queue). */
    fun seekSafe(seconds: Double) {
        engine.seekTo(seconds)
    }

    /**
     * Hardware decoder failure → software fallback (§3.3 escape hatch):
     * switch decoding and record it. `hwdec-current` observation (MS-011
     * props) confirms the switch on device.
     */
    fun onDecoderError() {
        engine.send(listOf("set", "hwdec", "no"))
        Diag.record("decoder", "fallback to software decoding")
    }
}
