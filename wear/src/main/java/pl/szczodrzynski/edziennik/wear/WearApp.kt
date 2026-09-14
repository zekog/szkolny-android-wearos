/*
 * Copyright (c) Kuba Szczodrzyński 2024.
 */

package pl.szczodrzynski.edziennik.wear

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.wear.data.WearRepository

class WearApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val repository: WearRepository by lazy { WearRepository(this) }

    override fun onCreate() {
        super.onCreate()
        // parse the cached JSON off the main thread to keep startup smooth
        appScope.launch(Dispatchers.IO) {
            repository.loadCache()
        }
    }
}
