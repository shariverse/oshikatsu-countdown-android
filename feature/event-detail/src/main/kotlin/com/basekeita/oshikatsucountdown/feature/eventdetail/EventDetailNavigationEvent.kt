package com.basekeita.oshikatsucountdown.feature.eventdetail

/** 予定の詳細から app に返す遷移の要求。 */
sealed interface EventDetailNavigationEvent {
    /** 前の画面に戻る（戻るボタン・削除の後）。 */
    data object Back : EventDetailNavigationEvent

    /** 予定を編集する。 */
    data class Edit(
        val eventId: String,
    ) : EventDetailNavigationEvent
}
