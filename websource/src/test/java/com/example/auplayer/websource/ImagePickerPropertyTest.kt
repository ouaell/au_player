package com.example.auplayer.websource

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Picker invariants over random candidates (MS-064): dedupe, order, size gate, absolute URLs. */
class ImagePickerPropertyTest {
    @Test
    fun `output is deduped DOM-ordered and filtered`() {
        val random = Random(0x91C4L)
        repeat(300) {
            val count = random.nextInt(0, 20)
            val candidates = List(count) { index ->
                ImageCandidate(
                    url = randomUrl(random),
                    width = random.nextInt(0, 2000),
                    height = random.nextInt(0, 2000),
                    fromAd = random.nextDouble() < 0.2,
                    order = index
                )
            }
            val pageUrl = "https://example.com/gallery/page"
            val picked = ImagePicker.pick(candidates, pageUrl, 300)
            assertEquals("duplicates leak", picked.distinct(), picked)
            for (url in picked) {
                assertTrue("relative url leaked: $url", url.startsWith("https://"))
            }
            val expected = candidates
                .filter { !it.fromAd && it.width >= 300 && it.height >= 300 }
                .mapNotNull { candidate ->
                    candidate.url?.takeUnless { raw -> raw.startsWith("data:") }?.let { raw ->
                        ImagePicker.resolveUrl(pageUrl, raw)
                    }
                }
                .distinct()
            assertEquals(expected, picked)
        }
    }

    private fun randomUrl(random: Random): String? {
        return when (random.nextInt(6)) {
            0 -> null
            1 -> "data:image/gif;base64,xyz"
            2 -> "/img/${random.nextInt(100)}.jpg"
            3 -> "https://cdn.example/${random.nextInt(100)}.jpg"
            4 -> "//cdn.example/${random.nextInt(100)}.jpg"
            else -> "https://example.com/gallery/${random.nextInt(100)}.jpg"
        }
    }
}
