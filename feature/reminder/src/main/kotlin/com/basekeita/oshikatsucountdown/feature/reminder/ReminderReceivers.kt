package com.basekeita.oshikatsucountdown.feature.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.basekeita.oshikatsucountdown.data.definition.model.ReminderTiming
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * 予約した時刻に通知を出し、空いた枠を埋めるために予約し直す。
 * 文面は出すときに読み直す（予約の後に推しの名前を変えても、今の名前で出す）。
 * 当日のカウントダウンも置き直す（8 日先までしか予約しないので、7 日前・前日の通知のときに、アプリを開かなくても先の予約を足す）。
 */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACTION_FIRE_REMINDER) return

        val identifier = intent.getStringExtra(EXTRA_IDENTIFIER) ?: return
        val eventId = intent.getStringExtra(EXTRA_EVENT_ID)?.let(UUID::fromString) ?: return
        val timing = intent.getStringExtra(EXTRA_TIMING)?.let { name -> ReminderTiming.entries.firstOrNull { it.name == name } } ?: return
        val occurrenceDate = intent.getStringExtra(EXTRA_OCCURRENCE_DATE)?.let(LocalDate::parse) ?: return
        val fireAt = intent.getLongExtra(EXTRA_FIRE_AT, -1L).takeIf { it >= 0 }?.let(Instant::ofEpochMilli) ?: return

        val entryPoint = EntryPointAccessors.fromApplication<ReminderEntryPoint>(context)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val settings = entryPoint.userSettingsRepository().readReminderSettings()
                val event = entryPoint.eventRepository().readEvents().firstOrNull { it.id == eventId }
                val zone = entryPoint.clock().zone
                if (isReminderStillPlanned(entryPoint.planReminders(), event, settings, identifier, fireAt, zone) && event != null) {
                    val oshiName = event.oshiId?.let { oshiId -> entryPoint.oshiRepository().readOshis().firstOrNull { it.id == oshiId }?.name }
                    val content = reminderContentOf(timing, occurrenceDate, event, oshiName, ResourceReminderWording(context))
                    postReminderNotification(context, identifier, eventId, content)
                }
                entryPoint.reminderSync().refreshAll()
            } catch (exception: IOException) {
                Log.w(TAG, "通知を出せませんでした", exception)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "ReminderAlarmReceiver"
    }
}

/**
 * 予定の通知と当日のカウントダウンを置き直すきっかけのうち、システムから届くもの。
 * 再起動（アラームは再起動で消える）、アプリの更新、端末の時刻・タイムゾーンの変更（アラームは UTC で入っているので計画し直す）、
 * 正確なアラームの許可（Android 12。許可されたときだけ届く。取り消されると入れてあった正確なアラームは消える）。
 */
class ReminderSystemReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action !in systemActions) return

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

    private companion object {
        val systemActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )

        /**
         * `AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`（Android 12 で足された知らせ）。
         * Android 11 では届かないだけなので版で分けずに受ける。API 31 の定数を参照すると lint（InlinedApi）が止めるので、同じ値の文字列で持つ。
         */
        const val ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED = "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
    }
}
