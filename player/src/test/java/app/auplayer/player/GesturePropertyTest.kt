package app.auplayer.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

/** Mapper invariants over random inputs (MS-064): locked mutes, finite outputs, bounded seeks. */
class GesturePropertyTest {
    @Test
    fun `locked always ignores`() {
        val random = Random(0x10CCEDL)
        val config = GestureConfig()
        repeat(500) {
            val dx = random.nextFloat() * 4 - 2
            val dy = random.nextFloat() * 4 - 2
            val duration = random.nextLong(0, 10_000)
            assertEquals(GestureAction.Ignored, GestureMapper.horizontalDrag(dx, duration, true))
            assertEquals(GestureAction.Ignored, GestureMapper.leftVertical(dy, true))
            assertEquals(GestureAction.Ignored, GestureMapper.rightVertical(dy, true))
            assertEquals(
                GestureAction.Ignored,
                GestureMapper.doubleTap(TapZone.values().random(random), config, true),
            )
        }
    }

    @Test
    fun `outputs stay finite and proportional`() {
        val random = Random(0x5EEDL)
        repeat(500) {
            val dx = random.nextFloat() * 4 - 2
            val duration = random.nextLong(0, 10_000)
            when (val action = GestureMapper.horizontalDrag(dx, duration, false)) {
                is GestureAction.SeekBy -> {
                    assertTrue(
                        "seek proportional",
                        abs(action.deltaMs) <= (abs(dx) * duration).toLong() + 1,
                    )
                }
                else -> throw AssertionError("unexpected $action")
            }
            val brightness = GestureMapper.leftVertical(dx, false)
            assertTrue(brightness is GestureAction.AdjustBrightness)
            val volume = GestureMapper.rightVertical(dx, false)
            assertTrue(volume is GestureAction.AdjustVolume)
        }
    }
}
