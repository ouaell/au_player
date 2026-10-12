package app.auplayer.enhance

import app.auplayer.aucore.Diag

/**
 * NCNN seam (MS-045). Prebuilt Android+Vulkan libs land with the native
 * build; until then nothing links native code. `JniNcnnBridge` degrades to
 * "unavailable" when the library is absent, never crashing the caller.
 */
interface NcnnBridge {
    fun init(): Boolean
    fun initVulkan(): Boolean
    fun inferTile(pixels: IntArray, width: Int, height: Int): IntArray
    fun release()
}

object JniNcnnBridge : NcnnBridge {
    var available: Boolean = false
        private set

    override fun init(): Boolean {
        available = try {
            System.loadLibrary("ncnn_jni")
            true
        } catch (error: UnsatisfiedLinkError) {
            Diag.record("ncnn", "native missing: " + error.message.toString())
            false
        }
        return available
    }

    override fun initVulkan(): Boolean = false

    override fun inferTile(pixels: IntArray, width: Int, height: Int): IntArray {
        throw UnsupportedOperationException("native ncnn not bundled yet")
    }

    override fun release() = Unit
}

/** Fake for unit tests. */
class FakeNcnnBridge(
    var vulkanWorks: Boolean = true,
    var scale: Int = 2
) : NcnnBridge {
    var releases = 0
    override fun init(): Boolean = true
    override fun initVulkan(): Boolean = vulkanWorks
    override fun inferTile(pixels: IntArray, width: Int, height: Int): IntArray {
        return Tiler.upscaleNearest(pixels, width, height, scale)
    }
    override fun release() {
        releases++
    }
}

enum class NcnnBackend {
    VULKAN,
    CPU,
    DISABLED
}

/**
 * Backend policy (MS-045): Vulkan preferred; CPU only for small pages;
 * anything else disables the feature with a message instead of crashing.
 */
class NcnnEngine(private val bridge: NcnnBridge) {
    var backend: NcnnBackend = NcnnBackend.DISABLED
        private set

    fun init(vulkanAvailable: Boolean, pagePixels: Long): Boolean {
        if (!bridge.init()) {
            backend = NcnnBackend.DISABLED
            return false
        }
        backend = if (vulkanAvailable && bridge.initVulkan()) {
            NcnnBackend.VULKAN
        } else if (pagePixels <= CPU_MAX_PIXELS) {
            NcnnBackend.CPU
        } else {
            NcnnBackend.DISABLED
        }
        return backend != NcnnBackend.DISABLED
    }
}

private const val CPU_MAX_PIXELS = 2_000_000L
