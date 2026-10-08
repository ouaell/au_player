package com.example.auplayer.library

/**
 * Reader progress over the shared [ResumeStore] (MS-041): page index rides
 * in `positionMs`, page count in `durationMs`, so continue-watching,
 * recents and cleanup stay unified with video history.
 */
class ReaderHistory(private val store: ResumeStore) {
    suspend fun savePage(uri: String, title: String, index: Int, total: Int, nowMs: Long) {
        store.save(ResumeEntry(uri, title, index.toLong(), total.toLong(), nowMs))
    }

    /** Last page index, or null when never opened. */
    suspend fun lastPage(uri: String): Int? {
        return store.load(uri)?.positionMs?.toInt()
    }

    suspend fun recent(limit: Int): List<ResumeEntry> = store.continueWatching(limit)
}
