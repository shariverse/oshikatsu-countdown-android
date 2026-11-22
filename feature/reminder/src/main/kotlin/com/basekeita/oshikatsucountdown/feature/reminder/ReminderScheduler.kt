package com.basekeita.oshikatsucountdown.feature.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.basekeita.oshikatsucountdown.data.definition.repository.EventRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.UserSettingsRepository
import com.basekeita.oshikatsucountdown.domain.reminder.PlanRemindersUseCase
import com.basekeita.oshikatsucountdown.domain.reminder.PlannedReminder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.time.Clock
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

internal const val ACTION_FIRE_REMINDER = "com.basekeita.oshikatsucountdown.feature.reminder.action.FIRE_REMINDER"
internal const val EXTRA_IDENTIFIER = "com.basekeita.oshikatsucountdown.feature.reminder.extra.IDENTIFIER"
internal const val EXTRA_EVENT_ID = "com.basekeita.oshikatsucountdown.feature.reminder.extra.EVENT_ID"
internal const val EXTRA_TIMING = "com.basekeita.oshikatsucountdown.feature.reminder.extra.TIMING"
internal const val EXTRA_OCCURRENCE_DATE = "com.basekeita.oshikatsucountdown.feature.reminder.extra.OCCURRENCE_DATE"
internal const val EXTRA_FIRE_AT = "com.basekeita.oshikatsucountdown.feature.reminder.extra.FIRE_AT"

/**
 * 予定の通知の予約。計画（`PlanRemindersUseCase`）のとおりに AlarmManager のアラームを置き換える。
 *
 * - 決めた時刻ちょうどに出す（iOS 版は日付と時刻を指定して予約する）ので、アラームは `setExactAndAllowWhileIdle`（`RTC_WAKEUP`）。
 *   端末の待機中（Doze）でも出す。正確なアラームを使えなければ予約を残さない（`areReminderNotificationsAllowed`）
 * - アラームは端末の時刻（UTC）で入るので、タイムゾーンが変わったら計画し直す（その土地の同じ日付・時刻に出す）
 * - 近い順に上限まで予約するので、推し・予定・設定の変更、アプリが前面に来たとき、通知を出したとき、
 *   再起動・アプリの更新・時刻・タイムゾーンの変更・正確なアラームの許可の変更のたびに予約し直す
 * - 通知を出せない（許可が無い・端末の設定で OFF）ときは予約を残さない
 */
@Singleton
class ReminderScheduler
@Inject
internal constructor(
    @param:ApplicationContext private val context: Context,
    private val eventRepository: EventRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val planReminders: PlanRemindersUseCase,
    private val clockProvider: Provider<Clock>,
    private val alarmStore: ReminderAlarmStore,
) {
    /** 続けて呼ばれたら順に置き換える（重ねて動かすと、前の置き換えが足したアラームを次が取り消せずに残すため）。 */
    private val mutex = Mutex()

    suspend fun reschedule() {
        mutex.withLock {
            try {
                replaceAlarms()
            } catch (exception: IOException) {
                Log.w(TAG, "通知を予約し直せませんでした", exception)
            }
        }
    }

    private suspend fun replaceAlarms() {
        // 読めなければ（IOException）予約を変えずに終える（空・既定値として読むと、全部取り消す・OFF にした通知を入れてしまう）
        val events = eventRepository.readEvents()
        val settings = userSettingsRepository.readReminderSettings()
        // 端末のタイムゾーンを読み直すため、呼ぶたびに作る
        val clock = clockProvider.get()
        val plan = if (areReminderNotificationsAllowed(context)) {
            planReminders(events, settings, clock.instant(), clock.zone)
        } else {
            emptyList()
        }
        val planned = plan.map { it.identifier }.toSet()
        val previous = alarmStore.identifiers(ReminderAlarmStore.Kind.REMINDER_ALARMS)
        // 途中で止まっても（プロセスが死ぬ・呼んだ scope が取り消される）入れたアラームを後で取り消せるよう、足す前に記録に入れておく
        alarmStore.setIdentifiers(ReminderAlarmStore.Kind.REMINDER_ALARMS, previous + planned)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        (previous - planned).forEach { identifier -> cancelAlarm(alarmManager, identifier) }
        // 同じ識別子の PendingIntent は置き換わるので、計画にあるものはすべて入れ直す
        plan.forEach { reminder -> setExactAlarm(alarmManager, reminder.fireAt.toEpochMilli(), firePendingIntent(reminder)) }
        alarmStore.setIdentifiers(ReminderAlarmStore.Kind.REMINDER_ALARMS, planned)
    }

    private fun cancelAlarm(
        alarmManager: AlarmManager,
        identifier: String,
    ) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            fireIntent(identifier),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun firePendingIntent(reminder: PlannedReminder): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        fireIntent(reminder.identifier)
            .putExtra(EXTRA_EVENT_ID, reminder.eventId.toString())
            .putExtra(EXTRA_TIMING, reminder.timing.name)
            .putExtra(EXTRA_OCCURRENCE_DATE, reminder.occurrenceDate.toString())
            .putExtra(EXTRA_FIRE_AT, reminder.fireAt.toEpochMilli()),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** 識別子を data に入れ、通知ごとに別の PendingIntent にする（extra は PendingIntent の同じさの判定に入らない）。 */
    private fun fireIntent(identifier: String): Intent = Intent(context, ReminderAlarmReceiver::class.java)
        .setAction(ACTION_FIRE_REMINDER)
        .setData(Uri.fromParts(URI_SCHEME, identifier, null))
        .putExtra(EXTRA_IDENTIFIER, identifier)

    private companion object {
        const val TAG = "ReminderScheduler"
        const val URI_SCHEME = "oshikatsu-reminder"
    }
}
