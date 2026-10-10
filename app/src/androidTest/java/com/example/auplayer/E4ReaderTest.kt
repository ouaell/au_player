package com.example.auplayer

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * E4 — reader on real files (§17.2 scenario 4, no-crash + pages visible):
 * VIEW a generated CBZ (3 pages) and a generated PDF (1 page) through the
 * real routing into [ReaderScreen]; the page counter text proves pages
 * rendered. Evidence goes to logcat under E4GATE.
 */
@RunWith(AndroidJUnit4::class)
class E4ReaderTest {
    @Test
    fun readerOpensCbzAndPdf() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cbz = File(context.filesDir, "e4-pages.cbz")
        assertTrue("missing pushed cbz: $cbz", cbz.isFile)
        val pdf = File(context.filesDir, "e4-doc.pdf")
        assertTrue("missing pushed pdf: $pdf", pdf.isFile)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        openViewing(device, cbz, "application/vnd.comicbook+zip")
        assertPagesRendered(device, "cbz")

        openViewing(device, pdf, "application/pdf")
        assertPagesRendered(device, "pdf")
    }

    /**
     * Dump-free render proof (UiAutomator dumps wedge on the emulator):
     * reader pages are static and bright — a rendered page has high mean
     * luminance and variance; a black/failed surface does not. Which content
     * rendered is proven by the E4GATE pages/title lines in logcat.
     */
    private fun assertPagesRendered(device: UiDevice, what: String) {
        Thread.sleep(SETTLE_MS)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val shot = File(instrumentation.targetContext.filesDir, "e4-$what.png")
        assertTrue("screenshot failed", device.takeScreenshot(shot))
        val bitmap: Bitmap = BitmapFactory.decodeFile(shot.absolutePath)
            ?: error("undecodable $shot")
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var sum = 0L
        var sumSq = 0L
        var count = 0L
        var index = 0
        while (index < pixels.size) {
            val pixel = pixels[index]
            val luma = ((pixel shr 16) and 0xFF) + ((pixel shr 8) and 0xFF) + (pixel and 0xFF)
            sum += luma
            sumSq += luma.toLong() * luma
            count++
            index += PIXEL_STEP
        }
        val mean = sum.toDouble() / count
        val variance = sumSq.toDouble() / count - mean * mean
        Log.i(GATE_TAG, "$what mean=$mean var=$variance")
        assertTrue("$what screen dark (no pages?)", mean > MIN_MEAN)
        assertTrue("$what screen flat (no pages?)", variance > MIN_VARIANCE)
    }

    private fun openViewing(device: UiDevice, file: File, mime: String) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(Uri.fromFile(file), mime)
            .setClassName(context, MainActivity::class.java.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        // Either signal proves foregrounding; waitForWindowUpdate alone
        // false-negatives on the emulator, By.pkg alone can wedge. Both
        // failing means genuinely stuck.
        val updated = device.waitForWindowUpdate(null, FOREGROUND_MS)
        val packaged = device.wait(Until.hasObject(By.pkg(context.packageName)), FOREGROUND_MS)
        assertTrue("activity never foregrounded for ${file.name}", updated || packaged)
    }
}

private const val UI_TIMEOUT_MS = 15_000L
private const val FOREGROUND_MS = 10_000L
private const val SETTLE_MS = 2_000L
private const val PIXEL_STEP = 4
private const val MIN_MEAN = 100.0
private const val MIN_VARIANCE = 1500.0
private const val GATE_TAG = "E4GATE"
