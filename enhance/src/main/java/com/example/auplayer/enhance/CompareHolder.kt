package com.example.auplayer.enhance

import com.example.auplayer.core.EnhanceMode

/**
 * Press-and-hold compare (MS-031): snapshots (mode, rung), clears shaders
 * while held, restores on release. A rung change mid-hold is ignored for the
 * restore (snapshot wins), per §6.5.
 */
class CompareHolder(private val controller: EnhanceController) {
    private var snapshot: Snapshot? = null

    fun press(currentMode: EnhanceMode, currentRung: Int) {
        snapshot = Snapshot(currentMode, currentRung)
        controller.clear()
    }

    fun release(sourceHeightPx: Int) {
        val held = snapshot
        snapshot = null
        if (held != null && held.mode != EnhanceMode.OFF) {
            controller.apply(held.mode, sourceHeightPx, held.rung)
        }
    }

    fun holding(): Boolean = snapshot != null

    private data class Snapshot(val mode: EnhanceMode, val rung: Int)
}
