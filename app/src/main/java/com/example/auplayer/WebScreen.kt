package com.example.auplayer

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.math.abs

/**
 * In-app browser + extractor (§8, MS-042/43/44 made visible): hardened WebView,
 * "Read this page" (scroll-load → collect → filter → preview), "Open in
 * reader" (cookie/Referer download into a cache folder shown by ReaderScreen).
 * First-use notice + clear-data per §8. Shown only when
 * BuildConfig.WEB_SOURCE_ENABLED (store flavor omits it).
 */
@Composable
fun WebScreen(onOpenReader: (dirPath: String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var urlText by remember { mutableStateOf("https://") }
    var status by remember { mutableStateOf("") }
    var candidates by remember { mutableStateOf(listOf<String>()) }
    var checked by remember { mutableStateOf(setOf<String>()) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf(!webNoticeSeen(context)) }
    val webRef = remember { mutableStateOf<WebView?>(null) }

    if (notice) {
        NoticeDialog(onAck = {
            setWebNoticeSeen(context)
            notice = false
        })
    }
    Column(modifier = modifier.fillMaxSize().padding(8.dp)) {
        OutlinedTextField(
            value = urlText,
            onValueChange = { urlText = it },
            label = { Text("Page URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        WebActions(
            busy = busy,
            canOpen = checked.isNotEmpty(),
            handlers = WebHandlers(
                onGo = {
                    val target = urlText.trim()
                    if (WebPolicy.shouldBlockUrl(target)) {
                        status = "blocked: only https pages"
                    } else {
                        webRef.value?.loadUrl(target)
                    }
                },
                onRead = {
                    scope.launch {
                        busy = true
                        status = readPage(webRef.value, urlText.trim()) { picked, found ->
                            candidates = picked
                            checked = picked.toSet()
                            status = "${picked.size} images (${found} candidates)"
                        }
                        busy = false
                    }
                },
                onOpen = {
                    scope.launch {
                        busy = true
                        status = "downloading…"
                        val dir = downloadKept(context, webRef.value, checked.toList())
                        busy = false
                        if (dir != null) {
                            onOpenReader(dir.absolutePath)
                        } else {
                            status = "nothing downloaded"
                        }
                    }
                },
                onClear = {
                    CookieManager.getInstance().removeAllCookies(null)
                    WebStorage.getInstance().deleteAllData()
                    webRef.value?.clearCache(true)
                    status = "browsing data cleared"
                }
            )
        )
        Text(text = status, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
        BrowserPane(webRef = webRef, initialUrl = urlText)
        WebPreview(
            candidates = candidates,
            checked = checked,
            onToggle = { url, on ->
                checked = if (on) checked + url else checked - url
            }
        )
    }
}

@Composable
private fun NoticeDialog(onAck: () -> Unit) {
    AlertDialog(
        onDismissRequest = { },
        confirmButton = {
            TextButton(onClick = onAck) { Text("Understood") }
        },
        title = { Text("Web Source") },
        text = {
            Text(
                "The app contains no content. You are responsible for the " +
                    "pages you open and the rights to read their images."
            )
        }
    )
}

@Composable
private fun BrowserPane(webRef: MutableState<WebView?>, initialUrl: String) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                applyHardenedSettings(settings)
                webChromeClient = WebChromeClient()
                webViewClient = policyClient(ctx)
                loadUrl(initialUrl.ifBlank { "about:blank" })
                webRef.value = this
            }
        },
        modifier = Modifier.weight(1f).fillMaxWidth()
    )
}
@Composable
private fun WebActions(busy: Boolean, canOpen: Boolean, handlers: WebHandlers) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Button(onClick = handlers.onGo, enabled = !busy) { Text("Go") }
        Button(onClick = handlers.onRead, enabled = !busy) { Text("Read this page") }
        Button(onClick = handlers.onOpen, enabled = !busy && canOpen) { Text("Open in reader") }
        Button(onClick = handlers.onClear, enabled = !busy) { Text("Clear data") }
    }
}

private data class WebHandlers(
    val onGo: () -> Unit,
    val onRead: () -> Unit,
    val onOpen: () -> Unit,
    val onClear: () -> Unit
)

@Composable
private fun WebPreview(
    candidates: List<String>,
    checked: Set<String>,
    onToggle: (String, Boolean) -> Unit
) {
    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
        items(candidates, key = { it }) { url ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Checkbox(
                    checked = url in checked,
                    onCheckedChange = { on -> onToggle(url, on) }
                )
                Text(text = url, maxLines = 2, modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun applyHardenedSettings(settings: WebSettings) {
    val wanted = defaultWebSettings()
    settings.javaScriptEnabled = wanted.javaScriptEnabled
    settings.allowFileAccess = wanted.allowFileAccess
    settings.allowContentAccess = wanted.allowContentAccess
    settings.blockNetworkLoads(wanted.blockNetworkLoads)
    settings.mixedContentMode = if (wanted.mixedContentBlocked) {
        WebSettings.MIXED_CONTENT_NEVER_ALLOW
    } else {
        WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    }
    settings.safeBrowsingEnabled = wanted.safeBrowsingEnabled
    settings.setSupportMultipleWindows(!wanted.popupBlocked)
    settings.domStorageEnabled = wanted.domStorageEnabled
}

private fun policyClient(appContext: Context): WebViewClient {
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

private suspend fun WebView.evalJs(script: String): String? =
    suspendCancellableCoroutine { cont ->
        evaluateJavascript(script) { cont.resume(it) }
    }

/** Scroll-loads lazy images (§8: step until height settles, 15 s cap), then collects. */
private suspend fun readPage(
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

private suspend fun downloadKept(
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

private fun webNoticeSeen(context: Context): Boolean {
    return context.getSharedPreferences(WEB_PREFS, Context.MODE_PRIVATE)
        .getBoolean(NOTICE_KEY, false)
}

private fun setWebNoticeSeen(context: Context) {
    context.getSharedPreferences(WEB_PREFS, Context.MODE_PRIVATE)
        .edit().putBoolean(NOTICE_KEY, true).apply()
}
