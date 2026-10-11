package com.giwu.bible

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class GiwuApplication : Application() {

    /**
     * Outlives every screen: session restore, bookmark loading and read-aloud
     * all have to keep going while the reader moves between screens.
     */
    private val appScope = CoroutineScope(SupervisorJob())

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this, appScope)
        container.observePlaybackSettings(appScope)
    }
}

/** The app graph, for composables that need a repository or a store. */
@Composable
fun rememberAppContainer(): AppContainer {
    val context = LocalContext.current
    return (context.applicationContext as GiwuApplication).container
}
