package com.example.auplayer.core

/** Bounded in-memory diagnostics store (screen + crash logger land in MS-025). */
object Diag {
    fun report(message: String): String = message

    fun record(tag: String, message: String) {
        synchronized(lock) {
            if (entries.size >= MAX_DIAG_ENTRIES) entries.removeFirst()
            entries.addLast(DiagEntry(tag, message))
        }
    }

    fun entries(): List<DiagEntry> = synchronized(lock) { entries.toList() }

    fun clear() {
        synchronized(lock) { entries.clear() }
    }

    private val lock = Any()
    private val entries = ArrayDeque<DiagEntry>()
}

data class DiagEntry(val tag: String, val message: String)

private const val MAX_DIAG_ENTRIES = 200
