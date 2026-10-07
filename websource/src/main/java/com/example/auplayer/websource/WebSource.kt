package com.example.auplayer.websource

/** Browser + extractor land in MS-042/MS-043/MS-044. */
interface WebSourceExtractor {
    fun extract(html: String, pageUrl: String): List<String>
}

class FakeWebSourceExtractor : WebSourceExtractor {
    override fun extract(html: String, pageUrl: String): List<String> = emptyList()
}
