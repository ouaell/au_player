package com.example.auplayer.enhance

import com.example.auplayer.core.EnhanceMode
import com.example.auplayer.player.FakeMpvBridge
import com.example.auplayer.player.PlayerEngine
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompareHolderTest {
    private fun setup(): Triple<CompareHolder, EnhanceController, FakeMpvBridge> {
        val bridge = FakeMpvBridge()
        val controller = EnhanceController(PlayerEngine(bridge, Dispatchers.Unconfined))
        return Triple(CompareHolder(controller), controller, bridge)
    }

    @Test
    fun `press clears and release restores`() {
        val (holder, controller, bridge) = setup()
        controller.apply(EnhanceMode.ANIME, 1080)
        val applied = bridge.commands.size
        holder.press(EnhanceMode.ANIME, 0)
        assertTrue(holder.holding())
        assertTrue(bridge.commands.size > applied)
        holder.release(1080)
        assertFalse(holder.holding())
        assertEquals(EnhanceMode.ANIME, controller.currentMode)
        assertTrue(bridge.commands.last().joinToString(" ").contains("Anime4K_Upscale_CNN_x2_M"))
    }

    @Test
    fun `release after off-mode press stays cleared`() {
        val (holder, controller, _) = setup()
        holder.press(EnhanceMode.OFF, 0)
        holder.release(1080)
        assertEquals(EnhanceMode.OFF, controller.currentMode)
        assertFalse(holder.holding())
    }

    @Test
    fun `release without press is a no-op`() {
        val (holder, controller, bridge) = setup()
        val before = bridge.commands.size
        holder.release(1080)
        assertEquals(before, bridge.commands.size)
        assertEquals(EnhanceMode.OFF, controller.currentMode)
    }
}
