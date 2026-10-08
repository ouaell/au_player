package com.example.auplayer.enhance

import com.example.auplayer.core.EnhanceMode

/** One mpv `glsl-shaders` pass (asset path inside the APK). */
data class ShaderPass(val assetPath: String)

/**
 * Fixed chains (§6.3, MS-027). Exactly one 2× neural luma pass, then mpv
 * scales to the screen. `rung` 0 = top; higher rungs are governor steps down.
 * Unknown rungs and sources > 1440p yield no passes (Off).
 */
object ShaderChainBuilder {
    fun available(mode: EnhanceMode, sourceHeightPx: Int): Boolean {
        return mode != EnhanceMode.OFF && sourceHeightPx <= MAX_ENHANCE_HEIGHT
    }

    fun build(mode: EnhanceMode, sourceHeightPx: Int, rung: Int = 0): List<ShaderPass> {
        if (!available(mode, sourceHeightPx)) return emptyList()
        return when (mode) {
            EnhanceMode.OFF -> emptyList()
            EnhanceMode.ANIME -> animeRung(rung)
            EnhanceMode.STANDARD -> standardRung(rung)
        }
    }

    private fun animeRung(rung: Int): List<ShaderPass> {
        return when (rung) {
            0 -> listOf(
                ShaderPass(patched("Anime4K_Clamp_Highlights")),
                ShaderPass(patched("Anime4K_Restore_CNN_M")),
                ShaderPass(patched("Anime4K_Upscale_CNN_x2_M"))
            )
            1 -> listOf(
                ShaderPass(patched("Anime4K_Clamp_Highlights")),
                ShaderPass(patched("Anime4K_Upscale_CNN_x2_S"))
            )
            else -> emptyList()
        }
    }

    private fun standardRung(rung: Int): List<ShaderPass> {
        // rung 1 (FSR) ships once its source is license-verified (MS-026);
        // until then the governor steps straight to Off.
        return when (rung) {
            0 -> listOf(ShaderPass(patched("FSRCNNX_x2_8-0-4-1")))
            else -> emptyList()
        }
    }

    private fun patched(name: String): String = "shaders/patched/$name.glsl"
}

private const val MAX_ENHANCE_HEIGHT = 1440
