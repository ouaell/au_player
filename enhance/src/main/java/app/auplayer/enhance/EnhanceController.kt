package app.auplayer.enhance

import app.auplayer.aucore.Diag
import app.auplayer.aucore.EnhanceMode
import app.auplayer.aucore.MpvCommandException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Outcome of an enhance application (MS-028). */
sealed interface EnhanceResult {
    data object Applied : EnhanceResult
    data object Cleared : EnhanceResult
    data class Unavailable(val reason: String) : EnhanceResult
    data class Failed(val rung: Int) : EnhanceResult
}

/**
 * Applies built chains to mpv (§6.3). Takes a plain command sender (wired to
 * `PlayerEngine::send` in AppContainer) so `:enhance` never depends on
 * `:player` — no module cycle. Command shapes (`change-list glsl-shaders
 * set/clr`, `:` separator) are per §6.3 and get proven on native by the
 * MS-059 shader suite. Shader compile cache dir is set at startup (MS-008);
 * first-activation "Preparing…" is UI-owned (MS-029).
 */
class EnhanceController(
    private val send: (List<String>) -> Unit,
    private val pathResolver: (String) -> String = { it },
) {
    var currentMode: EnhanceMode = EnhanceMode.OFF
        private set
    var currentRung: Int = 0
        private set

    fun apply(mode: EnhanceMode, sourceHeightPx: Int, rung: Int = 0): EnhanceResult {
        val passes = ShaderChainBuilder.build(mode, sourceHeightPx, rung)
        if (passes.isEmpty()) {
            clear()
            return if (mode == EnhanceMode.OFF) {
                EnhanceResult.Cleared
            } else {
                EnhanceResult.Unavailable("no chain for $mode rung $rung at ${sourceHeightPx}p")
            }
        }
        return try {
            send(listOf("set", "scale", "lanczos"))
            send(listOf("set", "dscale", "mitchell"))
            send(listOf("set", "correct-downscaling", "yes"))
            send(listOf("set", "linear-downscaling", "no"))
            val paths = passes.joinToString(GLSL_SEPARATOR) { pathResolver(it.assetPath) }
            send(listOf("change-list", "glsl-shaders", "set", paths))
            currentMode = mode
            currentRung = rung
            EnhanceResult.Applied
        } catch (error: MpvCommandException) {
            Diag.record("enhance", "apply failed rung $rung: ${error.message}")
            clearBestEffort()
            EnhanceResult.Failed(rung)
        }
    }

    suspend fun applyAsync(
        mode: EnhanceMode,
        sourceHeightPx: Int,
        rung: Int = 0,
    ): EnhanceResult = withContext(Dispatchers.Default) {
        apply(mode, sourceHeightPx, rung)
    }

    fun clear() {
        send(listOf("change-list", "glsl-shaders", "clr", ""))
        send(listOf("set", "scale", "bilinear"))
        send(listOf("set", "dscale", "bilinear"))
        currentMode = EnhanceMode.OFF
        currentRung = 0
    }

    /** Best-effort reset on the failure path: a broken bridge must not throw twice. */
    private fun clearBestEffort() {
        try {
            clear()
        } catch (error: MpvCommandException) {
            Diag.record("enhance", "clear also failed: " + error.message.toString())
        }
    }
}

private const val GLSL_SEPARATOR = ":"
