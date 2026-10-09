package com.example.auplayer

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Bundled OFL fonts (MS-067, §3.4): APK assets → filesDir/fonts (mpv's
 * sub-fonts-dir/osd-fonts-dir point there). Copies missing files only,
 * best-effort at startup; failures degrade to system fonts (Diag-worthy,
 * never fatal). CJK is intentionally not bundled (size) — Android system
 * fallback covers it; see docs/assets-licenses.md.
 */
fun installBundledFonts(context: Context) {
    val target = File(context.filesDir, FONTS_DIR)
    try {
        target.mkdirs()
        val assets = context.assets.list(FONTS_ASSET) ?: return
        for (name in assets) {
            val out = File(target, name)
            if (out.isFile) continue
            context.assets.open("$FONTS_ASSET/$name").use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            }
        }
    } catch (error: java.io.IOException) {
        Log.w("AuPlayer", "bundled fonts unavailable: " + error.message)
    }
}

private const val FONTS_ASSET = "fonts"
private const val FONTS_DIR = "fonts"
