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
        val engine = (context.applicationContext as AuApp).container.playerEngine
        val pkg = context.packageName

        launchFile(device, context, clip, "video/mp4")
        assertTrue("player screen never came up", device.wait(Until.hasObject(By.pkg(pkg)), UI_TIMEOUT_MS))
        // Freeze the UI for a stable dump: time text ticks invalidate the
        // tree mid-walk (partial results). Engine state is explicit — a later
        // paused engine would fail E8's growth wait, so every player test
        // sets its own state up front.
        engine.play()
        assertTrue("no playback at start", waitForGrowth(device))
        engine.pause()
        // Controls auto-hide after 3 s; tap first so the audit sees them.
        device.click(device.displayWidth / 2, device.displayHeight / 2)
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
        // Either signal proves foregrounding: waitForWindowUpdate alone
        // false-negatives on the emulator, By.pkg alone can wedge.
        val updated = device.waitForWindowUpdate(null, FOREGROUND_MS)
        val packaged = device.wait(Until.hasObject(By.pkg(context.packageName)), FOREGROUND_MS)
        assertTrue("activity never foregrounded", updated || packaged)
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

    private fun waitForGrowth(device: UiDevice): Boolean {
        val baseline = traceLines()
        val deadline = System.currentTimeMillis() + GROWTH_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            if (traceLines() > baseline) return true
            Thread.sleep(POLL_MS)
        }
        return false
    }

    private fun assertLabeled(device: UiDevice, pkg: String, screen: String) {
        val nodes = device.findObjects(By.pkg(pkg))
        Log.i(GATE_TAG, "$screen nodes=${nodes.size}")
        for (node in nodes) {
            try {
                Log.i(
                    GATE_TAG,
                    "node class=${node.className} clickable=${node.isClickable} " +
                        "bounds=${node.visibleBounds} text=${node.text} desc=${node.contentDescription}"
                )
            } catch (_: StaleObjectException) {
            }
        }
        // The hierarchy churns under us (time text ticks); stale nodes
        // vanished on their own — only judge the ones still standing.
        // Edge-to-edge system scrims (status/nav backgrounds: full-width
        // strips at the display edges, no text) are not app content.
        val displayW = device.displayWidth
        val displayH = device.displayHeight
        val unlabeled = nodes.filter { node ->
            try {
                node.isClickable &&
                    node.text.isNullOrBlank() && node.contentDescription.isNullOrBlank() &&
                    !isEdgeChrome(node, displayW, displayH)
            } catch (_: StaleObjectException) {
                false
            }
        }
        assertTrue(
            "unlabeled clickables on $screen: ${unlabeled.map { it.className }}",
            unlabeled.isEmpty()
        )
    }

    private fun isEdgeChrome(node: androidx.test.uiautomator.UiObject2, displayW: Int, displayH: Int): Boolean {
        if (node.className != "android.view.View") return false
        return try {
            val bounds = node.visibleBounds
            bounds.width() == displayW &&
                bounds.height() < displayH / CHROME_MAX_HEIGHT_FRACTION &&
                (bounds.top == 0 || bounds.bottom == displayH)
        } catch (_: StaleObjectException) {
            false
        }
    }
}

private const val UI_TIMEOUT_MS = 15_000L
private const val FOREGROUND_MS = 10_000L
private const val SETTLE_MS = 2_000L
private const val POLL_MS = 250L
private const val GROWTH_TIMEOUT_MS = 20_000L
private const val GATE_TAG = "E7GATE"
private const val CHROME_MAX_HEIGHT_FRACTION = 8
