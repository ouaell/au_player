package com.example.auplayer.enhance

import com.example.auplayer.core.EnhanceMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompareHolderTest {
    private fun setup(): Triple<CompareHolder, EnhanceController, FakeSender> {
        val sender = FakeSender()
        val controller = EnhanceController(sender)
        return Triple(CompareHolder(controller), controller, sender)
    }

    @Test
    fun `press clears and release restores`() {
        val (holder, controller, sender) = setup()
        controller.apply(EnhanceMode.ANIME, 1080)
        val applied = sender.sent.size
        holder.press(EnhanceMode.ANIME, 0)
        assertTrue(holder.holding())
        assertTrue(sender.sent.size > applied)
        holder.release(1080)
        assertFalse(holder.holding())
        assertEquals(EnhanceMode.ANIME, controller.currentMode)
        assertTrue(sender.sent.last().joinToString(" ").contains("Anime4K_Upscale_CNN_x2_M"))
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
        val (holder, controller, sender) = setup()
        val before = sender.sent.size
        holder.release(1080)
        assertEquals(before, sender.sent.size)
        assertEquals(EnhanceMode.OFF, controller.currentMode)
    }
}
