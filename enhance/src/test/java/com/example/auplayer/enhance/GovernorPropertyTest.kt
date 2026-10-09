package com.example.auplayer.enhance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Property streams for the governor (MS-064): never steps up, stays in range, deterministic. */
class GovernorPropertyTest {
    @Test
    fun `rung never decreases within a video`() {
        val random = Random(0x60EDL)
        repeat(200) {
            val governor = EnhanceGovernor()
            governor.startVideo(batterySaver = random.nextBoolean())
            var drops = 0L
            var decoder = 0L
            var lastRung = governor.rung
            repeat(20) {
                drops += random.nextLong(0, 8)
                decoder += random.nextLong(0, 4)
                governor.sample(drops, decoder, random.nextInt(0, 7))
                assertTrue("rung stepped up", governor.rung >= lastRung)
                assertTrue("rung negative", governor.rung >= 0)
                lastRung = governor.rung
            }
        }
    }

    @Test
    fun `same stream replays identically`() {
        val first = runStream(1234L)
        val second = runStream(1234L)
        assertEquals(first, second)
    }

    private fun runStream(seed: Long): List<Int> {
        val random = Random(seed)
        val governor = EnhanceGovernor()
        governor.startVideo(batterySaver = false)
        val rungs = ArrayList<Int>()
        var drops = 0L
        var decoder = 0L
        repeat(20) {
            drops += random.nextLong(0, 8)
            decoder += random.nextLong(0, 4)
            governor.sample(drops, decoder, random.nextInt(0, 7))
            rungs += governor.rung
        }
        return rungs
    }
}
