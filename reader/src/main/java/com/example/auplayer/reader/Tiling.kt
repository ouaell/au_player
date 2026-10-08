package com.example.auplayer.reader

/** Pixel rect (MS-035). Shared by PDF zoom tiles, webtoon strips and NCNN. */
data class Tile(val left: Int, val top: Int, val right: Int, val bottom: Int)

/**
 * Overlap tile grid (MS-035). Row-major, edges clamp exactly to the page so
 * stitching is gap-free; overlap blends seams (linear blend lands in MS-047).
 */
object Tiling {
    fun grid(pageW: Int, pageH: Int, tile: Int, overlap: Int): List<Tile> {
        require(pageW > 0 && pageH > 0) { "bad page $pageW x $pageH" }
        require(tile > overlap && overlap >= 0) { "bad tile $tile overlap $overlap" }
        val step = tile - overlap
        val xs = positions(pageW, step)
        val ys = positions(pageH, step)
        return ys.flatMap { y ->
            xs.map { x ->
                Tile(x, y, minOf(x + tile, pageW), minOf(y + tile, pageH))
            }
        }
    }

    private fun positions(length: Int, step: Int): List<Int> {
        val starts = mutableListOf<Int>()
        var at = 0
        while (at < length) {
            starts += at
            at += step
        }
        return starts
    }
}
