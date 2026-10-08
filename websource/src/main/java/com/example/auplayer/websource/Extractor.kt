package com.example.auplayer.websource

/** Lazy-image candidate reported by the page (MS-043). */
data class ImageCandidate(
    val url: String?,
    val width: Int,
    val height: Int,
    val fromAd: Boolean,
    val order: Int
)

/**
 * Extraction pipeline (MS-043): absolute resolution in DOM order, ≥300px
 * filter, data-URI/ad/dup rejection. The JS collector ([JsCollector]) feeds
 * these candidates; fixtures mirror lazy/srcset/background/relative cases.
 */
object ImagePicker {
    fun pick(
        candidates: List<ImageCandidate>,
        pageUrl: String,
        minSizePx: Int = MIN_IMAGE_PX
    ): List<String> {
        val seen = LinkedHashSet<String>()
        candidates.sortedBy { it.order }.forEach { candidate ->
            val raw = candidate.url ?: return@forEach
            if (raw.startsWith(DATA_PREFIX)) return@forEach
            if (candidate.fromAd) return@forEach
            if (candidate.width < minSizePx || candidate.height < minSizePx) return@forEach
            val absolute = resolveUrl(pageUrl, raw) ?: return@forEach
            seen += absolute
        }
        return seen.toList()
    }

    fun resolveUrl(pageUrl: String, raw: String): String? {
        return when {
            raw.startsWith(HTTPS) || raw.startsWith(HTTP) -> raw
            raw.startsWith(PROTOCOL_RELATIVE) -> HTTPS + raw
            raw.startsWith(DATA_PREFIX) -> null
            raw.startsWith(ROOT_PATH) -> resolveRoot(pageUrl, raw)
            else -> resolveRelative(pageUrl, raw)
        }
    }

    private fun resolveRoot(pageUrl: String, raw: String): String? {
        val schemeEnd = pageUrl.indexOf(SCHEME_SEP)
        if (schemeEnd < 0) return null
        val slash = pageUrl.indexOf('/', schemeEnd + SCHEME_SEP.length)
        val root = if (slash < 0) pageUrl else pageUrl.substring(0, slash)
        return root + raw
    }

    private fun resolveRelative(pageUrl: String, raw: String): String? {
        val dir = pageUrl.substringBeforeLast('/', "")
        return if (dir.isEmpty()) null else dir + "/" + raw
    }
}

/**
 * Page-side collector contract (runs via evaluateJavascript after
 * step-scrolling to trigger lazy loads, §8). Pinned here so the shape is
 * reviewed; instrumented test (T2) runs it against real fixtures.
 */
object JsCollector {
    /** One lazy-load trigger step; Kotlin repeats it until height settles (§8). */
    const val SCROLL_STEP: String = "window.scrollBy(0, window.innerHeight);"

    val SCRIPT: String = """
        (function() {
          const urls = [];
          const push = (u, w, h, ad) => { if (u) urls.push({u, w: w||0, h: h||0, ad: !!ad}); };
          document.querySelectorAll('img').forEach(img => {
            const ad = img.closest('[class*=ad],[id*=ad],iframe') !== null;
            const src = img.currentSrc || img.src || img.getAttribute('data-src') ||
              img.getAttribute('data-original') || img.getAttribute('data-lazy-src');
            push(src, img.naturalWidth, img.naturalHeight, ad);
            const set = img.getAttribute('srcset');
            if (set) set.split(',').forEach(part => push(part.trim().split(' ')[0], 0, 0, ad));
          });
          document.querySelectorAll('picture > source').forEach(source => {
            const set = source.getAttribute('srcset');
            if (set) set.split(',').forEach(part => push(part.trim().split(' ')[0], 0, 0, false));
          });
          document.querySelectorAll('*').forEach(el => {
            const bg = getComputedStyle(el).backgroundImage;
            const match = /url\("([^"]+)"\)/.exec(bg || '');
            if (match) push(match[1], el.clientWidth, el.clientHeight, false);
          });
          return JSON.stringify(urls);
        })();
    """.trimIndent()
}

private const val MIN_IMAGE_PX = 300
private const val DATA_PREFIX = "data:"
private const val HTTPS = "https:"
private const val HTTP = "http:"
private const val PROTOCOL_RELATIVE = "//"
private const val SCHEME_SEP = "://"
private const val ROOT_PATH = "/"
