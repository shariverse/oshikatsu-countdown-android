package com.basekeita.oshikatsucountdown.feature.oshi

/** 推しの一覧から app に返す遷移の要求。 */
sealed interface OshiListNavigationEvent {
    /** 推しを編集する。 */
    data class OpenOshi(
        val oshiId: String,
    ) : OshiListNavigationEvent

    /** 推しを登録する。 */
    data object AddOshi : OshiListNavigationEvent
}

/** 推しの登録・編集から app に返す遷移の要求。 */
sealed interface OshiEditorNavigationEvent {
    /** 保存・削除し終えた、または保存せずに閉じた（初回の導線では「あとで」）。 */
    data object Finished : OshiEditorNavigationEvent
}
