package app.auplayer.enhance

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Stages the bundled shader pack into app-private storage.
 *
 * mpv reads `glsl-shaders` from the filesystem, not from the APK, so the
 * assets have to exist as real files before mpv can compile them. Copies are
 * content-hashed: a changed pack is re-copied, an unchanged one is left alone
 * so a relaunch does no disk work.
 */
class ShaderStager(private val context: Context) {

    private val root: File get() = File(context.filesDir, ASSET_ROOT)

    /** Copies anything missing or stale. Returns true when the pack is ready. */
    fun stage(): Boolean {
        val names = listOf(
            "Anime4K_Clamp_Highlights.glsl",
            "Anime4K_Restore_CNN_M.glsl",
            "Anime4K_Upscale_CNN_x2_M.glsl",
            "Anime4K_Upscale_CNN_x2_S.glsl",
            "ravu-lite-r4.hook",
        )
        if (!root.exists() && !root.mkdirs()) {
            Log.w(TAG, "cannot create ${root.path}")
            return false
        }
        var ok = true
        for (name in names) {
            if (!copyIfStale(name)) ok = false
        }
        if (ok) Log.i(TAG, "shader pack ready at ${root.path}")
        return ok
    }

    /**
     * Maps a pack-relative asset path to its staged file.
     *
     * Asset paths are rooted at `filesDir` ("shaders/patched/Foo.glsl"), which
     * is exactly where [stage] puts them. Joining them onto the pack root
     * instead would repeat the "shaders/patched/" segment. Pure so it can be
     * tested without a Context.
     */
    fun resolve(assetPath: String): String = resolveIn(context.filesDir, assetPath)

    private fun copyIfStale(name: String): Boolean {
        val target = File(root, name)
        val stamp = File(root, STAMP_FILE)
        return try {
            val bytes = context.assets.open("$ASSET_ROOT/$name").use { it.readBytes() }
            // One stamp for the whole pack: the pack is versioned as a unit, so
            // a single content hash keeps staging atomic and cheap to reason about.
            val current = stampFor(name, bytes)
            if (target.exists() && stamp.exists() && stamp.readText() == current) {
                return true
            }
            target.outputStream().use { it.write(bytes) }
            stamp.writeText(current)
            true
        } catch (error: Exception) {
            Log.e(TAG, "failed to stage $name", error)
            false
        }
    }

    private fun stampFor(name: String, bytes: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) } + "  " + name + "\n"
    }

    companion object {
        private const val TAG = "AuPlayer"
        const val ASSET_ROOT = "shaders/patched"
        private const val STAMP_FILE = ".pack-stamp"

        /** Pack-relative asset path -> absolute staged path under [filesDir]. */
        fun resolveIn(filesDir: File, assetPath: String): String = File(filesDir, assetPath).path
    }
}