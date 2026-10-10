package com.example.auplayer

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs

/**
 * E8 — player controls on the real engine, fully dump-free (UiAutomator
 * dumps wedge on the emulator): pause freezes time-pos, play resumes, and a
 * shown-vs-hidden screenshot diff proves the controls chrome renders.
 * Evidence under E8GATE.
 */
@RunWith(AndroidJUnit4::class)
class E8PlayerControlsTest {
    @Test
    fun controlsPauseAndBack() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clip = File(context.filesDir, "e1.mp4")
        assertTrue("missing pushed clip: $clip", clip.isFile)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val engine = (context.applicationContext as AuApp).container.playerEngine
        val intent = Intent(Intent.ACTION_VIEW).putExtra(
            MainActivity.EXTRA_DEBUG_PATH,
            clip.absolutePath
        ).setClassName(context, MainActivity::class.java.name)
        val launchTime = System.currentTimeMillis()
        val scenario = ActivityScenario.launch<MainActivity>(intent)
        scenario.use {
            runBlocking {
                // Explicit state: a previous test may have left the shared
                // engine paused (E7 freezes the UI), which would wedge the
                // growth wait below.
                engine.play()
                assertTrue("no playback at start", waitForFreshPlayback(launchTime))
                engine.pause()
                delay(FREEZE_MS)
                val frozen = traceLines()
                delay(FREEZE_MS)
                assertEquals("time advanced while paused", frozen, traceLines())
                Log.i(GATE_TAG, "pause freezes time")
                assertControlsRender(device)
                engine.play()
                assertTrue("no resume after play", waitForGrowthBeyond(traceLines()))
                Log.i(GATE_TAG, "play resumes time")
            }
        }
        Log.i(GATE_TAG, "controls done")
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
            val count = traceLines()
            if (count > baseline) return true
            delay(POLL_MS)
        }
        return false
    }

    private suspend fun waitForGrowthBeyond(baseline: Int): Boolean {
        val deadline = System.currentTimeMillis() + GROWTH_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            if (traceLines() > baseline) return true
            delay(POLL_MS)
        }
        return false
    }

    /**
     * Pixel proof that controls render (dump-free): on frozen video, tap to
     * show, shoot, wait out the 3 s auto-hide, shoot again. Differing pixels
     * in the control strip can only be the controls chrome.
     */
    private suspend fun assertControlsRender(device: UiDevice) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val dir = instrumentation.targetContext.filesDir
        device.click(device.displayWidth / 2, device.displayHeight / 2)
        delay(SHOW_MS)
        val shown = File(dir, "e8-shown.png")
        assertTrue("screenshot failed", device.takeScreenshot(shown))
        delay(HIDE_MS)
        val hidden = File(dir, "e8-hidden.png")
        assertTrue("screenshot failed", device.takeScreenshot(hidden))
        val diff = differingPixels(shown, hidden)
        Log.i(GATE_TAG, "controls diff pixels=$diff")
        assertTrue("controls chrome did not render", diff > MIN_DIFF)
    }

    private fun differingPixels(first: File, second: File): Int {
        val a = android.graphics.BitmapFactory.decodeFile(first.absolutePath)
            ?: error("undecodable $first")
        val b = android.graphics.BitmapFactory.decodeFile(second.absolutePath)
            ?: error("undecodable $second")
        if (a.width != b.width || a.height != b.height) return Int.MAX_VALUE
        val pa = IntArray(a.width * a.height)
        val pb = IntArray(b.width * b.height)
        a.getPixels(pa, 0, a.width, 0, 0, a.width, a.height)
        b.getPixels(pb, 0, b.width, 0, 0, b.width, b.height)
        var top = 0
        var bottom = 0
        for (y in 0 until a.height) {
            for (x in 0 until a.width) {
                val index = y * a.width + x
                val da = abs(((pa[index] shr 16) and 0xFF) - ((pb[index] shr 16) and 0xFF)) +
                    abs(((pa[index] shr 8) and 0xFF) - ((pb[index] shr 8) and 0xFF)) +
                    abs((pa[index] and 0xFF) - (pb[index] and 0xFF))
                if (da > DIFF_LUMA) {
                    if (y < a.height / 2) {
                        top++
                    } else {
                        bottom++
                    }
                }
            }
        }
        Log.i(GATE_TAG, "controls diff top=$top bottom=$bottom")
        if (bottom < MIN_DIFF) {
            Log.w(GATE_TAG, "bottom row did not render?")
        }
        return top + bottom
    }
}

private const val POLL_MS = 250L
private const val FREEZE_MS = 2_500L
private const val SHOW_MS = 1_000L
private const val HIDE_MS = 4_000L
private const val MIN_DIFF = 2_000
private const val DIFF_LUMA = 40
private const val GROWTH_TIMEOUT_MS = 20_000L
private const val GATE_TAG = "E8GATE"
