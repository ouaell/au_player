package com.example.auplayer.library

/** Resume point for continue-watching (MS-019). */
data class ResumeEntry(
    val uri: String,
    val title: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAtMs: Long
)

/** History seam (§4). Room backs it on device (ResumeDb); tests use memory. */
interface ResumeStore {
    suspend fun save(entry: ResumeEntry)
    suspend fun load(uri: String): ResumeEntry?
    suspend fun remove(uri: String)
    suspend fun clear()
    suspend fun continueWatching(limit: Int): List<ResumeEntry>
}

class InMemoryResumeStore : ResumeStore {
    private val lock = Any()
    private val entries = mutableMapOf<String, ResumeEntry>()

    override suspend fun save(entry: ResumeEntry) {
        synchronized(lock) { entries[entry.uri] = entry }
    }

    override suspend fun load(uri: String): ResumeEntry? {
        synchronized(lock) { return entries[uri] }
    }

    override suspend fun remove(uri: String) {
        synchronized(lock) { entries.remove(uri) }
    }

    override suspend fun clear() {
        synchronized(lock) { entries.clear() }
    }

    override suspend fun continueWatching(limit: Int): List<ResumeEntry> {
        synchronized(lock) {
            return entries.values
                .filter { it.durationMs > 0 && it.positionMs > 0 && it.positionMs < it.durationMs }
                .sortedByDescending { it.updatedAtMs }
                .take(limit)
        }
    }
}
