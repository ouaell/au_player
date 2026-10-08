package com.example.auplayer

import android.content.Context
import com.example.auplayer.enhance.EnhanceController
import com.example.auplayer.player.FakeMpvBridge
import com.example.auplayer.player.PlayerEngine

/** Manual DI root (§2: no Hilt/Koin). Fakes back seams until natives land. */
class AppContainer(@Suppress("UNUSED_PARAMETER") context: Context) {
    val webSourceEnabled: Boolean = BuildConfig.WEB_SOURCE_ENABLED

    /** Single engine for the app (§3.4). Fake bridge until the self-built AAR lands. */
    val playerEngine: PlayerEngine by lazy { PlayerEngine(FakeMpvBridge()) }

    /** Enhance controller wired to the single engine (no module cycle). */
    val enhanceController: EnhanceController by lazy {
        EnhanceController(playerEngine::send)
    }
}
