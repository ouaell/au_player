package com.example.auplayer.websource

/**
 * Parses [JsCollector.SCRIPT] output (MS-043 device half): evaluateJavascript
 * JSON-encodes string results, so a one-layer unwrap precedes the parse.
 * Dependency-free (org.json is Android-only and unusable in JVM tests):
 * a small strict reader for the flat [{u,w,h,ad}] shape. Malformed entries
 * are skipped, never crash. JVM-tested.
 */
object CollectorParse {
    fun parse(raw: String?): List<ImageCandidate> {
        if (raw.isNullOrBlank() || raw == "null") return emptyList()
        val text = raw.trim()
        val arrayText = if (text.startsWith("\"")) {
            unwrap(text)
        } else {
            text
        }
        return splitObjects(arrayText).mapIndexedNotNull { index, obj ->
            readEntry(obj, index)
        }
    }

    private fun unwrap(quoted: String): String {
        return quoted.removeSurrounding("\"")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }

    private fun splitObjects(arrayText: String): List<String> {
        val trimmed = arrayText.trim()
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return emptyList()
        val out = ArrayList<String>()
        var depth = 0
        var inString = false
        var escaped = false
        var start = -1
        for (pos in trimmed.indices) {
            val char = trimmed[pos]
            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (char == '\\') {
                    escaped = true
                } else if (char == '"') {
                    inString = false
                }
            } else {
                when (char) {
                    '"' -> inString = true
                    '{' -> {
                        if (depth == 0) start = pos
                        depth++
                    }
                    '}' -> {
                        depth--
                        if (depth == 0 && start >= 0) {
                            out += trimmed.substring(start, pos + 1)
                            start = -1
                        }
                    }
                }
            }
        }
        return out
    }

    private fun readEntry(obj: String, order: Int): ImageCandidate? {
        val url = readStringField(obj, "u") ?: return null
        return ImageCandidate(
            url = url.ifEmpty { null },
            width = readIntField(obj, "w"),
            height = readIntField(obj, "h"),
            fromAd = readBoolField(obj, "ad"),
            order = order
        )
    }

    private fun readStringField(obj: String, name: String): String? {
        val pattern = Regex("\"$name\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
        val match = pattern.find(obj) ?: return null
        return match.groupValues[1]
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }

    private fun readIntField(obj: String, name: String): Int {
        val match = Regex("\"$name\"\\s*:\\s*(-?\\d+)").find(obj) ?: return 0
        return match.groupValues[1].toIntOrNull() ?: 0
    }

    private fun readBoolField(obj: String, name: String): Boolean {
        return Regex("\"$name\"\\s*:\\s*true").containsMatchIn(obj)
    }
}
