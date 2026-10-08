package com.example.auplayer.websource

/**
 * WebView hardening (MS-042, §8): allowlist/denylist URL policy plus the
 * exact WebSettings values. The screen itself is T2/instrumented; the policy
 * is pure and JVM-tested. Cleartext HTTP is unsupported in 1.0 (§17.7-16).
 */
object WebPolicy {
    fun shouldBlockUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.startsWith(HTTP_CLEAR) || !lower.startsWith(HTTPS)
    }

    fun externalAppScheme(url: String): Boolean {
        val lower = url.lowercase()
        return lower.startsWith("intent:") || lower.startsWith("market:")
    }
}

private const val HTTP_CLEAR = "http://"
private const val HTTPS = "https:"

/** WebSettings contract asserted by the hardening test. */
data class WebSettings(
    val javaScriptEnabled: Boolean = true,
    val allowFileAccess: Boolean = false,
    val allowContentAccess: Boolean = false,
    val blockNetworkLoads: Boolean = false,
    val mixedContentBlocked: Boolean = true,
    val safeBrowsingEnabled: Boolean = true,
    val popupBlocked: Boolean = true,
    val domStorageEnabled: Boolean = true
)

fun defaultWebSettings(): WebSettings = WebSettings()
