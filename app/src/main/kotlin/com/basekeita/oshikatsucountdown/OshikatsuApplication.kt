package com.basekeita.oshikatsucountdown

import android.app.Application
import com.basekeita.oshikatsucountdown.feature.widget.WidgetDataSync
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

@HiltAndroidApp
class OshikatsuApplication : Application() {
    @Inject
    lateinit var widgetDataSync: WidgetDataSync

    /** プロセスが動いている間ずっと続ける処理（ウィジェットの描き直し）。 */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        widgetDataSync.start(applicationScope)
    }
}
