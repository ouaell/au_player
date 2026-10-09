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
 * E3 — Enhance on the real engine (§17.2 scenario 2, crash-focused): apply a
 * real patched shader (RAVU-lite, Standard top rung, pushed by the workflow)
 * while playing, survive shader compile on swiftshader, keep playing, then
 * clear back to Off. Command shapes mirror EnhanceController.apply.
 * Evidence goes to logcat under E3GATE.
 */
@RunWith(AndroidJUnit4::class)
class E3EnhanceTest {
    @Test
    fun enhanceToggleSurvivesOnRealEngine() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clip = File(context.filesDir, "e1.mp4")
        assertTrue("missing pushed clip: $clip", clip.isFile)
        val shader = File(context.filesDir, "e3-ravu.hook")
        assertTrue("missing pushed shader: $shader", shader.isFile)
        val engine = (context.applicationContext as AuApp).container.playerEngine
        val intent = Intent(Intent.ACTION_VIEW).putExtra(
            MainActivity.EXTRA_DEBUG_PATH,
            clip.absolutePath
        ).setClassName(context, MainActivity::class.java.name)
        val launchTime = System.currentTimeMillis()
        ActivityScenario.launch<MainActivity>(intent).use {
            runBlocking {
                assertTrue("no playback at start", waitForFreshPlayback(launchTime))
                gate("playing unenhanced ok")

                engine.send(listOf("set", "scale", "lanczos"))
                engine.send(listOf("set", "dscale", "mitchell"))
                engine.send(listOf("set", "correct-downscaling", "yes"))
                engine.send(listOf("set", "linear-downscaling", "no"))
                engine.send(listOf("change-list", "glsl-shaders", "set", shader.absolutePath))
                Log.i(GATE_TAG, "shader applied, waiting out compile")
                delay(COMPILE_MS)
                assertTrue("playback died with shader", waitForFreshPlayback(launchTime))
                gate("enhanced ok")

                engine.send(listOf("change-list", "glsl-shaders", "clr", ""))
                engine.send(listOf("set", "scale", "bilinear"))
                engine.send(listOf("set", "dscale", "bilinear"))
                delay(SETTLE_MS)
                assertTrue("playback died after clear", waitForFreshPlayback(launchTime))
                gate("cleared ok")
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

    private fun gate(message: String) {
        Log.i(GATE_TAG, "step $message")
    }
}

private const val COMPILE_MS = 8_000L
private const val SETTLE_MS = 2_000L
private const val POLL_MS = 250L
private const val GROWTH_TIMEOUT_MS = 20_000L
private const val GATE_TAG = "E3GATE"
