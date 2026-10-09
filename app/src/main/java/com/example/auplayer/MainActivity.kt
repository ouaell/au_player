package com.example.auplayer

import android.app.PictureInPictureParams
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.auplayer.library.ResumeEntry
import com.example.auplayer.player.PipCalc
import com.example.auplayer.player.PlayerHandle
import com.example.auplayer.player.PlayerScreen
import com.example.auplayer.routing.Incoming
import com.example.auplayer.routing.IncomingAction
import com.example.auplayer.routing.IntentRouter
import com.example.auplayer.routing.Route
import com.example.auplayer.ui.theme.AuTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Single-activity (§2). Predictive back via enableOnBackInvokedCallback (manifest). */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val viewUri = if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.toString() ?: intent.getStringExtra(EXTRA_DEBUG_PATH)
        } else {
            null
        }
        Log.i("AuPlayer", "route viewUri=$viewUri action=${intent?.action}")
        // Test-only direct path (E1/E2/E3): bypasses routing, unchanged.
        val debugPath = intent?.getStringExtra(EXTRA_DEBUG_PATH)
        setContent {
            AuTheme {
                if (debugPath != null) {
                    val app = application as AuApp
                    val scope = rememberCoroutineScope()
                    PlayerScreen(
                        uri = debugPath,
                        handle = PlayerHandle(
                            engine = app.container.playerEngine,
                            enhance = app.container.enhanceController,
                            onBack = { finish() },
                            onProgress = savePlayback(app, scope, debugPath)
                        )
                    )
                } else {
                    RoutedScreen(intent = intent, fallbackUri = viewUri, onBack = { finish() })
                }
            }
        }
    }

    /** PiP entry (MS-023); behavior verified on device (Day-1 gate T3). */
    fun enterAuPip(videoW: Int, videoH: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val (width, height) = PipCalc.aspect(videoW, videoH)
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(width, height))
            .build()
        enterPictureInPictureMode(params)
    }

    companion object {
        /** Test-only direct path (E1): avoids file:// exposure + scoped storage. */
        const val EXTRA_DEBUG_PATH = "au_debug_path"
    }
}

/** Resume-point saver shared by both player entries (MS-019). */
private fun savePlayback(
    app: AuApp,
    scope: CoroutineScope,
    uri: String
): (Long, Long) -> Unit = { positionMs, durationMs ->
    scope.launch(Dispatchers.IO) {
        try {
            app.container.resumeStore.save(
                ResumeEntry(
                    uri = uri,
                    title = uri.substringAfterLast('/'),
                    positionMs = positionMs,
                    durationMs = durationMs,
                    updatedAtMs = System.currentTimeMillis()
                )
            )
            Log.i("E9GATE", "saved $uri pos=$positionMs dur=$durationMs")
        } catch (error: android.database.sqlite.SQLiteException) {
            Log.w("E9GATE", "save failed: " + error.message)
        } catch (error: java.io.IOException) {
            Log.w("E9GATE", "save failed: " + error.message)
        } catch (error: IllegalStateException) {
            // Missing/broken store backend must never kill playback.
            Log.w("E9GATE", "save failed: " + error.message)
        }
    }
}

/**
 * Real intent routing (MS-040 made visible): video/audio streams go to the
 * player, archives/PDF/images to [ReaderScreen], everything else home.
 * [viewUri] preserves the legacy player path for data URIs without a MIME.
 */
@Composable
private fun RoutedScreen(intent: Intent?, fallbackUri: String?, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as AuApp
    val scope = rememberCoroutineScope()
    val incoming = Incoming(
        action = when (intent?.action) {
            Intent.ACTION_VIEW -> IncomingAction.VIEW
            Intent.ACTION_SEND -> IncomingAction.SEND
            Intent.ACTION_SEND_MULTIPLE -> IncomingAction.SEND_MULTIPLE
            else -> IncomingAction.OTHER
        },
        mime = intent?.type,
        uris = listOfNotNull(intent?.data?.toString(), fallbackUri)
    )
    when (val route = IntentRouter.route(incoming)) {
        is Route.Player -> PlayerScreen(
            uri = route.uri,
            handle = PlayerHandle(
                engine = app.container.playerEngine,
                enhance = app.container.enhanceController,
                onBack = onBack,
                onProgress = savePlayback(app, scope, route.uri)
            )
        )
        is Route.Reader -> ReaderScreen(uri = route.uris.first(), kind = route.kind)
        Route.Unsupported -> AuNav(app.container)
    }
}
