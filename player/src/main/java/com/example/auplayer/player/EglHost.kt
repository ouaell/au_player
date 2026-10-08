package com.example.auplayer.player

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.view.Surface

/**
 * App-owned EGL state (Push B2). mpv draws into the *current* framebuffer via
 * the render API; Android owns the display/context/window-surface lifecycle.
 * GLES3 requested (vo=gpu needs it); creation failure fails closed (E1/E3).
 */
class EglHost {
    private var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var context: EGLContext = EGL14.EGL_NO_CONTEXT
    private var surface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var config: EGLConfig? = null

    fun init(): Boolean {
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (display === EGL14.EGL_NO_DISPLAY) return false
        val version = IntArray(VERSION_LEN)
        if (!EGL14.eglInitialize(display, version, 0, version, 1)) {
            release()
            return false
        }
        config = chooseConfig() ?: run {
            release()
            return false
        }
        val attrs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, GLES_VERSION, EGL14.EGL_NONE)
        context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, attrs, 0)
        if (context === EGL14.EGL_NO_CONTEXT) {
            release()
            return false
        }
        return true
    }

    fun makeWindowSurface(surface: Surface): Boolean {
        val cfg = config ?: return false
        destroySurface()
        val eglSurface = EGL14.eglCreateWindowSurface(display, cfg, surface, intArrayOf(EGL14.EGL_NONE), 0)
        if (eglSurface === EGL14.EGL_NO_SURFACE) return false
        this.surface = eglSurface
        return makeCurrent()
    }

    fun makeCurrent(): Boolean {
        if (surface === EGL14.EGL_NO_SURFACE) return false
        return EGL14.eglMakeCurrent(display, surface, surface, context)
    }

    fun swapBuffers(): Boolean {
        if (surface === EGL14.EGL_NO_SURFACE) return false
        return EGL14.eglSwapBuffers(display, surface)
    }

    fun release() {
        destroySurface()
        if (context !== EGL14.EGL_NO_CONTEXT) {
            EGL14.eglDestroyContext(display, context)
            context = EGL14.EGL_NO_CONTEXT
        }
        if (display !== EGL14.EGL_NO_DISPLAY) {
            EGL14.eglTerminate(display)
            display = EGL14.EGL_NO_DISPLAY
        }
        config = null
    }

    private fun destroySurface() {
        if (surface !== EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroySurface(display, surface)
            surface = EGL14.EGL_NO_SURFACE
        }
    }

    private fun chooseConfig(): EGLConfig? {
        val attribs = intArrayOf(
            EGL14.EGL_RED_SIZE, COLOR_BITS,
            EGL14.EGL_GREEN_SIZE, COLOR_BITS,
            EGL14.EGL_BLUE_SIZE, COLOR_BITS,
            EGL14.EGL_ALPHA_SIZE, COLOR_BITS,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES3_BIT_KHR,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val count = IntArray(1)
        val ok = EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, count, 0)
        if (!ok || count[0] <= 0) return null
        return configs[0]
    }
}

private const val VERSION_LEN = 2
private const val GLES_VERSION = 3
private const val COLOR_BITS = 8
