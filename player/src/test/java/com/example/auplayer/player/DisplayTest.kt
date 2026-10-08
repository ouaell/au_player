package com.example.auplayer.player

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayTest {
    @Test
    fun `aspect cycles and wraps`() {
        assertEquals(AspectMode.FILL, cycleAspect(AspectMode.FIT))
        assertEquals(AspectMode.FIT, cycleAspect(AspectMode.CUSTOM))
    }

    @Test
    fun `fixed ratios scale to video height`() {
        assertEquals(Pair(1280, 720), aspectSize(AspectMode.SIXTEEN_NINE, 1280, 720, 1f))
        assertEquals(Pair(320, 240), aspectSize(AspectMode.FOUR_THREE, 320, 240, 1f))
        assertEquals(Pair(0, 0), aspectSize(AspectMode.FIT, 0, 0, 1f))
    }

    @Test
    fun `follow video picks the long edge`() {
        assertEquals(
            EffectiveOrientation.LANDSCAPE,
            effectiveOrientation(OrientationPref.FOLLOW_VIDEO, 1920, 1080)
        )
        assertEquals(
            EffectiveOrientation.PORTRAIT,
            effectiveOrientation(OrientationPref.FOLLOW_VIDEO, 720, 1280)
        )
        assertEquals(
            EffectiveOrientation.UNSPECIFIED,
            effectiveOrientation(OrientationPref.AUTO, 1920, 1080)
        )
    }

    @Test
    fun `zoom clamps and resets`() {
        val zoom = ZoomState()
        zoom.apply(9f)
        assertEquals(4f, zoom.factor)
        zoom.reset()
        assertEquals(1f, zoom.factor)
    }
}
