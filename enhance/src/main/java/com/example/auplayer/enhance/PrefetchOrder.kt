package com.example.auplayer.enhance

/**
 * Prefetch priority (MS-048): current page first, then +1/+2/+3. Pure order
 * math; worker orchestration (one inference at a time, cancel on fast
 * scroll) binds this on device. Stale pages outside the window are dropped.
 */
object PrefetchOrder {
    fun batch(current: Int, total: Int, window: Int = PREFETCH_WINDOW): List<Int> {
        if (total <= 0) return emptyList()
        val first = current.coerceIn(0, total - 1)
        return (0..window).map { first + it }.filter { it < total }
    }

    fun dropStale(pending: List<Int>, current: Int, window: Int = PREFETCH_WINDOW): List<Int> {
        return pending.filter { it >= current && it <= current + window }
    }
}

/** Disk cache key: (contentHash, page, modelId, scale) — MS-048. */
fun cacheKey(contentHash: String, page: Int, modelId: String, scale: Int): String {
    return contentHash + "/" + page.toString() + "/" + modelId + "/x" + scale.toString()
}

private const val PREFETCH_WINDOW = 3
