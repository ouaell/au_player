package com.example.auplayer.reader

import com.example.auplayer.core.ReaderFilter
import com.example.auplayer.core.ReaderModeSetting
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderUiTest {
    @Test
    fun `tap zones map with custom config`() {
        val config = TapZoneConfig(
            left = TapAction.MENU,
            center = TapAction.NEXT,
            right = TapAction.PREV
        )
        assertEquals(TapAction.MENU, actionFor(ReaderTapZone.LEFT, config))
        assertEquals(TapAction.NEXT, actionFor(ReaderTapZone.CENTER, config))
        assertEquals(TapAction.PREV, actionFor(ReaderTapZone.RIGHT, config))
    }

    @Test
    fun `progress guards empty books`() {
        assertEquals(0f, readerProgress(0, 0))
        assertEquals(0.5f, readerProgress(0, 2))
        assertEquals(1f, readerProgress(9, 10))
    }

    @Test
    fun `grayscale matrix rows are equal`() {
        val matrix = filterMatrix(ReaderFilter.GRAYSCALE)
        assertEquals(20, matrix.size)
        assertEquals(0.299f, matrix[0])
        assertEquals(0.587f, matrix[1])
        assertEquals(0.114f, matrix[2])
        assertEquals(1f, matrix[18])
    }

    @Test
    fun `only webtoon scrolls`() {
        assertEquals(false, isPagedMode(ReaderModeSetting.WEBTOON))
        assertEquals(true, isPagedMode(ReaderModeSetting.LTR))
        assertEquals(true, isPagedMode(ReaderModeSetting.DOUBLE_PAGE))
    }
}
