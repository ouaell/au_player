package app.auplayer.enhance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GovernorTest {
    @Test
    fun `clean samples never move`() {
        val governor = EnhanceGovernor()
        governor.startVideo(batterySaver = false)
        repeat(10) {
            assertEquals(GovernorDecision.STAY, governor.sample(it.toLong(), 0L, 0))
        }
        assertEquals(0, governor.rung)
    }

    @Test
    fun `three bad samples step down once`() {
        val governor = EnhanceGovernor()
        governor.startVideo(batterySaver = false)
        assertEquals(GovernorDecision.STAY, governor.sample(3, 0, 0))
        assertEquals(GovernorDecision.STAY, governor.sample(6, 0, 0))
        assertEquals(GovernorDecision.STEP_DOWN, governor.sample(9, 0, 0))
        assertEquals(1, governor.rung)
        assertTrue(governor.lowered)
    }

    @Test
    fun `streak resets on a clean sample`() {
        val governor = EnhanceGovernor()
        governor.startVideo(batterySaver = false)
        governor.sample(3, 0, 0)
        governor.sample(6, 0, 0)
        assertEquals(GovernorDecision.STAY, governor.sample(6, 0, 0))
        assertEquals(GovernorDecision.STAY, governor.sample(9, 0, 0))
        assertEquals(0, governor.rung)
    }

    @Test
    fun `thermal steps down immediately`() {
        val governor = EnhanceGovernor()
        governor.startVideo(batterySaver = false)
        assertEquals(GovernorDecision.STEP_DOWN, governor.sample(0, 0, 2))
        assertEquals(GovernorDecision.AT_BOTTOM, governor.sample(0, 0, 3))
    }

    @Test
    fun `battery saver starts one rung lower`() {
        val governor = EnhanceGovernor()
        governor.startVideo(batterySaver = true)
        assertEquals(1, governor.rung)
        assertTrue(governor.lowered)
    }

    @Test
    fun `new video restores the top rung`() {
        val governor = EnhanceGovernor()
        governor.startVideo(batterySaver = false)
        governor.sample(0, 0, 2)
        assertEquals(1, governor.rung)
        governor.startVideo(batterySaver = false)
        assertEquals(0, governor.rung)
    }

    @Test
    fun `never steps up within a video`() {
        val governor = EnhanceGovernor(maxRung = 2)
        governor.startVideo(batterySaver = false)
        var peak = 0
        val totals = longArrayOf(0, 5, 5, 9, 9, 9, 9, 20, 20, 20, 20, 0, 0, 0, 50, 60, 70, 80)
        var total = 0L
        totals.forEachIndexed { index, step ->
            total += step
            governor.sample(total, 0, if (index == 5) 2 else 0)
            peak = maxOf(peak, governor.rung)
            assertTrue(governor.rung <= peak)
        }
        assertEquals(2, governor.rung)
    }
}
