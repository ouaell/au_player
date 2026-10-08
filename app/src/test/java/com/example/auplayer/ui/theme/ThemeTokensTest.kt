package com.example.auplayer.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

/** JVM contract test: §10 token values must stay exact until MS-005+ retunes them. */
class ThemeTokensTest {
    @Test
    fun `section ten palette is exact`() {
        assertEquals(Color(0xFF000000), bg)
        assertEquals(Color(0xFF101015), surface)
        assertEquals(Color(0xFF1A1A22), surfaceHigh)
        assertEquals(Color(0xFF2A2A35), outline)
        assertEquals(Color(0xFFECECF2), onSurface)
        assertEquals(Color(0xFF9A9AAB), muted)
        assertEquals(Color(0xFF7C5CFF), accent)
        assertEquals(Color(0xFF00D1FF), accent2)
        assertEquals(Color(0xFFFF5C6C), danger)
        assertEquals(Color(0xFF3DDC97), success)
    }

    @Test
    fun `spacing scale matches section ten`() {
        assertEquals(4.dp, Spacing.s4)
        assertEquals(8.dp, Spacing.s8)
        assertEquals(12.dp, Spacing.s12)
        assertEquals(16.dp, Spacing.s16)
        assertEquals(24.dp, Spacing.s24)
        assertEquals(32.dp, Spacing.s32)
        assertEquals(20.dp, Spacing.card)
        assertEquals(28.dp, Spacing.sheet)
    }

    @Test
    fun `type scale matches section ten`() {
        assertEquals(12.sp, auType.labelSmall.fontSize)
        assertEquals(14.sp, auType.labelMedium.fontSize)
        assertEquals(16.sp, auType.bodyMedium.fontSize)
        assertEquals(20.sp, auType.titleMedium.fontSize)
        assertEquals(28.sp, auType.titleLarge.fontSize)
        assertEquals(36.sp, auType.displaySmall.fontSize)
    }
}
