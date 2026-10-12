package eu.kanade.tachiyomi.ui.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.auplayer.aucore.Diag
import app.auplayer.enhance.EnhanceController
import app.auplayer.player.LibmpvBridge
import app.auplayer.player.MpvPaths
import app.auplayer.player.PlayerEngine
import app.auplayer.player.PlayerHandle
import app.auplayer.player.PlayerScreen
import app.auplayer.player.applyStartup

/**
 * Full-screen video playback.
 *
 * Owns exactly one [PlayerEngine] for its lifetime. Created eagerly in
 * [onCreate] rather than inside the composition so a configuration change does
 * not tear mpv down mid-playback; [onDestroy] releases it once.
 */
class PlayerActivity : ComponentActivity() {

    private var engine: PlayerEngine? = null
    private var released = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.data
        if (uri == null) {
            Log.w(TAG, "no media uri in intent, finishing")
            finish()
            return
        }

        val paths = MpvPaths(
            configDir = cacheDir,
            cacheDir = cacheDir,
            filesDir = filesDir,
        )
        val bridge = LibmpvBridge(paths)
        applyStartup(bridge, paths)

        val created = PlayerEngine(bridge)
        engine = created
        Diag.record("player", "activity created for $uri")

        setContent {
            // Written so progress reporting keeps a Compose-visible snapshot;
            // the durable write happens in the callback below.
            var positionMs by remember { mutableLongStateOf(0L) }
            val handle = remember(created) {
                PlayerHandle(
                    engine = created,
                    enhance = EnhanceController(send = created::send),
                    onBack = { finish() },
                    onProgress = { position, _ ->
                        positionMs = position
                        savePosition(uri, position)
                    },
                )
            }
            PlayerScreen(uri = uri.toString(), handle = handle)
        }
    }

    override fun onDestroy() {
        if (!released) {
            released = true
            engine?.release()
            engine = null
            Diag.record("player", "engine released")
        }
        super.onDestroy()
    }

    private fun savePosition(uri: Uri, positionMs: Long) {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(positionKey(uri), positionMs)
            .apply()
    }

    companion object {
        private const val TAG = "AuPlayer"
        private const val PREFS = "player_positions"

        fun positionKey(uri: Uri): String = uri.toString()

        fun intent(context: Context, uri: Uri): Intent =
            Intent(context, PlayerActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                setDataAndType(uri, "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
    }
}

/** Stored resume point for [uri]; [START_FROM_BEGINNING] when never played. */
const val START_FROM_BEGINNING = 0L

fun resumePosition(context: Context, uri: Uri): Long =
    context.getSharedPreferences("player_positions", Context.MODE_PRIVATE)
        .getLong(PlayerActivity.positionKey(uri), START_FROM_BEGINNING)