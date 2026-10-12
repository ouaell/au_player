package app.auplayer.enhance

import app.auplayer.aucore.EnhanceMode
import app.auplayer.aucore.MpvCommandException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Local command sink (no :player dep in :enhance tests). */
class FakeSender(var fail: Boolean = false) : (List<String>) -> Unit {
    val sent = mutableListOf<List<String>>()
    override fun invoke(args: List<String>) {
        if (fail) throw MpvCommandException("gl exploded")
        sent += args
    }
}

class EnhanceControllerTest {
    private fun controller(
        sender: FakeSender = FakeSender()
    ): Pair<EnhanceController, FakeSender> {
        return EnhanceController(sender) to sender
    }

    @Test
    fun `anime applies scale opts then chain`() {
        val (controller, sender) = controller()
        assertEquals(EnhanceResult.Applied, controller.apply(EnhanceMode.ANIME, 1080))
        val commands = sender.sent.map { it.joinToString(" ") }
        assertEquals(
            listOf(
                "set scale lanczos",
                "set dscale mitchell",
                "set correct-downscaling yes",
                "set linear-downscaling no",
                "change-list glsl-shaders set " +
                    "shaders/patched/Anime4K_Clamp_Highlights.glsl:" +
                    "shaders/patched/Anime4K_Restore_CNN_M.glsl:" +
                    "shaders/patched/Anime4K_Upscale_CNN_x2_M.glsl"
            ),
            commands
        )
        assertEquals(EnhanceMode.ANIME, controller.currentMode)
    }

    @Test
    fun `off clears and restores bilinear`() {
        val (controller, sender) = controller()
        controller.apply(EnhanceMode.ANIME, 1080)
        assertEquals(EnhanceResult.Cleared, controller.apply(EnhanceMode.OFF, 1080))
        assertTrue(sender.sent.last().joinToString(" ") == "set dscale bilinear")
    }

    @Test
    fun `oversize and unknown rungs are unavailable`() {
        val (controller, sender) = controller()
        val result = controller.apply(EnhanceMode.ANIME, 2160)
        assertTrue(result is EnhanceResult.Unavailable)
        assertEquals(EnhanceMode.OFF, controller.currentMode)
        assertTrue(sender.sent.any { it.contains("clr") })
    }

    @Test
    fun `bridge failure clears and reports the rung`() {
        val (controller, _) = controller(FakeSender(fail = true))
        assertEquals(EnhanceResult.Failed(0), controller.apply(EnhanceMode.ANIME, 1080))
    }

    @Test
    fun `async apply matches sync apply`() = runBlocking {
        val (controller, sender) = controller()
        assertEquals(EnhanceResult.Applied, controller.applyAsync(EnhanceMode.STANDARD, 720))
        assertTrue(
            sender.sent.any { command -> command.any { it.contains("ravu-lite-r4") } }
        )
    }
}
