package com.example.auplayer.reader

/**
 * Decode budget (MS-039): decoded bitmaps capped at ~25% of memoryClass,
 * webtoon slices ≤ 2048 px tall (§7.3). BitmapFactory paths are device-only;
 * the budget math here is JVM-tested and shared by PDF/webtoon/NCNN.
 */
object MemoryBudget {
    fun maxBytes(memoryClassMb: Int): Long {
        return memoryClassMb * BYTES_PER_MB / QUARTER_DIVISOR
    }

    /** Power-of-two downsample so the decoded image covers [dstW]×[dstH]. */
    fun sampleSize(srcW: Int, srcH: Int, dstW: Int, dstH: Int): Int {
        if (!validSize(srcW, srcH) || !validSize(dstW, dstH)) return MIN_SAMPLE
        var sample = MIN_SAMPLE
        while (srcW / (sample * 2) >= dstW && srcH / (sample * 2) >= dstH) {
            sample *= 2
        }
        return sample
    }

    private fun validSize(width: Int, height: Int): Boolean = width > 0 && height > 0

    /** Webtoon slice count for a strip of [heightPx] at most 2048 px/slice. */
    fun webtoonSlices(heightPx: Int): Int {
        if (heightPx <= 0) return 0
        return (heightPx + MAX_SLICE - 1) / MAX_SLICE
    }
}

private const val BYTES_PER_MB = 1024L * 1024L
private const val QUARTER_DIVISOR = 4L
private const val MIN_SAMPLE = 1
private const val MAX_SLICE = 2048
