package com.example.auplayer.player

import android.util.Log
import android.view.Surface
import com.example.auplayer.core.Diag
import com.example.auplayer.core.MpvCommandException
import java.io.File
import java.io.IOException

/**
 * Real bridge over the self-built libmpv (Push B2). Options flow from
 * [MpvStartup]; time-pos is observed as STRING and parsed (no ABI numbers).
 * Events arrive by name (`end-file`, `property-change`). Instantiation on a
 * JVM without native libs throws — tests guard with `assumeTrue(load())`.
 */
class LibmpvBridge(private val paths: MpvPaths, traceDir: File? = null) : MpvBridge {
    private var handle: Long = 0L
    private var listener: ((MpvEvent) -> Unit)? = null
    private val tracer = EventTracer(traceDir)

    @Volatile
    var surface: Surface? = null
        private set

    override fun init() {
        if (handle != 0L) return
        if (!MpvNative.load()) {
            Log.i("AuPlayer", "native libmpv absent")
            throw MpvCommandException("native libmpv absent")
        }
        val created = MpvNative.nativeCreate()
        if (created == 0L) throw MpvCommandException("mpv_create failed")
        handle = created
        applyStartup(this, paths)
        val code = MpvNative.nativeInit(handle)
        if (code != 0) {
            throw MpvCommandException("mpv_initialize: " + MpvNative.nativeErrorString(code))
        }
        MpvNative.nativeObserve(handle, TIME_POS_ID, "time-pos")
        MpvEvents.nativeStartEvents(handle, eventCallback)
        tracer.clear()
        Log.i("AuPlayer", "libmpv initialized")
    }

    override fun setOption(key: String, value: String) {
        val code = MpvNative.nativeSetOption(handle, key, value)
        if (code != 0) {
            throw MpvCommandException("set $key: " + MpvNative.nativeErrorString(code))
        }
    }

    override fun command(args: List<String>) {
        val code = MpvNative.nativeCommand(handle, args.toTypedArray())
        if (code != 0) {
            throw MpvCommandException("command failed: " + MpvNative.nativeErrorString(code))
        }
    }

    override fun setEventListener(listener: (MpvEvent) -> Unit) {
        this.listener = listener
    }

    override fun attach(token: Any?) {
        surface = token as? Surface
        Diag.record("surface", if (surface == null) "released" else "stored")
    }

    override fun detach() {
        surface = null
    }

    override fun destroy() {
        MpvEvents.nativeStopEvents(handle)
        MpvNative.nativeDestroy(handle)
        handle = 0L
        surface = null
    }

    fun renderInit(): Int = MpvRender.nativeRenderInit(handle)

    fun checkUpdate(): Boolean = MpvRender.nativeCheckUpdate(handle)

    fun renderFrame(width: Int, height: Int): Int = MpvRender.nativeRender(handle, width, height)

    private val eventCallback = object : MpvEvents.MpvEventCallback {
        override fun onEvent(name: String, replyUserdata: Long) {
            when {
                name == "end-file" -> {
                    tracer.line("eof")
                    listener?.invoke(MpvEvent.Eof)
                }
                name == "property-change" && replyUserdata == TIME_POS_ID -> {
                    val seconds = MpvNative.nativeGetProperty(handle, "time-pos")?.toDoubleOrNull()
                    if (seconds != null) {
                        tracer.line("time=$seconds")
                        listener?.invoke(MpvEvent.TimePos(seconds))
                    }
                }
                else -> Unit
            }
        }
    }
}

/** E-test trace writer (E1): time-pos + eof lines for workflow assertions. */
internal class EventTracer(private val dir: File?) {
    fun line(text: String) {
        val target = dir ?: return
        try {
            File(target, TRACE_NAME).appendText(text + "\n")
        } catch (error: IOException) {
            Diag.record("trace", "write failed: " + error.message.toString())
        }
    }

    fun clear() {
        val target = dir ?: return
        File(target, TRACE_NAME).delete()
    }
}

private const val TIME_POS_ID = 1L
private const val TRACE_NAME = "e1-trace.txt"
