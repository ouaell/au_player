package com.example.auplayer

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.example.auplayer.player.MpvEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs

/**
 * E1 — real engine plays a corpus clip (OPENCODE_COMMAND §5). The workflow
 * pushes `e1.mp4` into the app files dir; playback must advance time-pos,
 * reach EOF cleanly, show non-blank changing pixels mid-playback, and leave
 * logcat without FATAL/ANR (asserted by the workflow).
 */
@RunWith(AndroidJUnit4::class)
class E1PlayerTest {
    @Test
    fun realEngineAdvancesTimePosAndEndsCleanly() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clip = File(context.filesDir, "e1.mp4")
        assertTrue("missing pushed clip: $clip", clip.isFile)
        val intent = Intent(Intent.ACTION_VIEW).putExtra(
            MainActivity.EXTRA_DEBUG_PATH,
            clip.absolutePath
        ).setClassName(context, MainActivity::class.java.name)
        val engine = (context.applicationContext as AuApp).container.playerEngine
        val times = mutableListOf<Double>()
        var sawEof = false
        ActivityScenario.launch<MainActivity>(intent).use {
            runBlocking {
                val job = launch {
                    engine.events.collect { event ->
                        when (event) {
                            is MpvEvent.TimePos -> times += event.seconds
                            is MpvEvent.Eof -> sawEof = true
                        }
                    }
                }
                delay(STARTUP_MS)
                val first = takeShot("e1-shot-1.png")
                delay(SHOT_GAP_MS)
                val second = takeShot("e1-shot-2.png")
                val endedEarly = sawEof
                withTimeout(E1_TIMEOUT_MS) {
                    while (!sawEof && times.size < MAX_SAMPLES) {
                        delay(POLL_MS)
                    }
                }
                job.cancel()
                assertShots(first, second, endedEarly)
            }
        }
        assertTrue("no time-pos observed", times.isNotEmpty())
        assertTrue("time-pos never advanced", (times.max() - times.min()) >= MIN_SPAN_S)
        assertTrue("no clean EOF", sawEof)
    }

    private fun takeShot(name: String): File {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        val dir = instrumentation.targetContext.getExternalFilesDir(null)
            ?: error("no external dir")
        val file = File(dir, name)
        assertTrue("screenshot failed", device.takeScreenshot(file))
        return file
    }

    private fun assertShots(first: File, second: File, endedEarly: Boolean) {
        val firstStats = luminance(first)
        assertTrue("blank frame", firstStats.second > MIN_VARIANCE)
        if (!endedEarly) {
            val secondStats = luminance(second)
            val changed = abs(firstStats.first - secondStats.first) > MIN_DELTA ||
                abs(firstStats.second - secondStats.second) > MIN_DELTA
            assertTrue("screenshots identical (frozen video?)", changed)
        }
    }

    private fun luminance(png: File): Pair<Double, Double> {
        val bitmap: Bitmap = BitmapFactory.decodeFile(png.absolutePath)
            ?: error("undecodable $png")
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var sum = 0L
        var sumSq = 0L
        for (pixel in pixels) {
            val luma = ((pixel shr 16) and 0xFF) + ((pixel shr 8) and 0xFF) + (pixel and 0xFF)
            sum += luma
            sumSq += luma.toLong() * luma
        }
        val count = pixels.size.toDouble()
        val mean = sum / count
        return Pair(mean, sumSq / count - mean * mean)
    }
}

private const val E1_TIMEOUT_MS = 45_000L
private const val MIN_SPAN_S = 2.0
private const val STARTUP_MS = 800L
private const val SHOT_GAP_MS = 1_200L
private const val POLL_MS = 200L
private const val MAX_SAMPLES = 200
private const val MIN_VARIANCE = 10.0
private const val MIN_DELTA = 0.5
