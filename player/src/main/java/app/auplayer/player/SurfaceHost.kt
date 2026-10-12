package app.auplayer.player

import app.auplayer.aucore.Diag

/**
 * Surface lifecycle guard (§5.2, MS-009). Owns attach/detach ordering so the
 * UI layer (and later PiP/background/rotation paths) cannot leak a surface
 * or leave a black frame: create→attach, destroy→detach, recreate→re-attach.
 * Surface travels as `Any?`; the real `Surface` type binds in the view layer.
 */
class SurfaceHost(private val engine: PlayerEngine) {
    private var current: Any? = null

    val hasSurface: Boolean get() = current != null

    fun onSurfaceCreated(token: Any?) {
        if (token == null) {
            Diag.record("surface", "ignored null surface")
            return
        }
        current = token
        engine.attach(token)
        Diag.record("surface", "attached")
    }

    fun onSurfaceDestroyed() {
        if (current == null) {
            Diag.record("surface", "destroy without surface")
            return
        }
        current = null
        engine.detach()
        Diag.record("surface", "detached")
    }
}
