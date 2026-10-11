package com.example.auplayer

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import android.content.Context
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E-test scaffold (MS-060). E1–E9 scenarios land as the native engine does;
 * today this proves the app boots to first frame with no crash (logcat is
 * asserted clean by the workflow).
 */
@RunWith(AndroidJUnit4::class)
class SmokeTest {
    @Test
    fun appLaunchesWithoutCrash() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ActivityScenario.launch(MainActivity::class.java).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            // Anti-no-op: our package must actually own the foreground.
            assertTrue(
                "app never foregrounded",
                device.wait(Until.hasObject(By.pkg(context.packageName)), FOREGROUND_MS)
            )
        }
    }
}

private const val FOREGROUND_MS = 10_000L
