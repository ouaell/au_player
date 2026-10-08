package com.example.auplayer

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
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
        ActivityScenario.launch(MainActivity::class.java).use {
            assertTrue(true)
        }
    }
}
