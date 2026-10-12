package app.auplayer.enhance

import app.auplayer.aucore.EnhanceMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the asset-path -> staged-path mapping. Getting this wrong is silent
 * at build time and only surfaces as "Cannot open file" from mpv at runtime,
 * which is exactly how a doubled `shaders/patched/` segment shipped once.
 */
class ShaderStagerPathTest {

    private val filesDir = File("/data/user/0/app/files")

    @Test
    fun `resolve does not repeat the asset root segment`() {
        val resolved = ShaderStager.resolveIn(
            filesDir,
            "shaders/patched/Anime4K_Upscale_CNN_x2_M.glsl",
        )
        assertEquals(
            File(filesDir, "shaders/patched/Anime4K_Upscale_CNN_x2_M.glsl").path,
            resolved,
        )
        assertTrue(
            "path must not contain a doubled segment: $resolved",
            !resolved.contains("patched/shaders/patched"),
        )
    }

    @Test
    fun `resolve places every chain pass inside filesDir`() {
        listOf(
            "shaders/patched/Anime4K_Clamp_Highlights.glsl",
            "shaders/patched/Anime4K_Restore_CNN_M.glsl",
            "shaders/patched/Anime4K_Upscale_CNN_x2_M.glsl",
            "shaders/patched/Anime4K_Upscale_CNN_x2_S.glsl",
            "shaders/patched/ravu-lite-r4.hook",
        ).forEach { pass ->
            assertTrue(
                "outside filesDir: $pass",
                ShaderStager.resolveIn(filesDir, pass).startsWith(filesDir.path),
            )
        }
    }

    @Test
    fun `every anime chain pass resolves to a distinct real file`() {
        val resolved = ShaderChainBuilder
            .build(EnhanceMode.ANIME, sourceHeightPx = 720)
            .map { ShaderStager.resolveIn(filesDir, it.assetPath) }
        assertTrue("chain was empty", resolved.isNotEmpty())
        assertEquals("passes collapsed onto one file", resolved.size, resolved.distinct().size)
    }

    @Test
    fun `every standard chain pass resolves to a distinct real file`() {
        val resolved = ShaderChainBuilder
            .build(EnhanceMode.STANDARD, sourceHeightPx = 720)
            .map { ShaderStager.resolveIn(filesDir, it.assetPath) }
        assertTrue("chain was empty", resolved.isNotEmpty())
        assertEquals(resolved.size, resolved.distinct().size)
    }

    @Test
    fun `resolved paths name a file the pack actually stages`() {
        val staged = listOf(
            "Anime4K_Clamp_Highlights.glsl",
            "Anime4K_Restore_CNN_M.glsl",
            "Anime4K_Upscale_CNN_x2_M.glsl",
            "Anime4K_Upscale_CNN_x2_S.glsl",
            "ravu-lite-r4.hook",
        ).map { File(filesDir, "${ShaderStager.ASSET_ROOT}/$it").path }.toSet()
        ShaderChainBuilder.build(EnhanceMode.ANIME, sourceHeightPx = 720)
            .plus(ShaderChainBuilder.build(EnhanceMode.STANDARD, sourceHeightPx = 720))
            .map { ShaderStager.resolveIn(filesDir, it.assetPath) }
            .forEach { resolved ->
                assertTrue("not a staged pack file: $resolved", resolved in staged)
            }
    }
}