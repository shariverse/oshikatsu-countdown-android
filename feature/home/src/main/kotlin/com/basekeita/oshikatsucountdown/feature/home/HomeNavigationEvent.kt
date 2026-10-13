package com.basekeita.oshikatsucountdown.feature.home

/** ホームから app に返す遷移の要求。 */
sealed interface HomeNavigationEvent {
    /** 予定の詳細を開く。 */
    data class OpenEvent(
        val eventId: String,
    ) : HomeNavigationEvent

    /** 予定を追加する。 */
    data object AddEvent : HomeNavigationEvent
}
