package com.basekeita.oshikatsucountdown.feature.eventeditor

/** 予定の追加・編集から app に返す遷移の要求。 */
sealed interface EventEditorNavigationEvent {
    /** 保存し終えた、または保存せずに閉じた（初回の導線では「あとで」）。 */
    data object Finished : EventEditorNavigationEvent
}
