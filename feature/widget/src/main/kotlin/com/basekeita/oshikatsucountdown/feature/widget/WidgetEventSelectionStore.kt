package com.basekeita.oshikatsucountdown.feature.widget

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.basekeita.oshikatsucountdown.feature.widget.glance.EventCountdownWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** 1 件のウィジェットに出す予定の id を、ウィジェットに紐付けて持つ状態のキー。 */
internal val SELECTED_EVENT_ID_KEY = stringPreferencesKey("selected_event_id")

/** 1 件のウィジェット（`EventCountdownWidget`）に出す予定。ウィジェットごとに 1 件。 */
interface WidgetEventSelectionStore {
    /** 選んである予定の id。選んでいなければ null。 */
    suspend fun load(appWidgetId: Int): String?

    /** 予定を選び、ウィジェットを描き直す。 */
    suspend fun save(
        appWidgetId: Int,
        eventId: String,
    )
}

/**
 * Glance のウィジェットごとの状態に持つ。ウィジェットを外すと Glance が状態ごと消すので、ここで消す処理は持たない。
 */
internal class GlanceWidgetEventSelectionStore
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
) : WidgetEventSelectionStore {
    override suspend fun load(appWidgetId: Int): String? {
        val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
        return getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)[SELECTED_EVENT_ID_KEY]
    }

    override suspend fun save(
        appWidgetId: Int,
        eventId: String,
    ) {
        val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
        updateAppWidgetState(context, glanceId) { preferences -> preferences[SELECTED_EVENT_ID_KEY] = eventId }
        EventCountdownWidget().update(context, glanceId)
    }
}
