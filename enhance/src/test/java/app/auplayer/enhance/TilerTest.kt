package app.auplayer.enhance

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class TilerTest {
    private val width = 100
    private val height = 80
    private val pixels = IntArray(width * height) { it }

    @Test
    fun `split covers every pixel once per tile`() {
        val tiles = Tiler.split(pixels, width, height, 64, 8)
        assertEquals(4, tiles.size)
        val first = tiles.first()
        assertEquals(64, first.width)
        assertEquals(64, first.height)
        assertEquals(pixels[0], first.pixels[0])
    }

    @Test
    fun `stub model doubles dimensions`() {
        val doubled = Tiler.upscaleNearest(pixels, width, height, 2)
        assertEquals(width * 2 * height * 2, doubled.size)
        assertEquals(pixels[0], doubled[0])
        assertEquals(pixels[0], doubled[1])
        assertEquals(pixels[width], doubled[width * 4])
    }

    @Test
    fun `stitched tiles equal direct upscale`() {
        val tiles = Tiler.split(pixels, width, height, 64, 8).map { tile ->
            val up = Tiler.upscaleNearest(tile.pixels, tile.width, tile.height, 2)
            tile.copy(pixels = up)
        }
        val stitched = Tiler.stitch(tiles, width, height, 8, 2)
        assertArrayEquals(Tiler.upscaleNearest(pixels, width, height, 2), stitched)
    }
}
