package com.example.auplayer

import android.content.Context
import com.example.auplayer.enhance.EnhanceController
import com.example.auplayer.player.FakeMpvBridge
import com.example.auplayer.player.LibmpvBridge
import com.example.auplayer.player.MpvNative
import com.example.auplayer.player.MpvPaths
import com.example.auplayer.player.PlayerEngine
import java.io.File

/** Manual DI root (§2: no Hilt/Koin). Fakes back seams until natives land. */
class AppContainer(context: Context) {
    val webSourceEnabled: Boolean = BuildConfig.WEB_SOURCE_ENABLED

    private val mpvPaths = MpvPaths(
        configDir = File(context.filesDir, "mpv"),
        cacheDir = context.cacheDir,
        filesDir = context.filesDir
    )

    /**
     * Single engine for the app (§3.4). Real LibmpvBridge when native loads,
     * Fake bridge otherwise (unit tests, devices without the .so).
     */
    val playerEngine: PlayerEngine by lazy {
        if (MpvNative.load()) {
            PlayerEngine(LibmpvBridge(mpvPaths, context.getExternalFilesDir(null)))
        } else {
            PlayerEngine(FakeMpvBridge())
        }
    }

    /** Enhance controller wired to the single engine (no module cycle). */
    val enhanceController: EnhanceController by lazy {
        EnhanceController(playerEngine::send)
    }
}
