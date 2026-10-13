package com.basekeita.oshikatsucountdown.feature.settings

import androidx.annotation.RawRes
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.basekeita.oshikatsucountdown.navigation.LicensesRoute
import com.basekeita.oshikatsucountdown.navigation.SettingsRoute

/** 設定のタブの最初の画面を登録する。 */
fun EntryProviderScope<NavKey>.settingsEntry(onNavigationEvent: (SettingsNavigationEvent) -> Unit) {
    entry<SettingsRoute> {
        SettingsScreen(onNavigationEvent = onNavigationEvent)
    }
}

/**
 * ライセンスの一覧を登録する。
 *
 * @param librariesResId app の依存から作ったライブラリの一覧（`R.raw.aboutlibraries`）。
 */
fun EntryProviderScope<NavKey>.licensesEntry(
    @RawRes librariesResId: Int,
    onNavigationEvent: (LicensesNavigationEvent) -> Unit,
) {
    entry<LicensesRoute> {
        LicensesScreen(librariesResId = librariesResId, onNavigationEvent = onNavigationEvent)
    }
}
