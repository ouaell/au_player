package eu.kanade.tachiyomi.ui.videos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Videos area: the on-device library from MediaStore, tapped through to the
 * full-screen player. Permission is requested inline the first time the tab is
 * opened rather than at startup, so the ask arrives with the feature visible.
 */
data object VideosTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            return TabOptions(
                index = 1u,
                title = stringResource(MR.strings.videos_tab),
                icon = rememberVectorPainter(Icons.Filled.VideoLibrary),
            )
        }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val state = rememberVideoLibraryState()
        var hasPermission by remember { mutableStateOf(context.hasVideoPermission()) }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            hasPermission = granted
        }

        LaunchedEffect(hasPermission) {
            if (hasPermission) state.load()
        }

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
        }

        VideoLibraryScreen(
            videos = state.videos,
            isLoading = state.isLoading,
            hasPermission = hasPermission,
            onRequestPermission = {
                launcher.launch(context.videoPermission())
            },
            onOpen = { video ->
                context.startActivity(PlayerActivity.intent(context, video.uri))
            },
        )
    }
}

internal fun android.content.Context.hasVideoPermission(): Boolean {
    val permission = videoPermission()
    return androidx.core.content.ContextCompat.checkSelfPermission(this, permission) ==
        PackageManager.PERMISSION_GRANTED
}

internal fun android.content.Context.videoPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }