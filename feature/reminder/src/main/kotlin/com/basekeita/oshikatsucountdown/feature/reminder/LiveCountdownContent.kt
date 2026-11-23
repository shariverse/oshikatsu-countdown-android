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

/**
 * 当日のカウントダウンの、標準の通知の枠への置き方。
 *
 * iOS 版は「開演まで」のすぐ下に残りを出し、組にして読ませる。標準の通知では、残り（時計）は見出しの行の末尾に出て、
 * そのすぐ前がサブテキストになる（`ライブ · 開演まで · 2:59:17`）。そのため「開演まで」はサブテキストに入れて時計と組にし、
 * 推しの名前と開始の時刻は本文に出す（`テイルズ · 開演 17:00`）。開演を過ぎたら、サブテキストを「今日は当日！」にして時計を消す。
 *
 * @property subText 見出しの行の、時計のすぐ前に出す文言。
 * @property countsDown 開演までの残りを通知の時計で数える（開演を過ぎたら数えない）。
 * @property timeoutMillis 開演の 1 時間後に消えるまでの残り（過ぎていれば 0）。
 */
internal data class CountdownSlots(
    val subText: String,
    val countsDown: Boolean,
    val timeoutMillis: Long,
)

internal fun LiveCountdownContent.slotsAt(now: Instant): CountdownSlots {
    val started = isStarted(now)
    val subText = if (started) {
        startedText
    } else {
        untilText
    }
    return CountdownSlots(
        subText = subText,
        countsDown = !started,
        timeoutMillis = (end.toEpochMilli() - now.toEpochMilli()).coerceAtLeast(0),
    )
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
