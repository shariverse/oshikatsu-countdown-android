package com.basekeita.oshikatsucountdown.feature.reminder

import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.model.OshiColor
import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import com.basekeita.oshikatsucountdown.domain.reminder.LiveCountdownWindow
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel
import java.time.Instant
import java.util.UUID

/**
 * 当日のカウントダウンの中身。iOS 版のライブアクティビティの中身（`EventActivityAttributes`）と同じ項目。
 *
 * @property heading 推しの名前（推しなしの予定は種類）。
 * @property color 推し色（推しなしの予定はブラック）。
 * @property start 開演の時点。ここまでを通知の時計で数える。
 * @property untilText 残りの見出し（`開演まで` など）。
 * @property startText 開始の時刻（`開演 17:00` など）。
 * @property startedText 開演を過ぎた後の文言（`今日は当日！`）。
 */
internal data class LiveCountdownContent(
    val eventId: UUID,
    val heading: String,
    val title: String,
    val color: OshiColorUiModel,
    val start: Instant,
    val end: Instant,
    val untilText: String,
    val startText: String,
    val startedText: String,
) {
    /** 開演を過ぎたか。過ぎたら残りの代わりに「今日は当日！」を出す。 */
    fun isStarted(now: Instant): Boolean = now >= start
}

internal fun liveCountdownContentOf(
    window: LiveCountdownWindow,
    event: OshiEvent,
    oshi: Oshi?,
    wording: ReminderWording,
): LiveCountdownContent = LiveCountdownContent(
    eventId = event.id,
    heading = oshi?.name ?: wording.typeLabel(event.type),
    title = event.title,
    color = (oshi?.color ?: OshiColor.BLACK).toUiModel(),
    start = window.start,
    end = window.end,
    untilText = wording.untilStart(event.type),
    startText = wording.startTime(event),
    startedText = wording.started,
)

private fun OshiColor.toUiModel(): OshiColorUiModel = when (this) {
    OshiColor.PINK -> OshiColorUiModel.PINK
    OshiColor.RED -> OshiColorUiModel.RED
    OshiColor.ORANGE -> OshiColorUiModel.ORANGE
    OshiColor.YELLOW -> OshiColorUiModel.YELLOW
    OshiColor.GREEN -> OshiColorUiModel.GREEN
    OshiColor.MINT -> OshiColorUiModel.MINT
    OshiColor.BLUE -> OshiColorUiModel.BLUE
    OshiColor.NAVY -> OshiColorUiModel.NAVY
    OshiColor.PURPLE -> OshiColorUiModel.PURPLE
    OshiColor.LAVENDER -> OshiColorUiModel.LAVENDER
    OshiColor.BROWN -> OshiColorUiModel.BROWN
    OshiColor.BLACK -> OshiColorUiModel.BLACK
}
