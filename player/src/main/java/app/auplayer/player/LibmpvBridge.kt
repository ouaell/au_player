package app.auplayer.player

import android.util.Log
import android.view.Surface
import app.auplayer.aucore.Diag
import app.auplayer.aucore.MpvCommandException
import java.io.File
import java.io.IOException

/**
 * Real bridge over the self-built libmpv (Push B2). Options flow from
 * [MpvStartup]; time-pos is observed as STRING and parsed (no ABI numbers).
 * Events arrive by name (`end-file`, `property-change`). Instantiation on a
 * JVM without native libs throws — tests guard with `assumeTrue(load())`.
 */
class LibmpvBridge(
    private val paths: MpvPaths,
    traceDir: File? = null,
    /** Application Context; required for `content://` sources. */
    private val appContext: Any? = null,
) : MpvBridge {
    private var handle: Long = 0L
    private var listener: ((MpvEvent) -> Unit)? = null
    private val tracer = EventTracer(traceDir)

    /** Serializes direct (non-engine) attach/detach callers against each
     *  other; engine callers are already single-dispatcher ordered. */
    private val surfaceLock = Any()

    @Volatile
    var surface: Surface? = null
        private set

    override fun init() {
        if (handle != 0L) return
        if (!MpvNative.load()) {
            Log.i("AuPlayer", "native libmpv absent")
            throw MpvCommandException("native libmpv absent")
        }
        // Must precede loadfile: FFmpeg resolves content:// through this ctx.
        MpvNative.setAppContext(appContext)
        val created = MpvNative.nativeCreate()
        if (created == 0L) throw MpvCommandException("mpv_create failed")
        handle = created
        try {
            applyStartup(this, paths)
            val code = MpvNative.nativeInit(handle)
            if (code != 0) {
                throw MpvCommandException("mpv_initialize: " + MpvNative.nativeErrorString(code))
            }
        } catch (error: MpvCommandException) {
            Log.e("AuPlayer", "mpv init failed", error)
            throw error
        }
        MpvNative.nativeObserve(handle, TIME_POS_ID, "time-pos")
        MpvNative.nativeObserve(handle, DURATION_ID, "duration")
        MpvNative.nativeObserve(handle, PAUSE_ID, "pause")
        MpvEvents.nativeStartEvents(handle, eventCallback)
        tracer.clear()
        val version = MpvNative.nativeGetProperty(handle, "mpv-version")
        Log.i("AuPlayer", "libmpv initialized: $version")
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
        synchronized(surfaceLock) {
            surface = token as? Surface
            // vo/gpu android-context takes the Surface from the `wid` option
            // (WinID = jobject address) at vo init. mpv reads wid on ITS vo
            // thread, so wid must never point at a deleted ref: park it on 0
            // before swapping the global ref (S1-001: SIGABRT in
            // vo_android_init on a dangling address). Failures only degrade
            // to audio-only, recorded for diagnosis.
            parkWid()
            if (surface == null || handle == 0L) {
                if (handle != 0L) MpvSurface.nativeDetachSurface(handle)
                Diag.record("surface", "released")
                return
            }
            val ref = MpvSurface.nativeAttachSurface(handle, surface)
            if (ref != 0L) {
                try {
                    setOption(WID_OPTION, ref.toString())
                } catch (error: MpvCommandException) {
                    Log.e("AuPlayer", "wid rejected: " + error.message)
                    Diag.record("surface", "wid rejected: ${error.message}")
                }
            }
            Diag.record("surface", "stored")
        }
    }

    override fun detach() {
        synchronized(surfaceLock) {
            surface = null
            if (handle != 0L) {
                // Park wid BEFORE deleting the ref: a concurrent vo init
                // must see 0 (graceful no-video), never a dangling address.
                parkWid()
                MpvSurface.nativeDetachSurface(handle)
            }
        }
    }

    /** Best-effort wid reset; detach/attach paths must never throw. */
    private fun parkWid() {
        if (handle == 0L) return
        try {
            setOption(WID_OPTION, "0")
        } catch (error: MpvCommandException) {
            Log.w("AuPlayer", "wid park rejected: " + error.message)
            Diag.record("surface", "wid park rejected: ${error.message}")
        }
    }

    override fun destroy() {
        MpvEvents.nativeStopEvents(handle)
        MpvNative.nativeDestroy(handle)
        handle = 0L
        surface = null
    }

    override fun property(name: String): String? {
        // JNI returns null for missing/unavailable properties; no throw path.
        if (handle == 0L) return null
        return MpvNative.nativeGetProperty(handle, name)
    }

    // NOTE: MpvRender JNI bindings stay compiled (offscreen/thumbnail use);
    // the retired app-owned EGL loop called them, the vo owns the surface now.

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
                name == "property-change" && replyUserdata == DURATION_ID -> {
                    val seconds = MpvNative.nativeGetProperty(handle, "duration")?.toDoubleOrNull()
                    if (seconds != null) {
                        listener?.invoke(MpvEvent.Duration(seconds))
                    }
                }
                name == "property-change" && replyUserdata == PAUSE_ID -> {
                    val paused = MpvNative.nativeGetProperty(handle, "pause")
                    if (paused != null) {
                        listener?.invoke(MpvEvent.Paused(paused == "yes"))
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
private const val DURATION_ID = 2L
private const val PAUSE_ID = 3L
private const val TRACE_NAME = "e1-trace.txt"
private const val WID_OPTION = "wid"
