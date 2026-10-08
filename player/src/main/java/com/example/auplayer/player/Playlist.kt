package com.example.auplayer.player

/** Queue item (MS-021). */
data class QueueItem(val uri: String, val title: String)

/**
 * Playlist with auto-next (MS-021). EOF behavior reuses [afterEof] so loop
 * semantics stay identical between single-video and queue playback.
 */
class Playlist {
    private val items = mutableListOf<QueueItem>()
    var index: Int = -1
        private set

    val size: Int get() = items.size

    fun setItems(newItems: List<QueueItem>, startAt: Int = 0) {
        items.clear()
        items.addAll(newItems)
        index = if (newItems.isEmpty()) -1 else startAt.coerceIn(0, newItems.size - 1)
    }

    fun current(): QueueItem? = items.getOrNull(index)

    fun moveTo(position: Int): Boolean {
        if (position < 0 || position >= items.size) return false
        index = position
        return true
    }

    /** Applies EOF: returns the item to play next, or null to stop. */
    fun moveOnEof(mode: LoopMode, autoNext: Boolean): QueueItem? {
        if (items.isEmpty() || index < 0 || haltedByUser(mode, autoNext)) return null
        if (mode == LoopMode.ALL && index == items.size - 1) index = BEFORE_FIRST
        return when (afterEof(mode, hasNextInPlaylist = index < items.size - 1)) {
            EofAction.REPLAY -> current()
            EofAction.NEXT -> {
                index++
                current()
            }
            EofAction.STOP -> null
        }
    }

    private fun haltedByUser(mode: LoopMode, autoNext: Boolean): Boolean =
        !autoNext && mode == LoopMode.OFF
}

private const val BEFORE_FIRST = -1
