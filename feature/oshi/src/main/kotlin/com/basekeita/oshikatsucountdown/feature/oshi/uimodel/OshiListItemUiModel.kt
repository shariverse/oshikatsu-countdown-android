package com.basekeita.oshikatsucountdown.feature.oshi.uimodel

import androidx.compose.runtime.Immutable
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiUiModel

/** 一覧の推し 1 行。 */
@Immutable
data class OshiListItemUiModel(
    val oshi: OshiUiModel,
    val eventCount: Int,
)
