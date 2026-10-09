package com.example.auplayer

import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebStorage
import android.webkit.WebView
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
import com.example.auplayer.websource.WebPolicy
import kotlinx.coroutines.launch

/**
 * In-app browser + extractor (§8, MS-042/43/44 made visible): hardened WebView,
 * "Read this page" (scroll-load → collect → filter → preview), "Open in
 * reader" (cookie/Referer download into a cache folder shown by ReaderScreen).
 * First-use notice + clear-data per §8. Shown only when
 * BuildConfig.WEB_SOURCE_ENABLED (store flavor omits it).
 */
@Composable
@Suppress("LongMethod")
// Single screen scaffold: state holders + one Column of small extracted
// pieces (NoticeDialog, WebActions, BrowserPane, WebPreview). Splitting the
// state wiring further would scatter it without adding clarity.
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
        BrowserPane(
            webRef = webRef,
            initialUrl = urlText,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
        WebPreview(
            candidates = candidates,
            checked = checked,
            modifier = Modifier.weight(1f).fillMaxWidth(),
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
private fun BrowserPane(
    webRef: MutableState<WebView?>,
    initialUrl: String,
    modifier: Modifier = Modifier
) {
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
        modifier = modifier
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
    modifier: Modifier = Modifier,
    onToggle: (String, Boolean) -> Unit
) {
    LazyColumn(modifier = modifier) {
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
