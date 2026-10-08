package com.example.auplayer.core

/** Self-test probe outcome (MS-065). Device-only probes report PENDING here. */
enum class ProbeStatus {
    PASS,
    FAIL,
    PENDING_DEVICE
}

data class ProbeResult(val name: String, val status: ProbeStatus, val detail: String)

/**
 * JVM-runnable probes (MS-065 framework). The on-device runner (T2/T3) adds
 * playback/NCNN/subtitle/PiP probes and fills the same report schema.
 */
object SelfTest {
    fun jvmProbes(
        appVersion: String,
        diagEntries: Int,
        cacheWritable: Boolean,
        freeMb: Long
    ): List<ProbeResult> {
        return listOf(
            ProbeResult("version", ProbeStatus.PASS, appVersion),
            ProbeResult(
                "diagnostics",
                if (diagEntries >= 0) ProbeStatus.PASS else ProbeStatus.FAIL,
                "entries=$diagEntries"
            ),
            ProbeResult(
                "cache",
                if (cacheWritable) ProbeStatus.PASS else ProbeStatus.FAIL,
                "writable=$cacheWritable"
            ),
            ProbeResult(
                "storage",
                if (freeMb > 0) ProbeStatus.PASS else ProbeStatus.FAIL,
                "freeMb=$freeMb"
            ),
            ProbeResult("playback-hw", ProbeStatus.PENDING_DEVICE, "needs device"),
            ProbeResult("playback-sw", ProbeStatus.PENDING_DEVICE, "needs device"),
            ProbeResult("enhance-anime", ProbeStatus.PENDING_DEVICE, "needs device"),
            ProbeResult("enhance-standard", ProbeStatus.PENDING_DEVICE, "needs device"),
            ProbeResult("ncnn", ProbeStatus.PENDING_DEVICE, "needs device"),
            ProbeResult("subtitles-ar", ProbeStatus.PENDING_DEVICE, "needs device")
        )
    }

    fun reportJson(appVersion: String, results: List<ProbeResult>): String {
        val body = results.joinToString(",") { result ->
            "{\"name\":\"" + result.name + "\",\"status\":\"" + result.status.name +
                "\",\"detail\":\"" + result.detail + "\"}"
        }
        return "{\"app\":\"" + appVersion + "\",\"probes\":[" + body + "]}"
    }
}
