package com.example.auplayer.player

import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Real player surface: vo/gpu presents DIRECTLY to the SurfaceView Surface
 * (passed to mpv as `wid` at attach). Open waits for a usable surface —
 * without `wid` set before loadfile the vo has "Missing surface pointer"
 * and playback is audio-only (E1). No app-owned EGL loop.
 */
@Composable
fun PlayerScreen(
    uri: String,
    engine: PlayerEngine,
    modifier: Modifier = Modifier
) {
    val surfaces = remember(engine) { SurfaceHost(engine) }
    var surfaceReady by remember { mutableStateOf(false) }
    LaunchedEffect(uri, surfaceReady) {
        if (surfaceReady) {
            Log.i("AuPlayer", "open $uri")
            engine.open(uri)
        }
    }
    AndroidView(
        factory = { context ->
            SurfaceView(context).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        surfaces.onSurfaceCreated(holder.surface)
                        surfaceReady = usableFrame(holder) != null
                    }

                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int
                    ) {
                        // surfaceCreated may arrive with a 0x0 frame; a usable
                        // frame here arms the gated open above.
                        surfaces.onSurfaceCreated(holder.surface)
                        surfaceReady = usableFrame(holder) != null
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        surfaceReady = false
                        surfaces.onSurfaceDestroyed()
                    }
                })
            }
        },
        modifier = modifier
    )
}

private data class Frame(val surface: Surface, val width: Int, val height: Int)

private fun usableFrame(holder: SurfaceHolder): Frame? {
    val surface = holder.surface
    if (surface == null || !surface.isValid) return null
    return sizedFrame(holder, surface)
}

private fun sizedFrame(holder: SurfaceHolder, surface: Surface): Frame? {
    val frame = holder.surfaceFrame
    if (frame.width() <= 0 || frame.height() <= 0) return null
    return Frame(surface, frame.width(), frame.height())
}
