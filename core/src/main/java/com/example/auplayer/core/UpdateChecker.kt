package com.example.auplayer.core

/**
 * Direct-flavor update check (MS-075): reads the latest GitHub release and
 * opens the download page (no silent install). `play` flavor uses platform
 * in-app updates instead.
 */
object UpdateChecker {
    fun parseLatestTag(releaseJson: String): String? {
        val match = tagPattern.find(releaseJson)
        return match?.groupValues?.getOrNull(1)
    }

    /** Numeric dotted compare; leading `v` stripped; longer wins on ties. */
    fun isNewer(latest: String, current: String): Boolean {
        val latestParts = numbers(latest)
        val currentParts = numbers(current)
        val length = maxOf(latestParts.size, currentParts.size)
        for (index in 0 until length) {
            val left = latestParts.getOrElse(index) { 0 }
            val right = currentParts.getOrElse(index) { 0 }
            if (left != right) return left > right
        }
        return false
    }

    private fun numbers(version: String): List<Int> {
        return version.trimStart('v').split('.').map { it.toIntOrNull() ?: 0 }
    }

    private val tagPattern = Regex("\"tag_name\"\\s*:\\s*\"([^\"]+)\"")
}
