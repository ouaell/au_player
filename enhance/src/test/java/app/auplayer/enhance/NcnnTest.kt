package app.auplayer.enhance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NcnnTest {
    @Test
    fun `vulkan path wins when available`() {
        val engine = NcnnEngine(FakeNcnnBridge(vulkanWorks = true))
        assertTrue(engine.init(vulkanAvailable = true, pagePixels = 50_000_000L))
        assertEquals(NcnnBackend.VULKAN, engine.backend)
    }

    @Test
    fun `cpu only for small pages`() {
        val engine = NcnnEngine(FakeNcnnBridge(vulkanWorks = false))
        assertTrue(engine.init(vulkanAvailable = false, pagePixels = 1_000_000L))
        assertEquals(NcnnBackend.CPU, engine.backend)
        val big = NcnnEngine(FakeNcnnBridge(vulkanWorks = false))
        assertFalse(big.init(vulkanAvailable = false, pagePixels = 50_000_000L))
        assertEquals(NcnnBackend.DISABLED, big.backend)
    }

    @Test
    fun `missing native library disables gracefully`() {
        val engine = NcnnEngine(object : NcnnBridge by FakeNcnnBridge() {
            override fun init(): Boolean = false
        })
        assertFalse(engine.init(vulkanAvailable = true, pagePixels = 100L))
        assertEquals(NcnnBackend.DISABLED, engine.backend)
    }

    @Test
    fun `jni bridge reports unavailable without crashing`() {
        // No native lib bundled in unit tests: init must return false, not throw.
        assertFalse(JniNcnnBridge.init())
    }
}
