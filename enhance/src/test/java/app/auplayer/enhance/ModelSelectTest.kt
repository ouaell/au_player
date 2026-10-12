package app.auplayer.enhance

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelSelectTest {
    @Test
    fun `upscale below one-and-half display width`() {
        assertTrue(ModelSelect.needsUpscale(1000, 1080))
        assertFalse(ModelSelect.needsUpscale(2000, 1080))
        assertFalse(ModelSelect.needsUpscale(1620, 1080))
    }
}
