package eu.kanade.tachiyomi.ui.videos

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.videos.VideoRepository.Companion.displayName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tachiyomi.presentation.core.screens.EmptyScreen

/**
 * On-device video library.
 *
 * Thumbnails come straight from the platform's MediaStore cache via
 * [Context.getStyledContentProvider] loaders rather than Coil: one bounded
 * decode per visible cell, no disk round-trip, and no second image cache to
 * reason about on top of the reader's.
 */
@Composable
fun VideoLibraryScreen(
    videos: List<VideoItem>,
    isLoading: Boolean,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpen: (VideoItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        !hasPermission -> PermissionPrompt(onRequestPermission, modifier)
        isLoading -> Unit
        videos.isEmpty() -> EmptyScreen(message = "No videos on this device yet", modifier = modifier)
        else -> LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 168.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = modifier.fillMaxSize(),
        ) {
            items(videos, key = { it.id }) { video ->
                VideoCard(video = video, onClick = { onOpen(video) })
            }
        }
    }
}

@Composable
private fun PermissionPrompt(onRequestPermission: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "AU Player needs video access",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Grant the media permission to browse and play the videos stored on this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
        )
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onRequestPermission)
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text(
                text = "Allow access",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun VideoCard(video: VideoItem, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            VideoThumbnail(
                uri = video.uri,
                modifier = Modifier.fillMaxSize(),
            )
            Row(
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = video.durationLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                )
            }
        }
        Text(
            text = displayName(video.title),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text = video.sizeLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Platform thumbnail, decoded off the main thread and cached by the system. */
@Composable
private fun VideoThumbnail(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val loader = context.contentResolver.loadThumbnail(uri, Size(384, 216), null)
                loader.use { it }
            }.getOrNull()
        }
    }
    val current = bitmap
    if (current != null) {
        Image(
            bitmap = current.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }
}

/**
 * Loads the library once permission is granted and re-reads on [refresh].
 * Deliberately a plain state holder rather than a ViewModel: the tab host owns
 * it for the tab's lifetime and nothing outlives the composition.
 */
@Stable
class VideoLibraryState(context: Context) {
    var videos: List<VideoItem> by mutableStateOf(emptyList())
        private set
    var isLoading: Boolean by mutableStateOf(false)
        private set

    private val repository = VideoRepository(context)

    suspend fun load() {
        isLoading = true
        videos = runCatching { repository.load() }.getOrDefault(emptyList())
        isLoading = false
    }

    fun clear() {
        videos = emptyList()
        isLoading = false
    }
}

@Composable
fun rememberVideoLibraryState(): VideoLibraryState {
    val context = LocalContext.current
    return remember(context) { VideoLibraryState(context) }
}