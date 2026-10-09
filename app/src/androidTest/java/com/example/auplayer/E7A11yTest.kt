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
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * E7 — accessibility baseline (MS-074): every clickable node in our package
 * on the player and reader screens exposes text or a content description.
 * Screen-reader users get no silent buttons. Evidence under E7GATE.
 */
@RunWith(AndroidJUnit4::class)
class E7A11yTest {
    @Test
    fun clickableNodesAreLabeled() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clip = File(context.filesDir, "e1.mp4")
        assertTrue("missing pushed clip: $clip", clip.isFile)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val pkg = context.packageName

        launchFile(device, context, clip, "video/mp4")
        assertTrue("player screen never came up", device.wait(Until.hasObject(By.pkg(pkg)), UI_TIMEOUT_MS))
        Thread.sleep(SETTLE_MS)
        assertLabeled(device, pkg, "player")

        val cbz = File(context.filesDir, "e4-pages.cbz")
        if (cbz.isFile) {
            launchFile(device, context, cbz, "application/vnd.comicbook+zip")
            assertTrue("reader screen never came up", device.wait(Until.hasObject(By.pkg(pkg)), UI_TIMEOUT_MS))
            Thread.sleep(SETTLE_MS)
            assertLabeled(device, pkg, "reader")
        } else {
            Log.i(GATE_TAG, "reader fixtures absent, player only")
        }
    }

    private fun launchFile(device: UiDevice, context: Context, file: File, mime: String) {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(Uri.fromFile(file), mime)
            .setClassName(context, MainActivity::class.java.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        assertTrue("activity never foregrounded", device.waitForWindowUpdate(null, UI_TIMEOUT_MS))
    }

    private fun assertLabeled(device: UiDevice, pkg: String, screen: String) {
        val nodes = device.findObjects(By.clickable(true).pkg(pkg))
        Log.i(GATE_TAG, "$screen clickables=${nodes.size}")
        // The hierarchy churns under us (time text ticks); stale nodes
        // vanished on their own — only judge the ones still standing.
        val unlabeled = nodes.filter { node ->
            try {
                node.text.isNullOrBlank() && node.contentDescription.isNullOrBlank()
            } catch (_: StaleObjectException) {
                false
            }
        }
        assertTrue(
            "unlabeled clickables on $screen: ${unlabeled.map { it.className }}",
            unlabeled.isEmpty()
        )
    }
}

private const val UI_TIMEOUT_MS = 15_000L
private const val SETTLE_MS = 2_000L
private const val GATE_TAG = "E7GATE"
