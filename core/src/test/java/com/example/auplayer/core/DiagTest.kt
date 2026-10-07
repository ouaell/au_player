package com.example.auplayer.core

import org.junit.Assert.assertEquals
import org.junit.Test

class DiagTest {
    @Test
    fun `report returns the message unchanged`() {
        assertEquals("hello", Diag.report("hello"))
    }
}
