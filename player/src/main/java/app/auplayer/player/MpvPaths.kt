package app.auplayer.player

import app.auplayer.aucore.Diag
import java.io.File

/**
 * mpv startup options (§3.4). Pure data + file layout so the whole startup is
 * JVM-tested; the real `LibmpvBridge` only carries these strings to native.
 * Enhance≠Off overrides (lanczos/mitchell) are applied by MS-028, never here.
 */
data class MpvPaths(
    val configDir: File,
    val cacheDir: File,
    val filesDir: File,
)

fun startupOptions(paths: MpvPaths): List<Pair<String, String>> = listOf(
    "vo" to "gpu",
    "gpu-context" to "android",
    "opengl-es" to "yes",
    "hwdec" to "mediacodec-copy",
    "ao" to "audiotrack,opensles",
    "video-sync" to "audio",
    "gpu-shader-cache-dir" to File(paths.cacheDir, "mpv-shaders").path,
    "tls-verify" to "yes",
    "tls-ca-file" to File(paths.filesDir, "cacert.pem").path,
    "sub-fonts-dir" to File(paths.filesDir, "fonts").path,
    "osd-fonts-dir" to File(paths.filesDir, "fonts").path,
    "cache" to "yes",
    "demuxer-max-bytes" to "32MiB",
    "demuxer-max-back-bytes" to "16MiB",
    "save-position-on-quit" to "no",
    "scale" to "bilinear",
    "dscale" to "bilinear"
)

/** Creates shader-cache + fonts dirs. Returns the dirs (created or existing). */
fun ensureMpvDirs(paths: MpvPaths): List<File> {
    val dirs = listOf(
        File(paths.cacheDir, "mpv-shaders"),
        File(paths.filesDir, "fonts")
    )
    dirs.forEach { it.mkdirs() }
    return dirs
}

/** Applies the full startup: dirs, options in order, one Diag line (MS-008 gate). */
fun applyStartup(bridge: MpvBridge, paths: MpvPaths) {
    ensureMpvDirs(paths)
    startupOptions(paths).forEach { (key, value) -> bridge.setOption(key, value) }
    Diag.record("mpv", "startup options applied")
}
