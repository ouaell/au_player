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
 * E9 — continue-watching end to end: play, pause mid-clip, close (dispose
 * saves the resume point), open home, the continue row shows the clip.
 * Evidence goes to logcat under E9GATE.
 */
@RunWith(AndroidJUnit4::class)
class E9ResumeTest {
    @Test
    fun pauseThenContinueWatching() {
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
        ActivityScenario.launch<MainActivity>(intent).use {
            runBlocking {
                assertTrue("no playback at start", waitForFreshPlayback(launchTime))
                delay(SETTLE_MS)
                engine.pause()
                delay(SETTLE_MS)
                Log.i(GATE_TAG, "paused mid-clip")
            }
        }
        // Dispose (above) saves the resume point; give Room a beat.
        Thread.sleep(SETTLE_MS)
        val home = Intent(Intent.ACTION_MAIN)
            .setClassName(context, MainActivity::class.java.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(home)
        assertTrue(
            "continue row missing",
            device.wait(Until.hasObject(By.text("e1.mp4")), UI_TIMEOUT_MS)
        )
        Log.i(GATE_TAG, "continue row shown")
    }

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
private const val SETTLE_MS = 2_000L
private const val POLL_MS = 250L
private const val GROWTH_TIMEOUT_MS = 20_000L
private const val GATE_TAG = "E9GATE"
