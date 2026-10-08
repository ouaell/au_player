package com.example.auplayer.library

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionsTest {
    @Test
    fun `permission splits at api 33`() {
        assertEquals(Manifest.permission.READ_MEDIA_VIDEO, mediaPermissionFor(33))
        assertEquals(Manifest.permission.READ_MEDIA_VIDEO, mediaPermissionFor(36))
        assertEquals(Manifest.permission.READ_EXTERNAL_STORAGE, mediaPermissionFor(32))
        assertEquals(Manifest.permission.READ_EXTERNAL_STORAGE, mediaPermissionFor(26))
    }

    @Test
    fun `partial access starts at api 34`() {
        assertFalse(supportsPartialAccess(33))
        assertTrue(supportsPartialAccess(34))
    }
}
