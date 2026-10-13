package com.basekeita.oshikatsucountdown.navigation

/**
 * アプリの外（ホーム画面のウィジェット）からアプリを開くときの Intent の extra。
 * app はこれを読んで、ホームのタブに画面を積む。
 */
object ExternalLaunchExtras {
    /** 予定の詳細を開く。値は予定の id（String）。 */
    const val EVENT_ID = "com.basekeita.oshikatsucountdown.extra.EVENT_ID"

    /** 予定の追加を開く。値は true（Boolean）。 */
    const val ADD_EVENT = "com.basekeita.oshikatsucountdown.extra.ADD_EVENT"
}
