package com.example.auplayer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CrashLoggerTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `crash writes a file and logs to diag`() {
        Diag.clear()
        val dir = tmp.newFolder("crashes")
        val file = CrashLogger.onCrash(Thread.currentThread(), RuntimeException("boom"), dir)
        assertTrue(file.isFile)
        assertTrue(file.readText().contains("boom"))
        assertEquals(listOf(file.name), Diag.entries().map { it.message })
        assertEquals(listOf(file), CrashLogger.listCrashes(dir))
        Diag.clear()
    }

    @Test
    fun `report renders version and entries`() {
        val report = buildDiagnosticsReport(
            "0.1.0",
            listOf(DiagEntry("mpv", "startup options applied"))
        )
        assertTrue(report.contains("0.1.0"))
        assertTrue(report.contains("mpv: startup options applied"))
    }
}
