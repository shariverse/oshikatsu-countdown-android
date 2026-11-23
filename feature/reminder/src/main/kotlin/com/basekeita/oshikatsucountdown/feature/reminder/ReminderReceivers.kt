package com.basekeita.oshikatsucountdown.feature.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.basekeita.oshikatsucountdown.data.definition.model.ReminderTiming
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * `AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`（Android 12 で足された知らせ）。
 * Android 11 では届かないだけなので版で分けずに受ける。API 31 の定数を参照すると lint（InlinedApi）が止めるので、同じ値の文字列で持つ。
 */
internal const val ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED = "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"

/**
 * 予定の通知と当日のカウントダウンを置き直すきっかけのうち、システムから届くもの（マニフェストの `ReminderSystemReceiver` の intent-filter と同じ）。
 * 再起動（アラームは再起動で消える）、アプリの更新、端末の時刻・タイムゾーンの変更（アラームは UTC で入っているので計画し直す）、
 * 正確なアラームの許可（Android 12。許可されたときだけ届く。取り消されると入れてあった正確なアラームは消える）。
 */
internal val REMINDER_SYSTEM_ACTIONS = setOf(
    Intent.ACTION_BOOT_COMPLETED,
    Intent.ACTION_MY_PACKAGE_REPLACED,
    Intent.ACTION_TIME_CHANGED,
    Intent.ACTION_TIMEZONE_CHANGED,
    ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
)

/** 予約した時刻に、予定の通知を出す（`ReminderPoster`）。 */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACTION_FIRE_REMINDER) return

        val fired = intent.toFiredReminder() ?: return

        val poster = EntryPointAccessors.fromApplication<ReminderEntryPoint>(context).reminderPoster()
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                poster.post(fired)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/** アラームの extra から、届いた予定の通知を読む。どれかが欠けていれば（版が変わって読めないなど）null。 */
private fun Intent.toFiredReminder(): FiredReminder? {
    val identifier = getStringExtra(EXTRA_IDENTIFIER) ?: return null

    val eventId = getStringExtra(EXTRA_EVENT_ID)?.let(UUID::fromString) ?: return null

    val timing = getStringExtra(EXTRA_TIMING)?.let { name -> ReminderTiming.entries.firstOrNull { it.name == name } } ?: return null

    val occurrenceDate = getStringExtra(EXTRA_OCCURRENCE_DATE)?.let(LocalDate::parse) ?: return null

    val fireAt = getLongExtra(EXTRA_FIRE_AT, -1L).takeIf { it >= 0 }?.let(Instant::ofEpochMilli) ?: return null

    return FiredReminder(identifier, eventId, timing, occurrenceDate, fireAt)
}

/** システムから届くきっかけ（`REMINDER_SYSTEM_ACTIONS`）で、予定の通知と当日のカウントダウンを置き直す。 */
class ReminderSystemReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action !in REMINDER_SYSTEM_ACTIONS) return

        val reminderSync = EntryPointAccessors.fromApplication<ReminderEntryPoint>(context).reminderSync()
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                reminderSync.refreshAll()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
