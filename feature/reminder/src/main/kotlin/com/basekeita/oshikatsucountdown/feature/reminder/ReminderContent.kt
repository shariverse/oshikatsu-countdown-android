package com.basekeita.oshikatsucountdown.feature.reminder

import android.content.Context
import com.basekeita.oshikatsucountdown.data.definition.model.EventType
import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import com.basekeita.oshikatsucountdown.data.definition.model.ReminderTiming
import com.basekeita.oshikatsucountdown.ui.rule.formatMonthDayWeekday
import com.basekeita.oshikatsucountdown.ui.rule.formatTime
import com.basekeita.oshikatsucountdown.ui.uimodel.EventTypeUiModel
import java.time.LocalDate
import com.basekeita.oshikatsucountdown.ui.R as UiR

/**
 * 予定の通知の文面。
 *
 * - タイトル: 予定のタイトル
 * - サブタイトル: 推しの名前（推しなしの予定は種類）。ウィジェットのカウントダウンと同じ
 * - 本文: タイミングの文言（`あと 7 日`・`いよいよ明日！`・`今日！`・`あと 1 時間`）に、前の日に出すものは開催日、
 *   開始の時刻があればそれを ` · ` で続ける
 */
internal data class ReminderContent(
    val title: String,
    val subtitle: String,
    val body: String,
)

/** 文面に使う文言。端末の文言のリソースから引く（テストでは固定の文言を渡す）。 */
internal interface ReminderWording {
    fun daysLeft(days: Int): String

    val dayBefore: String
    val today: String
    val hourLeft: String

    fun typeLabel(type: EventType): String

    /** 開始の時刻（`開演 17:00`）。 */
    fun startTime(event: OshiEvent): String

    fun monthDayWeekday(date: LocalDate): String

    /** 当日のカウントダウンの残りの見出し（`開演まで`。種類で変わる）。 */
    fun untilStart(type: EventType): String

    /** 当日のカウントダウンの、開演を過ぎた後の文言（`今日は当日！`）。 */
    val started: String
}

internal fun reminderContentOf(
    timing: ReminderTiming,
    occurrenceDate: LocalDate,
    event: OshiEvent,
    oshiName: String?,
    wording: ReminderWording,
): ReminderContent {
    val lead = when (timing) {
        ReminderTiming.THIRTY_DAYS_BEFORE, ReminderTiming.SEVEN_DAYS_BEFORE -> wording.daysLeft(timing.daysBefore ?: 0)
        ReminderTiming.DAY_BEFORE -> wording.dayBefore
        ReminderTiming.ON_THE_DAY -> wording.today
        ReminderTiming.HOUR_BEFORE_START -> wording.hourLeft
    }
    val parts = buildList {
        add(lead)
        // 当日と開演 1 時間前は今日のことなので、日付は出さない
        val daysBefore = timing.daysBefore
        if (daysBefore != null && daysBefore > 0) add(wording.monthDayWeekday(occurrenceDate))
        if (event.startTime != null) add(wording.startTime(event))
    }
    return ReminderContent(
        title = event.title,
        subtitle = oshiName ?: wording.typeLabel(event.type),
        // ホームの一覧の行（`日付 · 推しの名前`）と同じ区切り
        body = parts.joinToString(separator = " · "),
    )
}

/** 端末の文言のリソースから引く文言。 */
internal class ResourceReminderWording(
    private val context: Context,
) : ReminderWording {
    override fun daysLeft(days: Int): String = context.getString(R.string.reminder_days_left, days)

    override val dayBefore: String get() = context.getString(R.string.reminder_day_before)
    override val today: String get() = context.getString(UiR.string.countdown_today)
    override val hourLeft: String get() = context.getString(R.string.reminder_hour_left)

    override fun typeLabel(type: EventType): String = context.getString(type.toUiModel().labelRes)

    override fun startTime(event: OshiEvent): String {
        val startTime = event.startTime ?: return ""
        return context.getString(R.string.reminder_start_time, context.getString(event.type.toUiModel().startLabelRes), formatTime(startTime))
    }

    override fun monthDayWeekday(date: LocalDate): String = formatMonthDayWeekday(date)

    override fun untilStart(type: EventType): String = context.getString(R.string.countdown_until_start, context.getString(type.toUiModel().startLabelRes))

    override val started: String get() = context.getString(R.string.countdown_started)
}

private fun EventType.toUiModel(): EventTypeUiModel = when (this) {
    EventType.LIVE -> EventTypeUiModel.LIVE
    EventType.BIRTHDAY -> EventTypeUiModel.BIRTHDAY
    EventType.RELEASE -> EventTypeUiModel.RELEASE
    EventType.STREAM -> EventTypeUiModel.STREAM
    EventType.OTHER -> EventTypeUiModel.OTHER
}
