package com.example.auplayer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    @Test
    fun `parses tag from release json`() {
        assertEquals(
            "v0.2.0",
            UpdateChecker.parseLatestTag("{\"tag_name\": \"v0.2.0\", \"draft\": false}")
        )
        assertNull(UpdateChecker.parseLatestTag("{}"))
    }

    @Test
    fun `numeric compare ignores leading v`() {
        assertTrue(UpdateChecker.isNewer("v0.2.0", "0.1.9"))
        assertFalse(UpdateChecker.isNewer("0.1.0", "0.1.0"))
        assertFalse(UpdateChecker.isNewer("0.1.0", "0.2.0"))
        assertTrue(UpdateChecker.isNewer("0.1.10", "0.1.9"))
    }
}
