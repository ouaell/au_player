package com.example.auplayer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.test.core.app.ActivityScenario
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
        assertTrue("cbz pages not shown", device.wait(Until.hasObject(By.textContains("1 / 3")), UI_TIMEOUT_MS))
        Log.i(GATE_TAG, "cbz 3 pages shown")

        openViewing(device, pdf, "application/pdf")
        assertTrue("pdf pages not shown", device.wait(Until.hasObject(By.textContains("1 / 1")), UI_TIMEOUT_MS))
        Log.i(GATE_TAG, "pdf 1 page shown")
    }

    private fun openViewing(device: UiDevice, file: File, mime: String) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(Uri.fromFile(file), mime)
            .setClassName(context, MainActivity::class.java.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        // The new activity must come up (crash = no window / ANR instead).
        assertTrue("activity never foregrounded for ${file.name}", device.waitForWindowUpdate(null, UI_TIMEOUT_MS))
    }
}

private const val UI_TIMEOUT_MS = 15_000L
private const val GATE_TAG = "E4GATE"
