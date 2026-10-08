package com.example.auplayer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelfTestTest {
    @Test
    fun `jvm probes pass with sane inputs`() {
        val results = SelfTest.jvmProbes("0.1.0", 3, cacheWritable = true, freeMb = 512)
        assertEquals(ProbeStatus.PASS, results.first { it.name == "version" }.status)
        assertEquals(ProbeStatus.PASS, results.first { it.name == "cache" }.status)
        assertTrue(results.any { it.status == ProbeStatus.PENDING_DEVICE })
    }

    @Test
    fun `report is well formed json`() {
        val json = SelfTest.reportJson("0.1.0", SelfTest.jvmProbes("0.1.0", 0, true, 1))
        assertTrue(json.startsWith("{\"app\":\"0.1.0\",\"probes\":["))
        assertTrue(json.endsWith("]}"))
        assertTrue(json.contains("\"name\":\"playback-hw\""))
    }
}
