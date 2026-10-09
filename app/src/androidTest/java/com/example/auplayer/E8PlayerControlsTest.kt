package com.example.auplayer

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * E8 — player controls on the real engine: controls visible at start, Pause
 * pauses (button flips to Play), tap toggles visibility, Back leaves.
 * Evidence goes to logcat under E8GATE.
 */
@RunWith(AndroidJUnit4::class)
class E8PlayerControlsTest {
    @Test
    fun controlsPauseAndBack() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clip = File(context.filesDir, "e1.mp4")
        assertTrue("missing pushed clip: $clip", clip.isFile)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val intent = Intent(Intent.ACTION_VIEW).putExtra(
            MainActivity.EXTRA_DEBUG_PATH,
            clip.absolutePath
        ).setClassName(context, MainActivity::class.java.name)
        val launchTime = System.currentTimeMillis()
        ActivityScenario.launch<MainActivity>(intent).use {
            runBlocking {
                assertTrue("no playback at start", waitForFreshPlayback(launchTime))
                // Controls auto-hide after 3 s; tap the surface to reshow.
                device.click(device.displayWidth / 2, device.displayHeight / 2)
                assertTrue("pause control missing", device.wait(Until.hasObject(pauseSelector()), UI_TIMEOUT_MS))
                Log.i(GATE_TAG, "controls visible")

                device.findObject(pauseSelector()).click()
                device.click(device.displayWidth / 2, device.displayHeight / 2)
                assertTrue("pause did not engage", device.wait(Until.hasObject(playSelector()), UI_TIMEOUT_MS))
                Log.i(GATE_TAG, "paused ok")

                device.findObject(playSelector()).click()
                device.click(device.displayWidth / 2, device.displayHeight / 2)
                assertTrue("resume did not engage", device.wait(Until.hasObject(pauseSelector()), UI_TIMEOUT_MS))
                Log.i(GATE_TAG, "resumed ok")

                device.findObject(By.text("Back")).click()
                assertTrue(
                    "back did not leave",
                    device.wait(Until.gone(By.pkg(context.packageName)), UI_TIMEOUT_MS)
                )
                Log.i(GATE_TAG, "back ok")
            }
        }
    }

    private fun pauseSelector() = By.desc("Pause")

    private fun playSelector() = By.desc("Play")

    private fun traceState(): Pair<Int, Long> {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val trace = File(instrumentation.targetContext.filesDir, "e1-trace.txt")
        return try {
            Pair(trace.readLines().size, trace.lastModified())
        } catch (error: java.io.IOException) {
            Pair(0, 0L)
        }
    }

    private suspend fun waitForFreshPlayback(launchTime: Long): Boolean {
        val baseline = traceState().first
        val deadline = System.currentTimeMillis() + GROWTH_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            val state = traceState()
            if (state.first > baseline) return true
            if (state.second > launchTime && state.first > 0) return true
            delay(POLL_MS)
        }
        return false
    }
}

private const val UI_TIMEOUT_MS = 15_000L
private const val POLL_MS = 250L
private const val GROWTH_TIMEOUT_MS = 20_000L
private const val GATE_TAG = "E8GATE"
