package app.auplayer.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests with fakes (MS-007 done-when). Deterministic: Unconfined dispatcher. */
class PlayerEngineTest {
    private fun engine(bridge: FakeMpvBridge = FakeMpvBridge()) =
        PlayerEngine(bridge, Dispatchers.Unconfined)

    @Test
    fun `open after attach issues loadfile immediately`() {
        val bridge = FakeMpvBridge()
        val engine = engine(bridge)
        engine.attach(Any())
        engine.open("file:///x.mp4")
        assertEquals(listOf(listOf("loadfile", "file:///x.mp4")), bridge.commands)
    }

    @Test
    fun `open without surface still loads after timeout`() = runBlocking {
        val bridge = FakeMpvBridge()
        PlayerEngine(bridge, Dispatchers.Unconfined, openSurfaceTimeoutMs = 200).open("file:///x.mp4")
        withTimeout(5_000) {
            while (bridge.commands.isEmpty()) delay(10)
        }
        assertEquals(listOf(listOf("loadfile", "file:///x.mp4")), bridge.commands)
    }

    @Test
    fun `open waits for a later attach`() = runBlocking {
        val bridge = FakeMpvBridge()
        val engine = PlayerEngine(bridge, Dispatchers.Unconfined, openSurfaceTimeoutMs = 5_000)
        engine.open("file:///x.mp4")
        assertEquals(emptyList<List<String>>(), bridge.commands)
        engine.attach(Any())
        withTimeout(5_000) {
            while (bridge.commands.isEmpty()) delay(10)
        }
        assertEquals(listOf(listOf("loadfile", "file:///x.mp4")), bridge.commands)
    }

    @Test
    fun `play and pause set pause property`() {
        val bridge = FakeMpvBridge()
        val engine = engine(bridge)
        engine.play()
        engine.pause()
        assertEquals(
            listOf(listOf("set", "pause", "no"), listOf("set", "pause", "yes")),
            bridge.commands,
        )
    }

    @Test
    fun `seek uses absolute seconds`() {
        val bridge = FakeMpvBridge()
        engine(bridge).seekTo(42.5)
        assertEquals(listOf(listOf("seek", "42.5", "absolute")), bridge.commands)
    }

    @Test
    fun `bridge events reach observers`() = runBlocking {
        val bridge = FakeMpvBridge()
        val engine = engine(bridge)
        val received = mutableListOf<MpvEvent>()
        val job = launch(Dispatchers.Unconfined) {
            engine.events.collect { received += it }
        }
        bridge.emit(MpvEvent.TimePos(1.5))
        withTimeout(5_000) {
            while (received.isEmpty()) {
                delay(10)
            }
        }
        job.cancel()
        assertEquals(listOf(MpvEvent.TimePos(1.5)), received)
    }

    @Test
    fun `engine controller delegates to engine`() {
        val bridge = FakeMpvBridge()
        val controller: PlayerController = EnginePlayerController(engine(bridge))
        controller.play()
        controller.pause()
        assertEquals(
            listOf(listOf("set", "pause", "no"), listOf("set", "pause", "yes")),
            bridge.commands,
        )
    }
}
