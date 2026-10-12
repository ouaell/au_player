package eu.kanade.tachiyomi.ui.videos

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One row of the on-device video library. */
data class VideoItem(
    val id: Long,
    val uri: Uri,
    val title: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSeconds: Long,
) {
    val durationLabel: String get() = formatDuration(durationMs)
    val sizeLabel: String get() = formatSize(sizeBytes)
}

/**
 * MediaStore video library reader.
 *
 * Queries only (no mutation): the browser lists whatever the system already
 * knows about, so there is nothing to sync and nothing to keep consistent.
 * Newest-first, because that is what a video library is opened for.
 */
class VideoRepository(private val context: Context) {

    suspend fun load(limit: Int = QUERY_LIMIT): List<VideoItem> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
        )
        val order = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        buildList {
            context.contentResolver.query(collection, projection, null, null, order)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                while (cursor.moveToNext() && size < limit) {
                    val id = cursor.getLong(idCol)
                    add(
                        VideoItem(
                            id = id,
                            uri = ContentUris.withAppendedId(collection, id),
                            title = cursor.getString(nameCol) ?: UNTITLED,
                            durationMs = cursor.getLong(durationCol).coerceAtLeast(0L),
                            sizeBytes = cursor.getLong(sizeCol).coerceAtLeast(0L),
                            dateAddedSeconds = cursor.getLong(dateCol),
                        ),
                    )
                }
            }
        }
    }

    companion object {
        private const val QUERY_LIMIT = 500
        private const val UNTITLED = "Untitled"

        /** `h:mm:ss` past an hour, `m:ss` below it. */
        fun formatDuration(ms: Long): String {
            val total = (ms / 1000).coerceAtLeast(0L)
            val hours = total / 3600
            val minutes = (total % 3600) / 60
            val seconds = total % 60
            return if (hours > 0) {
                "%d:%02d:%02d".format(hours, minutes, seconds)
            } else {
                "%d:%02d".format(minutes, seconds)
            }
        }

        /** Human file size; one decimal only above a gigabyte. */
        fun formatSize(bytes: Long): String = when {
            bytes >= GIGABYTE -> "%.1f GB".format(bytes.toDouble() / GIGABYTE)
            bytes >= MEGABYTE -> "%.0f MB".format(bytes.toDouble() / MEGABYTE)
            bytes >= KILOBYTE -> "%.0f KB".format(bytes.toDouble() / KILOBYTE)
            else -> "$bytes B"
        }

        /** Filenames carry an extension; titles read better without it. */
        fun displayName(fileName: String): String = fileName.substringBeforeLast('.', fileName).ifBlank { fileName }

        private const val KILOBYTE = 1024.0
        private const val MEGABYTE = KILOBYTE * 1024
        private const val GIGABYTE = MEGABYTE * 1024
    }
}