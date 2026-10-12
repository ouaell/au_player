package app.auplayer.player

import org.junit.Assume.assumeTrue
import org.junit.Test

/** Real-bridge tests run only where native libs exist (emulator/T2). */
class LibmpvBridgeTest {
    @Test
    fun nativeLoadsWhereBundled() {
        assumeTrue(MpvNative.load())
    }
}
