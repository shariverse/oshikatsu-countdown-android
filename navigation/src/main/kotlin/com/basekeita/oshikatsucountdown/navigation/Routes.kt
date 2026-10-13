package com.basekeita.oshikatsucountdown.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** ホームのタブの最初の画面。 */
@Serializable
data object HomeRoute : NavKey

/** 推しのタブの最初の画面（推しの一覧）。 */
@Serializable
data object OshiListRoute : NavKey

/** 設定のタブの最初の画面。 */
@Serializable
data object SettingsRoute : NavKey

/** 設定から開く、アプリで使っているライブラリのライセンスの一覧。 */
@Serializable
data object LicensesRoute : NavKey

/** 予定の詳細（秒単位のカウントダウン）。 */
@Serializable
data class EventDetailRoute(
    val eventId: String,
) : NavKey

/**
 * 予定の追加・編集。
 *
 * @param eventId 編集する予定。追加するときは null。
 * @param onboarding 初回の導線の 2 歩目として開く。
 */
@Serializable
data class EventEditorRoute(
    val eventId: String? = null,
    val onboarding: Boolean = false,
) : NavKey

/**
 * 推しの登録・編集。
 *
 * @param oshiId 編集する推し。登録するときは null。
 * @param onboarding 初回の導線の 1 歩目として開く。
 */
@Serializable
data class OshiEditorRoute(
    val oshiId: String? = null,
    val onboarding: Boolean = false,
) : NavKey
