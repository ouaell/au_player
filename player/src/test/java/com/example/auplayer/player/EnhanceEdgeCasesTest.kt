package com.example.auplayer.player

import com.example.auplayer.core.Diag
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnhanceEdgeCasesTest {
    private fun setup(): Triple<EnhanceEdgeCases, SurfaceHost, FakeMpvBridge> {
        val bridge = FakeMpvBridge()
        val engine = PlayerEngine(bridge, Dispatchers.Unconfined)
        val surfaces = SurfaceHost(engine)
        return Triple(EnhanceEdgeCases(engine, surfaces), surfaces, bridge)
    }

    @Test
    fun `surface loss detaches and reattach works`() {
        val (edges, surfaces, bridge) = setup()
        val token = Any()
        surfaces.onSurfaceCreated(token)
        edges.onSurfaceLost()
        assertEquals(null, bridge.attached)
        surfaces.onSurfaceCreated(token)
        assertEquals(token, bridge.attached)
    }

    @Test
    fun `seek without surface still issues the command`() {
        val (edges, _, bridge) = setup()
        edges.seekSafe(30.0)
        assertEquals(listOf(listOf("seek", "30.0", "absolute")), bridge.commands)
    }

    @Test
    fun `decoder fallback switches to software and logs`() {
        Diag.clear()
        val (edges, _, bridge) = setup()
        edges.onDecoderError()
        assertEquals(listOf(listOf("set", "hwdec", "no")), bridge.commands)
        assertTrue(Diag.entries().any { it.tag == "decoder" })
        Diag.clear()
    }
}
