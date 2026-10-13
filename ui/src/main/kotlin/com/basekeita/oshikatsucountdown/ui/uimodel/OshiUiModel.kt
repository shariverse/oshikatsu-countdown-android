package com.basekeita.oshikatsucountdown.ui.uimodel

import androidx.compose.runtime.Immutable

/**
 * 画面に出す推し。
 *
 * @property initial アバターに出す頭文字（見た目の 1 文字）。
 * @property photoPath 取り込んだ写真の絶対パス。写真が無ければ null。
 */
@Immutable
data class OshiUiModel(
    val id: String,
    val name: String,
    val initial: String,
    val color: OshiColorUiModel,
    val photoPath: String?,
)
