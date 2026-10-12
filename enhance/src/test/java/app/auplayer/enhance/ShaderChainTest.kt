package app.auplayer.enhance

import app.auplayer.aucore.EnhanceMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShaderChainTest {
    @Test
    fun `anime top rung is clamp restore upscale`() {
        assertEquals(
            listOf(
                ShaderPass("shaders/patched/Anime4K_Clamp_Highlights.glsl"),
                ShaderPass("shaders/patched/Anime4K_Restore_CNN_M.glsl"),
                ShaderPass("shaders/patched/Anime4K_Upscale_CNN_x2_M.glsl"),
            ),
            ShaderChainBuilder.build(EnhanceMode.ANIME, 1080),
        )
    }

    @Test
    fun `anime degrade drops restore for the small upscale`() {
        assertEquals(
            listOf(
                ShaderPass("shaders/patched/Anime4K_Clamp_Highlights.glsl"),
                ShaderPass("shaders/patched/Anime4K_Upscale_CNN_x2_S.glsl"),
            ),
            ShaderChainBuilder.build(EnhanceMode.ANIME, 1080, rung = 1),
        )
        assertTrue(ShaderChainBuilder.build(EnhanceMode.ANIME, 1080, rung = 2).isEmpty())
    }

    @Test
    fun `standard top rung is ravu lite`() {
        assertEquals(
            listOf(ShaderPass("shaders/patched/ravu-lite-r4.hook")),
            ShaderChainBuilder.build(EnhanceMode.STANDARD, 720),
        )
        assertTrue(ShaderChainBuilder.build(EnhanceMode.STANDARD, 720, rung = 1).isEmpty())
    }

    @Test
    fun `off and oversize sources yield no passes`() {
        assertTrue(ShaderChainBuilder.build(EnhanceMode.OFF, 720).isEmpty())
        assertTrue(ShaderChainBuilder.build(EnhanceMode.ANIME, 2160).isEmpty())
        assertFalse(ShaderChainBuilder.available(EnhanceMode.ANIME, 2160))
        assertTrue(ShaderChainBuilder.available(EnhanceMode.ANIME, 1440))
    }
}
