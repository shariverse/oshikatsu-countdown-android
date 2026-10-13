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
    val oshiId: String?,
    val type: EventTypeUiModel,
    val title: String,
    val date: LocalDate,
    val startTime: LocalTime?,
    val venue: String,
    val repeatsYearly: Boolean,
)
