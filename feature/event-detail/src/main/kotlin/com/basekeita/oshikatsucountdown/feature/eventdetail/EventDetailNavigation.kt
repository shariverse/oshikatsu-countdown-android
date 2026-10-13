package com.basekeita.oshikatsucountdown.feature.eventdetail

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.basekeita.oshikatsucountdown.navigation.EventDetailRoute

/** 予定の詳細を登録する。 */
fun EntryProviderScope<NavKey>.eventDetailEntry(onNavigationEvent: (EventDetailNavigationEvent) -> Unit) {
    entry<EventDetailRoute> { route ->
        EventDetailScreen(eventId = route.eventId, onNavigationEvent = onNavigationEvent)
    }
}
