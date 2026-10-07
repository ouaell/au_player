package com.example.auplayer.player

import org.junit.Assert.assertEquals
import org.junit.Test

class FakePlayerControllerTest {
    @Test
    fun `play and pause are counted`() {
        val fake = FakePlayerController()
        fake.play()
        fake.play()
        fake.pause()
        assertEquals(2, fake.plays)
        assertEquals(1, fake.pauses)
    }
}
