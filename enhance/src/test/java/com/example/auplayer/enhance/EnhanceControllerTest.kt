package com.example.auplayer.enhance

import com.example.auplayer.core.EnhanceMode
import com.example.auplayer.player.FakeMpvBridge
import com.example.auplayer.player.MpvBridge
import com.example.auplayer.player.MpvCommandException
import com.example.auplayer.player.PlayerEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnhanceControllerTest {
    private fun controller(
        bridge: FakeMpvBridge = FakeMpvBridge()
    ): Pair<EnhanceController, FakeMpvBridge> {
        val engine = PlayerEngine(bridge, Dispatchers.Unconfined)
        return EnhanceController(engine) to bridge
    }

    @Test
    fun `anime applies scale opts then chain`() {
        val (controller, bridge) = controller()
        assertEquals(EnhanceResult.Applied, controller.apply(EnhanceMode.ANIME, 1080))
        val commands = bridge.commands.map { it.joinToString(" ") }
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
        val (controller, bridge) = controller()
        controller.apply(EnhanceMode.ANIME, 1080)
        assertEquals(EnhanceResult.Cleared, controller.apply(EnhanceMode.OFF, 1080))
        assertTrue(bridge.commands.last().joinToString(" ") == "set dscale bilinear")
    }

    @Test
    fun `oversize and unknown rungs are unavailable`() {
        val (controller, bridge) = controller()
        val result = controller.apply(EnhanceMode.ANIME, 2160)
        assertTrue(result is EnhanceResult.Unavailable)
        assertEquals(EnhanceMode.OFF, controller.currentMode)
        assertTrue(bridge.commands.any { it.contains("clr") })
    }

    @Test
    fun `bridge failure clears and reports the rung`() {
        val failing = object : MpvBridge by FakeMpvBridge() {
            override fun command(args: List<String>): Unit =
                throw MpvCommandException("gl exploded")
        }
        val engine = PlayerEngine(failing, Dispatchers.Unconfined)
        val controller = EnhanceController(engine)
        assertEquals(EnhanceResult.Failed(0), controller.apply(EnhanceMode.ANIME, 1080))
    }

    @Test
    fun `async apply matches sync apply`() = runBlocking {
        val (controller, bridge) = controller()
        assertEquals(EnhanceResult.Applied, controller.applyAsync(EnhanceMode.STANDARD, 720))
        assertTrue(
            bridge.commands.any { command -> command.any { it.contains("FSRCNNX_x2_8-0-4-1") } }
        )
    }
}
