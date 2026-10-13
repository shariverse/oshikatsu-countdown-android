package com.basekeita.oshikatsucountdown.feature.home

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.basekeita.oshikatsucountdown.navigation.HomeRoute

/** ホームのタブの最初の画面を登録する。 */
fun EntryProviderScope<NavKey>.homeEntry(onNavigationEvent: (HomeNavigationEvent) -> Unit) {
    entry<HomeRoute> {
        HomeScreen(onNavigationEvent = onNavigationEvent)
    }
}
