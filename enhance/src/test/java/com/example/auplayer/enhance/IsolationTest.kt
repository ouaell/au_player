package com.example.auplayer.enhance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IsolationTest {
    @Test
    fun `two native crashes disable the feature`() {
        val policy = IsolationPolicy()
        assertFalse(policy.onNativeCrash())
        assertEquals(1, policy.crashes)
        assertTrue(policy.onNativeCrash())
        assertTrue(policy.disabled)
    }

    @Test
    fun `reset clears the count`() {
        val policy = IsolationPolicy()
        policy.onNativeCrash()
        policy.onNativeCrash()
        policy.reset()
        assertFalse(policy.disabled)
        assertEquals(0, policy.crashes)
    }
}
