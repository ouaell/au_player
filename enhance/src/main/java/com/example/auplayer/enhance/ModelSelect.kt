package com.example.auplayer.enhance

/**
 * Model selection (MS-046): ONE compact 2× Real-ESRGAN model for 1.0
 * (realesr-animevideov3 family; weights license verified in release notes
 * before packaging — see docs/assets-licenses.md). Upscale only when the
 * shorter edge is below 1.5× the display width.
 */
object ModelSelect {
    const val MODEL_ID = "realesr-animevideov3-x2-compact"

    fun needsUpscale(shortEdgePx: Int, displayWidthPx: Int): Boolean {
        return shortEdgePx < UPSCALE_FACTOR * displayWidthPx
    }
}

private const val UPSCALE_FACTOR = 1.5f
