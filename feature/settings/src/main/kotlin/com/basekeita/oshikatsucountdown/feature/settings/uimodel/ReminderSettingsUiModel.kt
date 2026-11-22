package com.basekeita.oshikatsucountdown.feature.settings.uimodel

import androidx.annotation.StringRes
import com.basekeita.oshikatsucountdown.data.definition.model.ReminderSettings
import com.basekeita.oshikatsucountdown.data.definition.model.ReminderTiming
import com.basekeita.oshikatsucountdown.feature.settings.R
import java.time.LocalTime

/** 通知のタイミング。並びは設定の画面に出す順。 */
enum class ReminderTimingUiModel(
    @param:StringRes val labelRes: Int,
) {
    THIRTY_DAYS_BEFORE(R.string.settings_reminder_timing_thirty_days),
    SEVEN_DAYS_BEFORE(R.string.settings_reminder_timing_seven_days),
    DAY_BEFORE(R.string.settings_reminder_timing_day_before),
    ON_THE_DAY(R.string.settings_reminder_timing_on_the_day),
    HOUR_BEFORE_START(R.string.settings_reminder_timing_hour_before),
}

/**
 * 通知の設定。
 *
 * @property enabledTimings 出すタイミング。
 * @property timeOfDay 日付で決まる通知（N 日前・前日・当日）を出す時刻。
 */
data class ReminderSettingsUiModel(
    val enabled: Boolean = true,
    val enabledTimings: Set<ReminderTimingUiModel> = ReminderTimingUiModel.entries.toSet(),
    val timeOfDay: LocalTime = ReminderSettings.DEFAULT_TIME_OF_DAY,
)

/** 通知の許可の状態。設定の画面の案内を出し分ける。 */
enum class NotificationPermissionUiModel {
    /** 通知を出せる。案内は出さない。 */
    GRANTED,

    /** まだ許可を求めていない（Android 13 以降）。「通知を許可する」を出す。 */
    NOT_REQUESTED,

    /** 許可されていない、または端末の設定で通知が OFF。「設定を開く」で端末の設定のこのアプリの通知の画面を開く。 */
    DENIED,

    /** 正確なアラームを使えない（Android 12）。「設定を開く」で端末の設定のこのアプリのアラームの画面を開く。 */
    EXACT_ALARM_DENIED,
}

internal fun ReminderSettings.toUiModel(): ReminderSettingsUiModel = ReminderSettingsUiModel(
    enabled = enabled,
    enabledTimings = timings.map { it.toUiModel() }.toSet(),
    timeOfDay = timeOfDay,
)

internal fun ReminderTiming.toUiModel(): ReminderTimingUiModel = when (this) {
    ReminderTiming.THIRTY_DAYS_BEFORE -> ReminderTimingUiModel.THIRTY_DAYS_BEFORE
    ReminderTiming.SEVEN_DAYS_BEFORE -> ReminderTimingUiModel.SEVEN_DAYS_BEFORE
    ReminderTiming.DAY_BEFORE -> ReminderTimingUiModel.DAY_BEFORE
    ReminderTiming.ON_THE_DAY -> ReminderTimingUiModel.ON_THE_DAY
    ReminderTiming.HOUR_BEFORE_START -> ReminderTimingUiModel.HOUR_BEFORE_START
}

internal fun ReminderTimingUiModel.toModel(): ReminderTiming = when (this) {
    ReminderTimingUiModel.THIRTY_DAYS_BEFORE -> ReminderTiming.THIRTY_DAYS_BEFORE
    ReminderTimingUiModel.SEVEN_DAYS_BEFORE -> ReminderTiming.SEVEN_DAYS_BEFORE
    ReminderTimingUiModel.DAY_BEFORE -> ReminderTiming.DAY_BEFORE
    ReminderTimingUiModel.ON_THE_DAY -> ReminderTiming.ON_THE_DAY
    ReminderTimingUiModel.HOUR_BEFORE_START -> ReminderTiming.HOUR_BEFORE_START
}

/**
 * 許可の状態を決める。SPEC の「通知が許可されている」は、通知を出せることと正確なアラームを使えることの両方。
 * 通知を出せなければ先にそちらを案内する。Android には「まだ聞いていない」の状態が無いので、一度求めたかどうかの印で分ける。
 * 許可のダイアログが無い版（Android 12 以前）で通知が OFF なら、端末の設定で ON にしてもらう。
 *
 * @param granted 通知を出せる。
 * @param exactAlarmsAllowed 正確なアラームを使える（Android 12 だけ利用者が切り替える）。
 */
internal fun notificationPermissionOf(
    granted: Boolean,
    canRequest: Boolean,
    requested: Boolean,
    exactAlarmsAllowed: Boolean,
): NotificationPermissionUiModel = if (!granted && canRequest && !requested) {
    NotificationPermissionUiModel.NOT_REQUESTED
} else if (!granted) {
    NotificationPermissionUiModel.DENIED
} else if (!exactAlarmsAllowed) {
    NotificationPermissionUiModel.EXACT_ALARM_DENIED
} else {
    NotificationPermissionUiModel.GRANTED
}
