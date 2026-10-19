package com.adheesh.gid

import android.app.Application
import com.adheesh.gid.app.AppContainer

// ── GIDApplication ────────────────────────────────────────────────────────────
// The Application subclass holds the single AppContainer instance.
// Every Activity (and via it, every ViewModel) can access repositories through
//   (application as GIDApplication).container
// In a future milestone this will be passed into ViewModelFactory.
class GIDApplication : Application() {

    // Initialized lazily so the Application context is ready when accessed
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
