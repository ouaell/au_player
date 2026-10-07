package com.example.auplayer.enhance

/** Video enhance engine seam (§6). Real chains land in MS-027/MS-028. */
interface EnhanceEngine {
    fun apply(mode: String)
    fun clear()
}

class FakeEnhanceEngine : EnhanceEngine {
    val applied = mutableListOf<String>()
    var clears = 0
    override fun apply(mode: String) { applied += mode }
    override fun clear() { clears++ }
}
