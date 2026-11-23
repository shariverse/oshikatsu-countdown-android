package com.basekeita.oshikatsucountdown.data.impl.mapping

import com.basekeita.oshikatsucountdown.data.definition.model.ReminderSettings
import com.basekeita.oshikatsucountdown.data.definition.model.ReminderTiming
import java.time.LocalTime

/**
 * 設定に保存するタイミングのキー。タイミングの名前を変えても保存済みの値が変わらないようにする。
 * 対応表に無いキーは読まない（既定の ON のまま）。
 */
internal val ReminderTiming.storageKey: String
    get() = when (this) {
        ReminderTiming.THIRTY_DAYS_BEFORE -> "thirtyDaysBefore"
        ReminderTiming.SEVEN_DAYS_BEFORE -> "sevenDaysBefore"
        ReminderTiming.DAY_BEFORE -> "dayBefore"
        ReminderTiming.ON_THE_DAY -> "onTheDay"
        ReminderTiming.HOUR_BEFORE_START -> "hourBeforeStart"
    }

private const val MINUTES_PER_DAY = 24 * 60

/** 通知の時刻を 0:00 からの分にする。 */
internal fun LocalTime.toMinutesOfDay(): Int = hour * 60 + minute

/**
 * 保存した値から通知の設定を作る。書いていない項目は既定（ON・5 つすべて・9:00）、
 * 1 日の外の分（壊れた値）は既定の 9:00 にする。
 */
internal fun reminderSettingsOf(
    enabled: Boolean?,
    timingsByStorageKey: Map<String, Boolean>,
    minutesOfDay: Int?,
): ReminderSettings {
    val defaults = ReminderSettings()
    val timeOfDay = minutesOfDay
        ?.takeIf { it in 0 until MINUTES_PER_DAY }
        ?.let { LocalTime.of(it / 60, it % 60) }
        ?: defaults.timeOfDay
    return ReminderSettings(
        enabled = enabled ?: defaults.enabled,
        timings = ReminderTiming.entries.filter { timingsByStorageKey[it.storageKey] ?: true }.toSet(),
        timeOfDay = timeOfDay,
    )
}
