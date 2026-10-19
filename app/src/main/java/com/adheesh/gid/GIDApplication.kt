package com.adheesh.gid

import android.app.Application
import com.adheesh.gid.app.AppContainer

// ── GIDApplication ────────────────────────────────────────────────────────────
// Holds the single AppContainer. Screens reach it through their ViewModel
// factories; receivers and services reach it through applicationContext.
class GIDApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.onAppStart()
    }
}
