package com.example.auplayer.player

import org.junit.Assert.assertEquals
import org.junit.Test

class GestureIndicatorTest {
    @Test
    fun `seek shows signed seconds`() {
        assertEquals("+10s", indicatorText(GestureIndicator(IndicatorKind.SEEK, 10f / 120f)))
        assertEquals("-5s", indicatorText(GestureIndicator(IndicatorKind.SEEK, -5f / 120f)))
    }

    @Test
    fun `brightness and volume show percent`() {
        assertEquals("75%", indicatorText(GestureIndicator(IndicatorKind.BRIGHTNESS, 0.75f)))
        assertEquals("30%", indicatorText(GestureIndicator(IndicatorKind.VOLUME, 0.3f)))
    }

    @Test
    fun `speed and zoom have fixed formats`() {
        assertEquals("2×", indicatorText(GestureIndicator(IndicatorKind.SPEED, 2f)))
        assertEquals("1.5x", indicatorText(GestureIndicator(IndicatorKind.ZOOM, 1.5f)))
    }
}
