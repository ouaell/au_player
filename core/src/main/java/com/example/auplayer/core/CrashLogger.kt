package com.example.auplayer.core

import java.io.File

/**
 * Crash logger (MS-025). The uncaught handler persists the stacktrace and
 * records one Diag line; the report builder feeds Diagnostics + share.
 */
object CrashLogger {
    fun install(crashDir: File) {
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            onCrash(thread, error, crashDir)
        }
    }

    fun onCrash(thread: Thread, error: Throwable, crashDir: File): File {
        crashDir.mkdirs()
        val file = File(crashDir, "crash-" + System.currentTimeMillis().toString() + ".txt")
        file.writeText("thread=" + thread.name + "\n" + error.stackTraceToString())
        Diag.record("crash", file.name)
        return file
    }

    fun listCrashes(crashDir: File): List<File> {
        return crashDir.listFiles()?.sortedByDescending { it.name }.orEmpty()
    }
}

/** Shareable one-screen report (MS-025 done-when: share path exists). */
fun buildDiagnosticsReport(appVersion: String, entries: List<DiagEntry>): String {
    return buildString {
        appendLine("AU Player diagnostics v" + appVersion)
        appendLine("entries=" + entries.size.toString())
        entries.forEach { appendLine(it.tag + ": " + it.message) }
    }
}
