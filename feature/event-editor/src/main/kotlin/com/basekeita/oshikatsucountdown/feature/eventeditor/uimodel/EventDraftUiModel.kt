package com.basekeita.oshikatsucountdown.feature.eventeditor.uimodel

import androidx.compose.runtime.Immutable
import com.basekeita.oshikatsucountdown.ui.uimodel.EventTypeUiModel
import java.time.LocalDate
import java.time.LocalTime

/**
 * 入力中の予定。
 *
 * @property oshiId 選んだ推し。推しなしは null。
 * @property title 入力中のタイトル（空白は保存するときに整える）。
 */
@Immutable
data class EventDraftUiModel(
    val oshiId: String? = null,
    val type: EventTypeUiModel = EventTypeUiModel.LIVE,
    val title: String = "",
    // 読み込む前の仮の値（`LocalDate.EPOCH` は API 34 から）。開いたときに今日か編集元の日付に置き換える
    val date: LocalDate = LocalDate.of(1970, 1, 1),
    val startTime: LocalTime? = null,
    val venue: String = "",
    val repeatsYearly: Boolean = false,
)

/** 種類を選ぶ。誕生日は毎年くり返し、それ以外はくり返さない（あとから切り替えられる）。 */
internal fun EventDraftUiModel.withType(type: EventTypeUiModel): EventDraftUiModel = copy(type = type, repeatsYearly = type == EventTypeUiModel.BIRTHDAY)

/** 選んでいた推しが消えていたら、推しなしにする。 */
internal fun EventDraftUiModel.withExistingOshi(oshiIds: Set<String>): EventDraftUiModel = if (oshiId != null && oshiId !in oshiIds) {
    copy(oshiId = null)
} else {
    this
}
