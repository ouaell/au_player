package com.example.auplayer.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TilingTest {
    @Test
    fun `single tile covers small pages`() {
        assertEquals(listOf(Tile(0, 0, 100, 100)), Tiling.grid(100, 100, 256, 16))
    }

    @Test
    fun `overlap grid clamps to edges`() {
        assertEquals(
            listOf(
                Tile(0, 0, 64, 64),
                Tile(56, 0, 100, 64),
                Tile(0, 56, 64, 100),
                Tile(56, 56, 100, 100)
            ),
            Tiling.grid(100, 100, 64, 8)
        )
    }

    @Test
    fun `webtoon strip tiles in one column`() {
        val tiles = Tiling.grid(1500, 22000, 2048, 16)
        assertEquals(11, tiles.size)
        assertEquals(Tile(0, 0, 1500, 2048), tiles.first())
        assertEquals(22000, tiles.last().bottom)
        assertEquals(0, tiles.last().left)
    }

    @Test
    fun `bad geometry throws`() {
        assertThrows(IllegalArgumentException::class.java) { Tiling.grid(0, 10, 64, 8) }
        assertThrows(IllegalArgumentException::class.java) { Tiling.grid(100, 100, 64, 64) }
    }
}
