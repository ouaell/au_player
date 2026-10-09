package com.example.auplayer.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Grid invariants over random page/tile/overlap shapes (MS-064). */
class TilingPropertyTest {
    @Test
    fun `tiles cover the page exactly and stay in bounds`() {
        val random = Random(0x711EL)
        repeat(300) {
            val pageW = random.nextInt(1, 22000)
            val pageH = random.nextInt(1, 22000)
            val tile = random.nextInt(64, 2048)
            val overlap = random.nextInt(0, 64)
            val grid = Tiling.grid(pageW, pageH, tile, overlap)
            assertTrue("empty grid", grid.isNotEmpty())
            for (rect in grid) {
                assertTrue("left bound", rect.left >= 0)
                assertTrue("top bound", rect.top >= 0)
                assertTrue("right bound", rect.right <= pageW)
                assertTrue("bottom bound", rect.bottom <= pageH)
                assertTrue("non-empty", rect.right > rect.left && rect.bottom > rect.top)
            }
            // First tile starts at origin, last tile reaches the far edges.
            val first = grid.first()
            assertEquals(0, first.left)
            assertEquals(0, first.top)
            assertEquals(pageW, grid.maxOf { it.right })
            assertEquals(pageH, grid.maxOf { it.bottom })
        }
    }

    @Test
    fun `single tile degenerates to the full page`() {
        val grid = Tiling.grid(800, 1200, 2048, 0)
        assertEquals(1, grid.size)
        assertEquals(Tile(0, 0, 800, 1200), grid.first())
    }
}
