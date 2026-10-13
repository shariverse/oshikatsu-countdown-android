package com.basekeita.oshikatsucountdown.feature.settings

/** 設定から app に返す遷移の要求。 */
sealed interface SettingsNavigationEvent {
    /** ライセンスの一覧を開く。 */
    data object OpenLicenses : SettingsNavigationEvent
}

/** ライセンスの一覧から app に返す遷移の要求。 */
sealed interface LicensesNavigationEvent {
    /** 設定に戻る。 */
    data object Back : LicensesNavigationEvent
}
