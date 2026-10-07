package com.example.auplayer

import android.app.Application

/**
 * Manual DI root (§2: no Hilt/Koin). Modules expose interfaces;
 * fakes live beside them for JVM tests (§4 rules).
 */
class AuApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
