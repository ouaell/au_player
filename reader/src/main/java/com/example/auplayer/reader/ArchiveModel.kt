package com.example.auplayer.reader

/** Natural sort (MS-034): `1,2,10` not `1,10,2`. Pure and JVM-tested. */
object NaturalOrder : Comparator<String> {
    override fun compare(first: String, second: String): Int {
        val left = split(first)
        val right = split(second)
        val common = minOf(left.size, right.size)
        for (index in 0 until common) {
            val result = partCompare(left[index], right[index])
            if (result != 0) return result
        }
        return left.size.compareTo(right.size)
    }

    private fun partCompare(left: Part, right: Part): Int {
        if (left.digits && right.digits) {
            val leftValue = left.text.trimStart('0')
            val rightValue = right.text.trimStart('0')
            val byLength = leftValue.length.compareTo(rightValue.length)
            if (byLength != 0) return byLength
            val byValue = leftValue.compareTo(rightValue)
            if (byValue != 0) return byValue
        }
        return left.text.compareTo(right.text)
    }

    private fun split(value: String): List<Part> {
        val parts = mutableListOf<Part>()
        val current = StringBuilder()
        var digits = false
        var started = false
        value.forEach { char ->
            val isDigit = char.isDigit()
            if (!started) {
                started = true
                digits = isDigit
            }
            if (isDigit != digits) {
                parts += Part(current.toString(), digits)
                current.clear()
                digits = isDigit
            }
            current.append(char)
        }
        if (started) parts += Part(current.toString(), digits)
        return parts
    }

    private data class Part(val text: String, val digits: Boolean)
}

/** Archive safety caps (MS-034 zip-bomb defense). */
object ArchiveLimits {
    fun checkEntry(name: String, uncompressedBytes: Long, entryIndex: Int) {
        if (entryIndex >= MAX_ENTRIES) {
            throw ArchiveLimitException("too many entries")
        }
        if (uncompressedBytes > MAX_ENTRY_BYTES) {
            throw ArchiveLimitException("entry too large: $name")
        }
    }
}

class ArchiveLimitException(message: String) : IllegalStateException(message)

/** Image extensions accepted from archives/folders/URLs. */
val SUPPORTED_IMAGE_EXTS: Set<String> = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "avif")

fun isImageName(name: String): Boolean {
    val lower = name.lowercase()
    if (lower.contains("__macosx")) return false
    val base = lower.substringAfterLast('/')
    if (base.startsWith(".")) return false
    return base.substringAfterLast('.', "") in SUPPORTED_IMAGE_EXTS
}

private const val MAX_ENTRIES = 10_000
private const val MAX_ENTRY_BYTES = 200L * 1024L * 1024L
