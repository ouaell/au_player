package app.auplayer.enhance

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import java.io.File

/**
 * Guards the asset-path -> staged-path mapping. Getting this wrong is silent
 * at build time and only shows up as "Cannot open file" from mpv at runtime,
 * which is exactly how the doubled `shaders/patched/` segment shipped once.
 */
class ShaderStagerPathTest {

    private fun stager(filesDir: File): ShaderStager {
        val context = mock(Context::class.java)
        org.mockito.Mockito.`when`(context.filesDir).thenReturn(filesDir)
        return ShaderStager(context)
    }

    @Test
    fun `resolve does not repeat the asset root segment`() {
        val filesDir = File("/data/user/0/app/files")
        val resolved = stager(filesDir).resolve("shaders/patched/Anime4K_Upscale_CNN_x2_M.glsl")
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
        val filesDir = File("/data/user/0/app/files")
        val subject = stager(filesDir)
        listOf(
            "shaders/patched/Anime4K_Clamp_Highlights.glsl",
            "shaders/patched/Anime4K_Restore_CNN_M.glsl",
            "shaders/patched/Anime4K_Upscale_CNN_x2_M.glsl",
            "shaders/patched/Anime4K_Upscale_CNN_x2_S.glsl",
            "shaders/patched/ravu-lite-r4.hook",
        ).forEach { pass ->
            assertTrue(
                "outside filesDir: $pass",
                subject.resolve(pass).startsWith(filesDir.path),
            )
        }
    }

    @Test
    fun `every anime chain pass resolves to a distinct file`() {
        val filesDir = File("/data/user/0/app/files")
        val subject = stager(filesDir)
        val resolved = ShaderChainBuilder
            .build(app.auplayer.aucore.EnhanceMode.ANIME, sourceHeightPx = 720)
            .map { subject.resolve(it.assetPath) }
        assertEquals(resolved.size, resolved.distinct().size)
        assertTrue(resolved.isNotEmpty())
    }
}