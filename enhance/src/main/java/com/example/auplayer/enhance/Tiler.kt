package com.example.auplayer.enhance

import com.example.auplayer.reader.Tile
import com.example.auplayer.reader.Tiling

/**
 * Overlap tiler/stitcher (MS-047). Tiles come from the shared [Tiling] grid;
 * overlap seams blend linearly. The "model" here is injectable so the golden
 * test runs with a stub (nearest ×2); the real NCNN model plugs the same
 * function shape on device.
 */
object Tiler {
    fun split(
        pixels: IntArray,
        width: Int,
        height: Int,
        tile: Int,
        overlap: Int
    ): List<TilePixels> {
        return Tiling.grid(width, height, tile, overlap).map { rect ->
            TilePixels(rect, extract(pixels, width, rect))
        }
    }

    fun stitch(
        tiles: List<TilePixels>,
        width: Int,
        height: Int,
        overlap: Int,
        scale: Int
    ): IntArray {
        val outW = width * scale
        val outH = height * scale
        val out = IntArray(outW * outH)
        val weight = IntArray(outW * outH)
        tiles.forEach { tilePixels ->
            splatTile(out, weight, outW, tilePixels, overlap, scale)
        }
        for (index in out.indices) {
            val total = weight[index]
            out[index] = if (total > 0) out[index] / total else 0
        }
        return out
    }

    private fun splatTile(
        out: IntArray,
        weight: IntArray,
        outW: Int,
        tilePixels: TilePixels,
        overlap: Int,
        scale: Int
    ) {
        val rect = tilePixels.rect
        val tileW = (rect.right - rect.left) * scale
        for (oy in rect.top * scale until rect.bottom * scale) {
            for (ox in rect.left * scale until rect.right * scale) {
                val lx = ox - rect.left * scale
                val ly = oy - rect.top * scale
                val px = tilePixels.pixels[ly * tileW + lx]
                val blend = blendWeight(ox / scale, oy / scale, rect, overlap)
                val at = oy * outW + ox
                out[at] = out[at] + px * blend
                weight[at] = weight[at] + blend
            }
        }
    }

    /** Stub "model" (MS-047 golden): nearest-neighbor ×[scale]. */
    fun upscaleNearest(pixels: IntArray, width: Int, height: Int, scale: Int = 2): IntArray {
        val out = IntArray(width * scale * height * scale)
        for (y in 0 until height) {
            for (x in 0 until width) {
                splatPixel(out, width, x, y, pixels[y * width + x], scale)
            }
        }
        return out
    }

    private fun splatPixel(out: IntArray, width: Int, x: Int, y: Int, px: Int, scale: Int) {
        for (sy in 0 until scale) {
            for (sx in 0 until scale) {
                out[(y * scale + sy) * width * scale + x * scale + sx] = px
            }
        }
    }

    private fun extract(pixels: IntArray, width: Int, rect: Tile): TilePixels {
        val tileW = rect.right - rect.left
        val tileH = rect.bottom - rect.top
        val tilePixels = IntArray(tileW * tileH)
        for (y in rect.top until rect.bottom) {
            for (x in rect.left until rect.right) {
                tilePixels[(y - rect.top) * tileW + (x - rect.left)] = pixels[y * width + x]
            }
        }
        return TilePixels(rect, tilePixels, tileW, tileH)
    }

    private fun blendWeight(x: Int, y: Int, rect: Tile, overlap: Int): Int {
        if (overlap <= 0) return FULL_WEIGHT
        val left = x - rect.left
        val right = rect.right - 1 - x
        val edge = minOf(left, right, y - rect.top, rect.bottom - 1 - y, overlap)
        return edge + 1
    }
}

// Array payload: identity is never compared (tests assert contents).
@Suppress("ArrayInDataClass")
data class TilePixels(val rect: Tile, val pixels: IntArray, val width: Int, val height: Int) {
    constructor(rect: Tile, pixels: IntArray) : this(
        rect,
        pixels,
        rect.right - rect.left,
        rect.bottom - rect.top
    )
}

private const val FULL_WEIGHT = 1
