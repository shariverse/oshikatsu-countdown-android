package com.basekeita.oshikatsucountdown.feature.eventeditor

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.basekeita.oshikatsucountdown.navigation.EventEditorRoute

/** 予定の追加・編集を登録する。 */
fun EntryProviderScope<NavKey>.eventEditorEntry(onNavigationEvent: (EventEditorNavigationEvent) -> Unit) {
    entry<EventEditorRoute> { route ->
        EventEditorScreen(
            eventId = route.eventId,
            onboarding = route.onboarding,
            onNavigationEvent = onNavigationEvent,
        )
    }
}
