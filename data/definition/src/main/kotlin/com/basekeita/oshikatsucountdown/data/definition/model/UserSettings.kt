package com.basekeita.oshikatsucountdown.data.definition.model

/**
 * 設定と、アプリの進み具合。
 *
 * @property showPastEvents 終わった予定もホームに出す。
 * @property onboardingCompleted 初回の導線（推しの登録 → 最初の予定）を終えた、または飛ばした。
 */
data class UserSettings(
    val showPastEvents: Boolean = false,
    val onboardingCompleted: Boolean = false,
)
