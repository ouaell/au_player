package com.example.auplayer.enhance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RungProbeTest {
    @Test
    fun `picks highest fitting rung at normal budget`() {
        // 30 fps → 16.67 ms interval → budget 9.17 ms → rungs 1 and 2 fit
        assertEquals(2, RungProbe.choose(mapOf(0 to 12.0, 1 to 8.0, 2 to 3.0), 16.67, 30.0))
    }

    @Test
    fun `high fps uses the tight budget`() {
        // fps 60 ≥ 50 → 35% of 16.67 = 5.83 ms
        assertEquals(2, RungProbe.choose(mapOf(0 to 12.0, 1 to 8.0, 2 to 3.0), 16.67, 60.0))
        assertNull(RungProbe.choose(mapOf(0 to 9.0), 16.67, 60.0))
    }

    @Test
    fun `nothing fitting means off`() {
        assertNull(RungProbe.choose(mapOf(0 to 99.0), 16.67, 24.0))
        assertNull(RungProbe.choose(emptyMap(), 16.67, 24.0))
        assertNull(RungProbe.choose(mapOf(0 to 1.0), 0.0, 24.0))
    }
}
