package com.example.auplayer.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Comparator contract over random names (MS-064): antisymmetric, transitive, numeric-aware. */
class NaturalOrderPropertyTest {
    @Test
    fun `numeric runs order by value not lexicographically`() {
        assertTrue(NaturalOrder.compare("2.jpg", "10.jpg") < 0)
        assertTrue(NaturalOrder.compare("10.jpg", "2.jpg") > 0)
        assertTrue(NaturalOrder.compare("page-2.jpg", "page-10.jpg") < 0)
    }

    @Test
    fun `random triples are transitive`() {
        val random = Random(0x9A7AL)
        repeat(500) {
            val names = List(3) { randomName(random) }
            val ab = NaturalOrder.compare(names[0], names[1])
            val bc = NaturalOrder.compare(names[1], names[2])
            val ac = NaturalOrder.compare(names[0], names[2])
            assertEquals(-ab.sign(), NaturalOrder.compare(names[1], names[0]).sign())
            if (ab <= 0 && bc <= 0) {
                assertTrue("transitivity: $names", ac <= 0)
            }
            if (ab >= 0 && bc >= 0) {
                assertTrue("transitivity: $names", ac >= 0)
            }
        }
    }

    @Test
    fun `sorted output is ordered`() {
        val random = Random(0x5077L)
        repeat(200) {
            val names = List(random.nextInt(1, 30)) { randomName(random) }
            val sorted = names.sortedWith(NaturalOrder)
            for (index in 0 until sorted.size - 1) {
                assertTrue(
                    "unordered pair in $sorted",
                    NaturalOrder.compare(sorted[index], sorted[index + 1]) <= 0
                )
            }
        }
    }

    private fun randomName(random: Random): String {
        val stem = when (random.nextInt(4)) {
            0 -> "page"
            1 -> "Page"
            2 -> "img_"
            else -> "x"
        }
        val number = when (random.nextInt(3)) {
            0 -> random.nextInt(0, 200).toString()
            1 -> random.nextInt(0, 200).toString().padStart(random.nextInt(1, 6), '0')
            else -> ""
        }
        val ext = listOf(".jpg", ".png", ".jpeg").random(random)
        return "$stem$number$ext"
    }

    private fun Int.sign(): Int = when {
        this < 0 -> -1
        this > 0 -> 1
        else -> 0
    }
}
