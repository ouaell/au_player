package app.auplayer.player

import org.junit.Assert.assertEquals
import org.junit.Test

/** Table-driven gesture→action tests (MS-013 done-when). */
class GesturesTest {
    private val config = GestureConfig()

    @Test
    fun `horizontal drag scales with duration`() {
        assertEquals(
            GestureAction.SeekBy(30_000),
            GestureMapper.horizontalDrag(0.25f, 120_000, locked = false)
        )
        assertEquals(
            GestureAction.SeekBy(-12_000),
            GestureMapper.horizontalDrag(-0.1f, 120_000, locked = false)
        )
    }

    @Test
    fun `vertical drags invert and split sides`() {
        assertEquals(
            GestureAction.AdjustBrightness(-0.2f),
            GestureMapper.leftVertical(0.2f, locked = false)
        )
        assertEquals(
            GestureAction.AdjustVolume(0.3f),
            GestureMapper.rightVertical(-0.3f, locked = false)
        )
    }

    @Test
    fun `double tap zones map correctly`() {
        assertEquals(
            GestureAction.SeekBy(-10_000),
            GestureMapper.doubleTap(TapZone.LEFT, config, locked = false)
        )
        assertEquals(
            GestureAction.SeekBy(10_000),
            GestureMapper.doubleTap(TapZone.RIGHT, config, locked = false)
        )
        assertEquals(
            GestureAction.TogglePlay,
            GestureMapper.doubleTap(TapZone.CENTER, config, locked = false)
        )
    }

    @Test
    fun `double tap seconds are configurable`() {
        val custom = GestureConfig(doubleTapSeconds = 30)
        assertEquals(
            GestureAction.SeekBy(30_000),
            GestureMapper.doubleTap(TapZone.RIGHT, custom, locked = false)
        )
    }

    @Test
    fun `long press toggles hold speed`() {
        assertEquals(
            GestureAction.HoldSpeed(true),
            GestureMapper.longPress(active = true, locked = false)
        )
        assertEquals(
            GestureAction.HoldSpeed(false),
            GestureMapper.longPress(active = false, locked = false)
        )
    }

    @Test
    fun `pinch clamps to min max`() {
        assertEquals(
            GestureAction.Zoom(4f),
            GestureMapper.pinch(9f, config, locked = false)
        )
        assertEquals(
            GestureAction.Zoom(0.5f),
            GestureMapper.pinch(0.1f, config, locked = false)
        )
        assertEquals(
            GestureAction.Zoom(2f),
            GestureMapper.pinch(2f, config, locked = false)
        )
    }

    @Test
    fun `single tap toggles controls`() {
        assertEquals(GestureAction.ToggleControls, GestureMapper.singleTap(locked = false))
    }

    @Test
    fun `everything is ignored while locked`() {
        assertEquals(GestureAction.Ignored, GestureMapper.horizontalDrag(0.5f, 60_000, locked = true))
        assertEquals(GestureAction.Ignored, GestureMapper.leftVertical(0.5f, locked = true))
        assertEquals(GestureAction.Ignored, GestureMapper.rightVertical(0.5f, locked = true))
        assertEquals(GestureAction.Ignored, GestureMapper.doubleTap(TapZone.LEFT, config, locked = true))
        assertEquals(GestureAction.Ignored, GestureMapper.longPress(active = true, locked = true))
        assertEquals(GestureAction.Ignored, GestureMapper.pinch(2f, config, locked = true))
        assertEquals(GestureAction.Ignored, GestureMapper.singleTap(locked = true))
    }
}
