package com.basekeita.oshikatsucountdown.feature.oshi

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.basekeita.oshikatsucountdown.feature.oshi.editor.OshiEditorScreen
import com.basekeita.oshikatsucountdown.feature.oshi.list.OshiListScreen
import com.basekeita.oshikatsucountdown.navigation.OshiEditorRoute
import com.basekeita.oshikatsucountdown.navigation.OshiListRoute

/** 推しのタブの最初の画面（推しの一覧）を登録する。 */
fun EntryProviderScope<NavKey>.oshiListEntry(onNavigationEvent: (OshiListNavigationEvent) -> Unit) {
    entry<OshiListRoute> {
        OshiListScreen(onNavigationEvent = onNavigationEvent)
    }
}

/** 推しの登録・編集を登録する。 */
fun EntryProviderScope<NavKey>.oshiEditorEntry(onNavigationEvent: (OshiEditorNavigationEvent) -> Unit) {
    entry<OshiEditorRoute> { route ->
        OshiEditorScreen(
            oshiId = route.oshiId,
            onboarding = route.onboarding,
            onNavigationEvent = onNavigationEvent,
        )
    }
}
