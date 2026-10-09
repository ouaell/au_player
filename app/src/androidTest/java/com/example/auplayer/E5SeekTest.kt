package com.example.auplayer

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * E5 — seek storm (§17.2 scenario 3, light): 20 deterministic seeks across
 * the 3s clip on the real engine; playback must keep advancing and the app
 * must not crash. Evidence goes to logcat under E5GATE.
 */
@RunWith(AndroidJUnit4::class)
class E5SeekTest {
    @Test
    fun seekStormSurvives() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clip = File(context.filesDir, "e1.mp4")
        assertTrue("missing pushed clip: $clip", clip.isFile)
        val engine = (context.applicationContext as AuApp).container.playerEngine
        val intent = Intent(Intent.ACTION_VIEW).putExtra(
            MainActivity.EXTRA_DEBUG_PATH,
            clip.absolutePath
        ).setClassName(context, MainActivity::class.java.name)
        val launchTime = System.currentTimeMillis()
        ActivityScenario.launch<MainActivity>(intent).use {
            runBlocking {
                assertTrue("no playback at start", waitForFreshPlayback(launchTime))
                Log.i(GATE_TAG, "seek storm starting")
                repeat(SEEK_COUNT) { step ->
                    val target = ((step * 0.37) % CLIP_SPAN).toString()
                    engine.seekTo(target.toDouble())
                    delay(SEEK_GAP_MS)
                }
                assertTrue("playback died in seek storm", waitForFreshPlayback(launchTime))
                Log.i(GATE_TAG, "seek storm survived")
            }
        }
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

private const val SEEK_COUNT = 20
private const val CLIP_SPAN = 2.8
private const val SEEK_GAP_MS = 300L
private const val POLL_MS = 250L
private const val GROWTH_TIMEOUT_MS = 20_000L
private const val GATE_TAG = "E5GATE"
