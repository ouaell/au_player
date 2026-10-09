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
import com.example.auplayer.player.MpvNative
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs

/**
 * E1 — real engine plays a corpus clip (OPENCODE_COMMAND §5). Progression +
 * EOF are proven by the app-side trace file (asserted by the workflow: the
 * test process cannot observe the app process event bus). This test proves:
 * native loads in-app, playback starts without crashing, and mid-playback
 * screenshots are non-blank and changing.
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
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            // Native libs live in the APP process (not the test process).
            scenario.onActivity {
                if (!MpvNative.load()) {
                    val packaged = File(it.applicationInfo.nativeLibraryDir).list()
                        ?.sorted().toString()
                    assertTrue(
                        "native missing: ${MpvNative.lastError}; packaged=$packaged",
                        false
                    )
                }
            }
            runBlocking {
                delay(STARTUP_MS)
                val first = takeShot("e1-shot-1.png")
                delay(SHOT_GAP_MS)
                val second = takeShot("e1-shot-2.png")
                assertShots(first, second)
            }
        }
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

    private fun assertShots(first: File, second: File) {
        val firstStats = luminance(first)
        assertTrue("blank frame", firstStats.second > MIN_VARIANCE)
        val secondStats = luminance(second)
        val changed = abs(firstStats.first - secondStats.first) > MIN_DELTA ||
            abs(firstStats.second - secondStats.second) > MIN_DELTA
        assertTrue("screenshots identical (frozen video?)", changed)
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

private const val STARTUP_MS = 800L
private const val SHOT_GAP_MS = 1_200L
private const val MIN_VARIANCE = 10.0
private const val MIN_DELTA = 0.5
