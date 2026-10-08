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
        val numeric = if (left.digits && right.digits) {
            numericCompare(left.text, right.text)
        } else {
            null
        }
        return numeric ?: left.text.compareTo(right.text)
    }

    private fun numericCompare(left: String, right: String): Int? {
        val leftValue = left.trimStart('0')
        val rightValue = right.trimStart('0')
        if (leftValue.length != rightValue.length) {
            return leftValue.length.compareTo(rightValue.length)
        }
        val byValue = leftValue.compareTo(rightValue)
        return if (byValue == 0) null else byValue
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
    if (lower.contains(MACOSX_MARKER)) return false
    val base = lower.substringAfterLast('/')
    return !base.startsWith(".") && base.substringAfterLast('.', "") in SUPPORTED_IMAGE_EXTS
}

private const val MACOSX_MARKER = "__macosx"

private const val MAX_ENTRIES = 10_000
private const val MAX_ENTRY_BYTES = 200L * 1024L * 1024L
