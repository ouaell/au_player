package com.example.auplayer

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Manual DI root (§2: no Hilt/Koin). Modules expose interfaces;
 * fakes live beside them for JVM tests (§4 rules).
 */
class AuApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    /**
     * Outlives any composition: resume-point saves fired from
     * DisposableEffect.onDispose must survive the teardown that triggered
     * them (a composable rememberCoroutineScope is already cancelled there —
     * E9 flake).
     */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onTerminate() {
        applicationScope.cancel()
        super.onTerminate()
    }
}
