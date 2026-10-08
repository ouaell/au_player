package com.example.auplayer.player

import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Real player surface (Push B3): SurfaceView frames go to mpv through
 * EGL + the render API; lifecycle is guarded by [SurfaceHost]. With no
 * native lib present the screen stays black and engine calls fail closed
 * (E1 proves the real path on the emulator).
 */
@Composable
fun PlayerScreen(
    uri: String,
    engine: PlayerEngine,
    modifier: Modifier = Modifier
) {
    val surfaces = remember(engine) { SurfaceHost(engine) }
    val render = remember(engine) {
        (engine.bridge as? LibmpvBridge)?.let { RenderLoop(it) }
    }
    LaunchedEffect(uri) {
        engine.open(uri)
    }
    DisposableEffect(Unit) {
        onDispose { render?.stop() }
    }
    AndroidView(
        factory = { context ->
            SurfaceView(context).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        surfaces.onSurfaceCreated(holder.surface)
                        val frame = usableFrame(holder)
                        if (frame != null) {
                            render?.start(frame.surface, frame.width, frame.height)
                        }
                    }

                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int
                    ) = Unit

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        surfaces.onSurfaceDestroyed()
                        render?.stop()
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
