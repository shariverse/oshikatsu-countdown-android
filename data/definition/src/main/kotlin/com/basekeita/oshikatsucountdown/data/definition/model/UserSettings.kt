package com.basekeita.oshikatsucountdown.data.definition.model

/**
 * 設定と、アプリの進み具合。
 *
 * @property showPastEvents 終わった予定もホームに出す。
 * @property onboardingCompleted 初回の導線（推しの登録 → 最初の予定）を終えた、または飛ばした。
 * @property reminders 予定の通知の設定。
 * @property notificationPermissionRequested 通知の許可を一度求めた。Android には「まだ聞いていない」の状態が無いため、
 *   設定の画面で「通知を許可する」と「設定を開く」を出し分けるのに使う。
 */
data class UserSettings(
    val showPastEvents: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val reminders: ReminderSettings = ReminderSettings(),
    val notificationPermissionRequested: Boolean = false,
)
