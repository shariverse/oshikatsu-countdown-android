package com.basekeita.oshikatsucountdown.feature.widget.uimodel

import androidx.compose.runtime.Immutable
import com.basekeita.oshikatsucountdown.ui.uimodel.EventTypeUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel
import java.time.LocalDate

/** 開催日から見た今日の位置。 */
enum class WidgetEventStatusUiModel {
    UPCOMING,
    TODAY,
    ENDED,
}

/**
 * ウィジェットと、ウィジェットに出す予定を選ぶ画面の予定 1 件。
 *
 * @property oshiName 結び付いた推しの名前。推しなしの予定は null。
 * @property initial アバターの頭文字。推しなしの予定はタイトルの頭文字。
 * @property occurrenceDate 数える対象の開催日（毎年くり返す予定は次の回）。
 * @property daysUntil 開催日までの日数。終わった予定は負。
 */
@Immutable
data class WidgetEventUiModel(
    val id: String,
    val title: String,
    val type: EventTypeUiModel,
    val oshiName: String?,
    val initial: String,
    val color: OshiColorUiModel,
    val photoPath: String?,
    val occurrenceDate: LocalDate,
    val daysUntil: Long,
    val status: WidgetEventStatusUiModel,
)
