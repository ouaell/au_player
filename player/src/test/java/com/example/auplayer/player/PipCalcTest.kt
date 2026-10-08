package com.example.auplayer.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PipCalcTest {
    @Test
    fun `aspect reduces by gcd`() {
        assertEquals(Pair(16, 9), PipCalc.aspect(1920, 1080))
        assertEquals(Pair(4, 3), PipCalc.aspect(640, 480))
        assertEquals(Pair(1, 1), PipCalc.aspect(1080, 1080))
    }

    @Test
    fun `invalid video falls back to sixteen nine`() {
        assertEquals(Pair(16, 9), PipCalc.aspect(0, 0))
        assertEquals(Pair(16, 9), PipCalc.aspect(-4, 1080))
    }
}
