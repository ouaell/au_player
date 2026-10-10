package com.example.auplayer

import android.content.Context
import android.content.Intent
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.Surface
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.example.auplayer.player.LibmpvBridge
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * S1-001 stress: 500 rapid attach/detach cycles against the real bridge,
 * interleaved with real rotations (true SurfaceHolder destroy/create through
 * SurfaceHost) and periodic re-opens (each open can trigger mpv vo init).
 * Before the fix this shape aborted the process: `vo_android_init` ran
 * `ANativeWindow_fromSurface` on a deleted global ref (wid still pointed at
 * it after detach). A SIGABRT kills the process, so mere completion plus the
 * workflow logcat gate (`E: native crash`) is the crash assertion; the final
 * playback + screenshot proof shows wid plumbing still delivers video.
 */
@RunWith(AndroidJUnit4::class)
class S1SurfaceStressTest {
    @Test
    fun attachDetachStormSurvives() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clip = File(context.filesDir, "e1.mp4")
        assertTrue("missing pushed clip: $clip", clip.isFile)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val engine = (context.applicationContext as AuApp).container.playerEngine
        val bridge = engine.bridge as LibmpvBridge
        val intent = Intent(Intent.ACTION_VIEW).putExtra(
            MainActivity.EXTRA_DEBUG_PATH,
            clip.absolutePath
        ).setClassName(context, MainActivity::class.java.name)
        val launchTime = System.currentTimeMillis()
        ActivityScenario.launch<MainActivity>(intent).use {
            try {
                runBlocking {
                    assertTrue("no playback at start", waitForFreshPlayback(launchTime))
                    val texA = SurfaceTexture(0)
                    val texB = SurfaceTexture(1)
                    val surfA = Surface(texA)
                    val surfB = Surface(texB)
                    try {
                        repeat(CYCLES) { i ->
                            // Alternate two dull surfaces: every attach swaps
                            // the global ref mpv's wid points at.
                            bridge.attach(if (i % 2 == 0) surfA else surfB)
                            if (i % 10 == 0) engine.open(clip.absolutePath)
                            bridge.detach()
                            bridge.attach(if (i % 2 == 0) surfB else surfA)
                            if (i % 10 == 5) engine.open(clip.absolutePath)
                            bridge.detach()
                            if (i % ROTATE_EVERY == 0) {
                                if ((i / ROTATE_EVERY) % 2 == 0) device.setOrientationLeft()
                                else device.setOrientationRight()
                                delay(ROTATE_SETTLE_MS)
                            }
                        }
                    } finally {
                        surfA.release()
                        surfB.release()
                        texA.release()
                        texB.release()
                    }
                    Log.i(GATE_TAG, "storm done cycles=$CYCLES")
                    device.setOrientationNatural()
                    delay(SETTLE_MS)
                    // Fresh open through the normal path must still produce video.
                    engine.open(clip.absolutePath)
                    assertTrue("no playback after storm", waitForFreshPlayback(launchTime))
                    val shot = File(
                        InstrumentationRegistry.getInstrumentation().targetContext.filesDir,
                        "s1-shot.png"
                    )
                    assertTrue("screenshot failed", device.takeScreenshot(shot))
                    assertTrue("blank screenshot after storm", shot.length() > MIN_SHOT_BYTES)
                    Log.i(GATE_TAG, "storm survived, playback proven")
                }
            } finally {
                device.setOrientationNatural()
            }
        }
    }

    private fun traceLines(): Int {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val trace = File(instrumentation.targetContext.filesDir, "e1-trace.txt")
        return try {
            trace.readLines().size
        } catch (error: java.io.IOException) {
            0
        }
    }

    private suspend fun waitForFreshPlayback(launchTime: Long): Boolean {
        val baseline = traceLines()
        val deadline = System.currentTimeMillis() + GROWTH_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            if (traceLines() > baseline) return true
            delay(POLL_MS)
        }
        return false
    }
}

private const val CYCLES = 500
private const val ROTATE_EVERY = 25
private const val ROTATE_SETTLE_MS = 1_500L
private const val SETTLE_MS = 2_500L
private const val POLL_MS = 250L
private const val GROWTH_TIMEOUT_MS = 20_000L
private const val MIN_SHOT_BYTES = 10_000L
private const val GATE_TAG = "S1GATE"
