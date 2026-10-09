package com.example.auplayer.player

/**
 * Seam over the native mpv wrapper (MS-004 verdict: self-built AAR pending).
 * `LibmpvBridge` (real implementation) lands with the native build; until
 * then [FakeMpvBridge] backs the engine. Surface travels as `Any?` so engine
 * logic stays JVM-testable (real `Surface` attach lands in MS-009).
 */
interface MpvBridge {
    fun init()
    fun setOption(key: String, value: String)
    fun command(args: List<String>)
    fun setEventListener(listener: (MpvEvent) -> Unit)
    fun attach(token: Any?)
    fun detach()
    fun destroy()
    /** String property read (null when unavailable); used for video height. */
    fun property(name: String): String?
}

/** Native-side events of interest (§5.2 observes; full set lands in MS-011). */
sealed interface MpvEvent {
    data class TimePos(val seconds: Double) : MpvEvent
    data class Duration(val seconds: Double) : MpvEvent
    data class Paused(val paused: Boolean) : MpvEvent
    data object Eof : MpvEvent
    data class Error(val message: String) : MpvEvent
}

/** Fake for unit tests (§4: every interface has a fake). */
class FakeMpvBridge : MpvBridge {
    val options = mutableMapOf<String, String>()
    val commands = mutableListOf<List<String>>()
    var listener: ((MpvEvent) -> Unit)? = null
    var attached: Any? = null
    var attaches = 0
    var detaches = 0
    var initialized = false
    var destroyed = false

    override fun init() {
        initialized = true
    }

    override fun setOption(key: String, value: String) {
        options[key] = value
    }

    override fun command(args: List<String>) {
        commands += args
    }

    override fun setEventListener(listener: (MpvEvent) -> Unit) {
        this.listener = listener
    }

    override fun attach(token: Any?) {
        attached = token
        attaches++
    }

    override fun detach() {
        attached = null
        detaches++
    }

    override fun destroy() {
        destroyed = true
    }

    override fun property(name: String): String? = null

    fun emit(event: MpvEvent) {
        listener?.invoke(event)
    }
}
