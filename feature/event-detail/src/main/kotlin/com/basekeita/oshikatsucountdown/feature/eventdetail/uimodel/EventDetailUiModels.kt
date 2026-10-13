package com.basekeita.oshikatsucountdown.feature.eventdetail.uimodel

import androidx.compose.runtime.Immutable
import com.basekeita.oshikatsucountdown.ui.uimodel.EventTypeUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel
import java.time.LocalDate
import java.time.LocalTime

/** 開始の時刻から見た今の位置。 */
enum class CountdownStatusUiModel {
    UPCOMING,
    TODAY,
    ENDED,
}

/**
 * 詳細に出す予定。
 *
 * @property occurrenceDate 数える対象の開催日（毎年くり返す予定は次の回）。
 */
@Immutable
data class EventDetailUiModel(
    val title: String,
    val type: EventTypeUiModel,
    val oshiName: String?,
    val color: OshiColorUiModel,
    val photoPath: String?,
    val occurrenceDate: LocalDate,
    val startTime: LocalTime?,
    val venue: String,
    val repeatsYearly: Boolean,
    val pinned: Boolean,
)

/**
 * 秒単位のカウントダウン。
 *
 * @property days 開始までの残りのうちの日数（24 時間単位）。時・分・秒はその残り。
 * @property daysUntil 開催日までの暦の日数。終わった予定は負（何日前か）。
 * @property progress 登録から開始までの経過の割合（0〜1）。求められないときは null。
 */
@Immutable
data class CountdownUiModel(
    val status: CountdownStatusUiModel,
    val days: Long,
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
    val daysUntil: Long,
    val progress: Float?,
    val progressStartDate: LocalDate,
)
