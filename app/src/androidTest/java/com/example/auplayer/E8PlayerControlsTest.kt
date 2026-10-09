package com.example.auplayer

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * E8 — player controls on the real engine. Core behavior needs no UI dumps
 * (pause freezes time-pos, play resumes, system back destroys): dump-shape
 * flakiness on the emulator cannot fake those. The Pause/Play buttons are
 * additionally exercised through the hierarchy with tap retries; a
 * persistent miss is a real failure. Evidence under E8GATE.
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
                engine.play()
                assertTrue("no resume after play", waitForGrowthBeyond(traceLines()))
                Log.i(GATE_TAG, "play resumes time")

                var buttonSeen = false
                var rounds = 0
                while (!buttonSeen && rounds < BUTTON_ROUNDS) {
                    rounds++
                    device.click(device.displayWidth / 2, device.displayHeight / 2)
                    buttonSeen = device.wait(Until.hasObject(By.desc("Pause")), BUTTON_WAIT_MS) ||
                        device.wait(Until.hasObject(By.desc("Play")), BUTTON_WAIT_MS)
                }
                assertTrue("pause/play button never visible", buttonSeen)
                Log.i(GATE_TAG, "buttons visible")

                device.pressBack()
                val deadline = System.currentTimeMillis() + UI_TIMEOUT_MS
                while (scenario.state != Lifecycle.State.DESTROYED &&
                    System.currentTimeMillis() < deadline
                ) {
                    delay(POLL_MS)
                }
                assertEquals(Lifecycle.State.DESTROYED, scenario.state)
                Log.i(GATE_TAG, "back destroys")
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
}

private const val UI_TIMEOUT_MS = 15_000L
private const val POLL_MS = 250L
private const val FREEZE_MS = 2_500L
private const val BUTTON_WAIT_MS = 5_000L
private const val BUTTON_ROUNDS = 3
private const val GROWTH_TIMEOUT_MS = 20_000L
private const val GATE_TAG = "E8GATE"
