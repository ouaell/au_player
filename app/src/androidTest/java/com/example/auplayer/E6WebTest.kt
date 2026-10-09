package com.example.auplayer

import android.content.Context
import android.content.Intent
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.auplayer.websource.CollectorParse
import com.example.auplayer.websource.ImagePicker
import com.example.auplayer.websource.JsCollector
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * E6 — WebView JS bridge on-device (§8 collector): load a fixture page from
 * a data: URL (no network), run [JsCollector.SCRIPT] through the real
 * evaluateJavascript encoding, and prove candidates + picker filtering work
 * end to end. Evidence goes to logcat under E6GATE.
 */
@RunWith(AndroidJUnit4::class)
class E6WebTest {
    @Test
    fun collectorFindsFixtureImages() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(Intent.ACTION_MAIN)
            .setClassName(context, MainActivity::class.java.name)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            val webRef = AtomicReference<WebView>()
            val loaded = CountDownLatch(1)
            scenario.onActivity { activity ->
                val view = WebView(activity)
                view.settings.javaScriptEnabled = true
                view.settings.blockNetworkLoads(true)
                view.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        loaded.countDown()
                    }
                }
                webRef.set(view)
                view.loadDataWithBaseURL(
                    "https://example.com/page",
                    FIXTURE_HTML,
                    "text/html",
                    "utf-8",
                    null
                )
            }
            assertTrue("fixture page never finished", loaded.await(PAGE_TIMEOUT_MS, TimeUnit.MILLISECONDS))
            runBlocking {
                val web = webRef.get()
                val raw = evalJs(web, JsCollector.SCRIPT)
                val found = CollectorParse.parse(raw)
                Log.i(GATE_TAG, "collector candidates=${found.size}")
                assertTrue(
                    "collector missed fixture images: $found",
                    found.mapNotNull { it.url }.containsAll(
                        listOf(
                            "https://cdn.example/p1.jpg",
                            "https://cdn.example/p2.jpg",
                            "https://cdn.example/bg.jpg"
                        )
                    )
                )
                val picked = ImagePicker.pick(found, "https://example.com/page")
                Log.i(GATE_TAG, "picker kept=${picked.size}")
                // Nothing loads (network blocked): natural sizes are 0 and the
                // size filter drops them; only the layout-sized background
                // survives. That exact outcome proves filtering on-device.
                assertTrue("background missing: $picked", picked.contains("https://cdn.example/bg.jpg"))
                assertTrue("ad pixel leaked in: $picked", picked.none { it.contains("ads.example") })
            }
        }
    }

    private suspend fun evalJs(view: WebView, script: String): String? {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val holder = AtomicReference<String>()
        val done = CountDownLatch(1)
        instrumentation.runOnMainSync {
            view.evaluateJavascript(script) { value ->
                holder.set(value)
                done.countDown()
            }
        }
        var waited = 0L
        while (!done.await(POLL_MS, TimeUnit.MILLISECONDS)) {
            waited += POLL_MS
            if (waited > JS_TIMEOUT_MS) break
        }
        return holder.get()
    }
}

private const val PAGE_TIMEOUT_MS = 15_000L
private const val JS_TIMEOUT_MS = 10_000L
private const val POLL_MS = 250L
private const val GATE_TAG = "E6GATE"

private const val FIXTURE_HTML = "<html><body>" +
    "<img src=\"https://cdn.example/p1.jpg\" width=\"800\" height=\"600\">" +
    "<img data-src=\"https://cdn.example/p2.jpg\" srcset=\"https://cdn.example/p2.jpg 1x\">" +
    "<div style=\"background-image:url('https://cdn.example/bg.jpg');width:500px;height:300px\"></div>" +
    "<div class=\"ad-box\"><img src=\"https://ads.example/t.gif\" width=\"1\" height=\"1\"></div>" +
    "</body></html>"
