package com.basekeita.oshikatsucountdown.ui.uimodel

import androidx.annotation.StringRes
import com.basekeita.oshikatsucountdown.ui.R

/**
 * 予定の種類。
 *
 * @property startLabelRes 開始の時刻の呼び名（「開演」「配信開始」など）。
 */
enum class EventTypeUiModel(
    @param:StringRes val labelRes: Int,
    @param:StringRes val startLabelRes: Int,
) {
    LIVE(R.string.event_type_live, R.string.event_start_live),
    BIRTHDAY(R.string.event_type_birthday, R.string.event_start_default),
    RELEASE(R.string.event_type_release, R.string.event_start_release),
    STREAM(R.string.event_type_stream, R.string.event_start_stream),
    OTHER(R.string.event_type_other, R.string.event_start_default),
}
