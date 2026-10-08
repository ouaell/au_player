package com.example.auplayer.player

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
                        val surface = holder.surface
                        surfaces.onSurfaceCreated(surface)
                        val frame = holder.surfaceFrame
                        if (surface != null && surface.isValid &&
                            frame.width() > 0 && frame.height() > 0
                        ) {
                            render?.start(surface, frame.width(), frame.height())
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
