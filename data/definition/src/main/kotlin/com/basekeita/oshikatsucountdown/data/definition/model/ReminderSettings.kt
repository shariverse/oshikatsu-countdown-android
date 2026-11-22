package com.basekeita.oshikatsucountdown.data.definition.model

import java.time.LocalTime

/** 予定の通知のタイミング。並びは設定の画面に出す順。 */
enum class ReminderTiming(
    /** 開催日の何日前に出すか。開演 1 時間前は日付で決まらないので null。 */
    val daysBefore: Int?,
) {
    /** 30 日前の、設定で選んだ時刻。 */
    THIRTY_DAYS_BEFORE(30),

    /** 7 日前の、設定で選んだ時刻。 */
    SEVEN_DAYS_BEFORE(7),

    /** 前日の、設定で選んだ時刻。 */
    DAY_BEFORE(1),

    /** 当日の、設定で選んだ時刻。 */
    ON_THE_DAY(0),

    /** 開演（開始）の 1 時間前。開始の時刻がある予定だけ。 */
    HOUR_BEFORE_START(null),
}

/**
 * 予定の通知の設定。アプリ全体で 1 つ（予定ごとには持たない）。
 *
 * @property enabled 予定を通知する。OFF なら何も予約しない。
 * @property timings 出すタイミング。
 * @property timeOfDay 日付で決まる通知（N 日前・前日・当日）を出す時刻。
 */
data class ReminderSettings(
    val enabled: Boolean = true,
    val timings: Set<ReminderTiming> = ReminderTiming.entries.toSet(),
    val timeOfDay: LocalTime = DEFAULT_TIME_OF_DAY,
) {
    companion object {
        /** 通知の時刻の既定（9:00）。 */
        val DEFAULT_TIME_OF_DAY: LocalTime = LocalTime.of(9, 0)
    }
}
