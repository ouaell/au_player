package com.example.auplayer

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.auplayer.reader.FetchHeaders
import com.example.auplayer.websource.CollectorParse
import com.example.auplayer.websource.ImagePicker
import com.example.auplayer.websource.JsCollector
import com.example.auplayer.websource.PageDownloader
import com.example.auplayer.websource.PageResult
import com.example.auplayer.websource.WebPolicy
import com.example.auplayer.websource.defaultWebSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.math.abs

/**
 * WebView flow helpers for WebScreen (split per the function-count gate).
 * Pure flow: harden settings, policy client, scroll-load + collect,
 * download-into-folder. UI stays in WebScreen.kt.
 */
internal fun applyHardenedSettings(settings: WebSettings) {
    val wanted = defaultWebSettings()
    settings.javaScriptEnabled = wanted.javaScriptEnabled
    settings.allowFileAccess = wanted.allowFileAccess
    settings.allowContentAccess = wanted.allowContentAccess
    settings.blockNetworkLoads = wanted.blockNetworkLoads
    settings.mixedContentMode = if (wanted.mixedContentBlocked) {
        WebSettings.MIXED_CONTENT_NEVER_ALLOW
    } else {
        WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    }
    settings.safeBrowsingEnabled = wanted.safeBrowsingEnabled
    settings.setSupportMultipleWindows(!wanted.popupBlocked)
    settings.domStorageEnabled = wanted.domStorageEnabled
}

internal fun policyClient(appContext: Context): WebViewClient {
    return object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val target = request.url.toString()
            val verdict = when {
                WebPolicy.shouldBlockUrl(target) -> "Blocked (https only)"
                WebPolicy.externalAppScheme(target) -> "External apps stay closed"
                else -> null
            }
            if (verdict == null) return false
            Toast.makeText(appContext, verdict, Toast.LENGTH_SHORT).show()
            return true
        }
    }
}

internal suspend fun WebView.evalJs(script: String): String? =
    suspendCancellableCoroutine { cont ->
        evaluateJavascript(script) { cont.resume(it) }
    }

/** Scroll-loads lazy images (§8: step until height settles, 15 s cap), then collects. */
internal suspend fun readPage(
    view: WebView?,
    fallbackUrl: String,
    onPicked: (List<String>, Int) -> Unit
): String {
    if (view == null) return "no page"
    val current = view.url ?: fallbackUrl
    var lastHeight = -1L
    val deadline = System.currentTimeMillis() + SCROLL_BUDGET_MS
    while (System.currentTimeMillis() < deadline) {
        view.evalJs(JsCollector.SCROLL_STEP)
        delay(SCROLL_STEP_MS)
        val height = view.evalJs("document.body.scrollHeight")?.trim()?.toLongOrNull() ?: -1L
        if (height == lastHeight) break
        lastHeight = height
    }
    val raw = view.evalJs(JsCollector.SCRIPT)
    val found = CollectorParse.parse(raw)
    val picked = ImagePicker.pick(found, current)
    onPicked(picked, found.size)
    Log.i("E6GATE", "websource picked=${picked.size} from=${found.size}")
    return "${picked.size} images (${found.size} candidates)"
}

internal suspend fun downloadKept(
    context: Context,
    view: WebView?,
    urls: List<String>
): File? = withContext(Dispatchers.IO) {
    val pageUrl = view?.url ?: return@withContext null
    if (urls.isEmpty()) return@withContext null
    val dir = File(File(context.cacheDir, "websource"), abs(pageUrl.hashCode()).toString())
    dir.mkdirs()
    val cookies = CookieManager.getInstance().getCookie(pageUrl)
    val downloader = PageDownloader(
        headers = FetchHeaders(
            referer = pageUrl,
            userAgent = view.settings.userAgentString,
            cookies = cookies
        ),
        cacheDir = dir
    )
    var saved = 0
    urls.forEachIndexed { index, url ->
        when (val result = downloader.download(url)) {
            is PageResult.Downloaded -> {
                // FolderProvider lists image extensions only; keep names clean.
                val ext = url.substringAfterLast('/', "").substringAfterLast('.', "")
                    .substringBefore('?').lowercase()
                    .takeIf { it in IMAGE_EXTS } ?: FALLBACK_EXT
                val named = File(dir, "page-%03d.%s".format(index, ext))
                result.file.copyTo(named, overwrite = true)
                result.file.delete()
                saved++
            }
            is PageResult.Failed -> {
                Log.w("WebScreen", "download failed $url: ${result.error}")
            }
        }
    }
    if (saved > 0) dir else null
}

private const val SCROLL_BUDGET_MS = 15_000L
private const val SCROLL_STEP_MS = 400L
private const val FALLBACK_EXT = "jpg"
private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "avif")
private const val WEB_PREFS = "au_web"
private const val NOTICE_KEY = "notice_seen"

internal fun webNoticeSeen(context: Context): Boolean {
    return context.getSharedPreferences(WEB_PREFS, Context.MODE_PRIVATE)
        .getBoolean(NOTICE_KEY, false)
}

internal fun setWebNoticeSeen(context: Context) {
    context.getSharedPreferences(WEB_PREFS, Context.MODE_PRIVATE)
        .edit().putBoolean(NOTICE_KEY, true).apply()
}
