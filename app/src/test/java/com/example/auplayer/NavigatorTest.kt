package com.example.auplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigatorTest {
    @Test
    fun `starts at home`() {
        assertEquals(NavDest.Home, Navigator().current)
    }

    @Test
    fun `navigate then back restores previous`() {
        val nav = Navigator()
        nav.navigate(NavDest.Player)
        assertEquals(NavDest.Player, nav.current)
        assertTrue(nav.goBack())
        assertEquals(NavDest.Home, nav.current)
    }

    @Test
    fun `back at root returns false`() {
        assertFalse(Navigator().goBack())
    }

    @Test
    fun `deep stack unwinds in order`() {
        val nav = Navigator()
        nav.navigate(NavDest.Reader)
        nav.navigate(NavDest.Settings)
        assertTrue(nav.goBack())
        assertEquals(NavDest.Reader, nav.current)
        assertTrue(nav.goBack())
        assertEquals(NavDest.Home, nav.current)
        assertFalse(nav.goBack())
    }
}
