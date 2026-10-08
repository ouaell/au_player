package com.example.auplayer.player

import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Lifecycle matrix (MS-009 done-when): every create/destroy ordering is safe. */
class SurfaceHostTest {
    private fun host(bridge: FakeMpvBridge = FakeMpvBridge()) =
        SurfaceHost(PlayerEngine(bridge, Dispatchers.Unconfined)) to bridge

    @Test
    fun `create then destroy attaches then detaches`() {
        val (host, bridge) = host()
        val token = Any()
        host.onSurfaceCreated(token)
        assertTrue(host.hasSurface)
        assertEquals(token, bridge.attached)
        host.onSurfaceDestroyed()
        assertFalse(host.hasSurface)
        assertEquals(null, bridge.attached)
        assertEquals(1, bridge.detaches)
    }

    @Test
    fun `destroy without surface is a safe no-op`() {
        val (host, bridge) = host()
        host.onSurfaceDestroyed()
        assertEquals(0, bridge.detaches)
    }

    @Test
    fun `double destroy detaches once`() {
        val (host, bridge) = host()
        host.onSurfaceCreated(Any())
        host.onSurfaceDestroyed()
        host.onSurfaceDestroyed()
        assertEquals(1, bridge.detaches)
    }

    @Test
    fun `recreate re-attaches the new surface`() {
        val (host, bridge) = host()
        host.onSurfaceCreated(Any())
        val second = Any()
        host.onSurfaceDestroyed()
        host.onSurfaceCreated(second)
        assertTrue(host.hasSurface)
        assertEquals(second, bridge.attached)
        assertEquals(2, bridge.attaches)
    }

    @Test
    fun `null surface is ignored`() {
        val (host, bridge) = host()
        host.onSurfaceCreated(null)
        assertFalse(host.hasSurface)
        assertEquals(0, bridge.attaches)
    }

    @Test
    fun `create replaces a live surface`() {
        val (host, bridge) = host()
        host.onSurfaceCreated(Any())
        val replacement = Any()
        host.onSurfaceCreated(replacement)
        assertEquals(replacement, bridge.attached)
        assertEquals(2, bridge.attaches)
        assertEquals(0, bridge.detaches)
    }
}
