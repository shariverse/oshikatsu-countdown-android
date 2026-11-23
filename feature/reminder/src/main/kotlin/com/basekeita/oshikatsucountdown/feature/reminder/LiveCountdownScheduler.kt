package com.basekeita.oshikatsucountdown.feature.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import com.basekeita.oshikatsucountdown.data.definition.repository.EventRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.OshiRepository
import com.basekeita.oshikatsucountdown.domain.reminder.LiveCountdownPlan
import com.basekeita.oshikatsucountdown.domain.reminder.PlanLiveCountdownsUseCase
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

private const val ACTION_COUNTDOWN_ALARM = "com.basekeita.oshikatsucountdown.feature.reminder.action.COUNTDOWN_ALARM"

/**
 * 当日のカウントダウン（iOS 版の当日のライブアクティビティにあたる）を、計画（`PlanLiveCountdownsUseCase`）のとおりに出す・予約する・消す。
 *
 * - 時間帯に入っている予定は、すぐに出す（出し続ける通知。残りは通知の時計で端末が数える）
 * - これから時間帯が始まる予定は、始まる時刻に正確なアラームを入れ、そのときに知らせて出す（iOS 26 以降の予約と同じ。`countdownsToAlert`）
 * - 出しているものには、開演（中身を「今日は当日！」に変える）と開演の 1 時間後（消す）のアラームを入れる（`countdownAlarmsOf`）
 * - 計画に無くなったもの（削除した・時刻を変えた・時間帯を過ぎた予定）は消し、中身が変わったものは出し直す
 * - 通知を出せない（許可が無い・正確なアラームを使えない）ときは、何も出さず予約も残さない
 *
 * 予定の通知と同じきっかけで置き直す（`ReminderSync`）。
 */
@Singleton
class LiveCountdownScheduler @Inject internal constructor(
    @param:ApplicationContext private val context: Context,
    private val eventRepository: EventRepository,
    private val oshiRepository: OshiRepository,
    private val planLiveCountdowns: PlanLiveCountdownsUseCase,
    private val clockProvider: Provider<Clock>,
    private val alarmStore: ReminderAlarmStore,
) {
    /** 続けて呼ばれたら順に置き直す。 */
    private val mutex = Mutex()

    /** 計画を作り直し、出しているもの・予約したものを置き直す。 */
    suspend fun refresh() {
        mutex.withLock {
            try {
                replace()
            } catch (exception: IOException) {
                Log.w(TAG, "当日のカウントダウンを置き直せませんでした", exception)
            }
        }
    }

    /**
     * 途中で止まっても（プロセスが死ぬ・呼んだ scope が取り消される）、入れたアラームと出した通知を後で取り消せるよう、
     * 足す前に「前の記録と今回のもの」を記録に書き、終えてから今回のものだけにする。
     */
    private suspend fun replace() {
        // 読めなければ（IOException）置き直さずに終える（空として読むと、出しているものを全部消してしまう）
        val events = eventRepository.readEvents()
        val oshis = oshiRepository.readOshis()
        // 端末のタイムゾーンを読み直すため、呼ぶたびに作る
        val clock = clockProvider.get()
        val now = clock.instant()
        val plan = if (areReminderNotificationsAllowed(context)) {
            planLiveCountdowns(events, now, clock.zone)
        } else {
            LiveCountdownPlan(current = emptyList(), scheduled = emptyList())
        }

        val previousAlarmKeys = alarmStore.identifiers(ReminderAlarmStore.Kind.COUNTDOWN_ALARMS)
        val previousShown = alarmStore.identifiers(ReminderAlarmStore.Kind.COUNTDOWN_NOTIFICATIONS)
        val alerts = countdownsToAlert(plan.current, previousAlarmKeys, previousShown)
        val alarms = countdownAlarmsOf(plan, now)

        val wording = ResourceReminderWording(context)
        val contents = plan.current.mapNotNull { window ->
            val event = events.firstOrNull { it.id == window.eventId } ?: return@mapNotNull null
            val oshi = event.oshiId?.let { oshiId -> oshis.firstOrNull { it.id == oshiId } }
            window.identifier to liveCountdownContentOf(window, event, oshi, wording)
        }
        val notificationRecords = recordReplacement(previous = previousShown, planned = contents.map { (identifier, _) -> identifier }.toSet())
        val alarmRecords = recordReplacement(previous = previousAlarmKeys, planned = alarms.keys)
        alarmStore.setIdentifiers(ReminderAlarmStore.Kind.COUNTDOWN_NOTIFICATIONS, notificationRecords.writeAhead)
        alarmStore.setIdentifiers(ReminderAlarmStore.Kind.COUNTDOWN_ALARMS, alarmRecords.writeAhead)

        notificationRecords.toCancel.forEach { cancelWithTag(context, it) }
        contents.forEach { (identifier, content) -> postCountdown(identifier, content, now, alert = identifier in alerts) }

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmRecords.toCancel.forEach { key -> cancelAlarm(alarmManager, key) }
        alarms.forEach { (key, triggerAt) -> setExactAlarm(alarmManager, triggerAt.toEpochMilli(), alarmPendingIntent(key)) }

        alarmStore.setIdentifiers(ReminderAlarmStore.Kind.COUNTDOWN_NOTIFICATIONS, notificationRecords.final)
        alarmStore.setIdentifiers(ReminderAlarmStore.Kind.COUNTDOWN_ALARMS, alarmRecords.final)
    }

    /**
     * 出し続ける通知で出す。同じ識別子なら置き換わる（中身が変わったら出し直しになる）。
     * 知らせるのは始めるときの 1 回だけ（置き直しでは鳴らさない）。開演の 1 時間後に消えるよう、時間切れも付けておく。
     *
     * iOS 版は「開演まで」のすぐ下に残りを出し、組にして読ませる。標準の通知では、残り（時計）は見出しの行の末尾に出て、
     * そのすぐ前がサブテキストになる（`ライブ · 開演まで · 2:59:17`）。そのため「開演まで」はサブテキストに入れ、
     * 推しの名前と開始の時刻は本文に出す（`テイルズ · 開演 17:00`）。開演を過ぎたら、サブテキストを「今日は当日！」にして時計を消す。
     */
    private fun postCountdown(
        identifier: String,
        content: LiveCountdownContent,
        now: Instant,
        alert: Boolean,
    ) {
        val started = content.isStarted(now)
        val subText = if (started) {
            content.startedText
        } else {
            content.untilText
        }
        val builder = NotificationCompat.Builder(context, COUNTDOWN_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(content.title)
            .setSubText(subText)
            .setContentText(context.getString(R.string.countdown_body, content.heading, content.startText))
            .setColor(content.color.color.toArgb())
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(!alert)
            .setTimeoutAfter((content.end.toEpochMilli() - now.toEpochMilli()).coerceAtLeast(0))
            .setContentIntent(openEventPendingIntent(context, identifier, content.eventId))
        if (started) {
            builder.setShowWhen(false)
        } else {
            // 開演までの残りを、端末が 1 秒ごとに数える
            builder
                .setWhen(content.start.toEpochMilli())
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
        }
        notifyWithTag(context, identifier, builder.build())
    }

    private fun cancelAlarm(
        alarmManager: AlarmManager,
        key: String,
    ) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            alarmIntent(key),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun alarmPendingIntent(key: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        alarmIntent(key),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /**
     * キーを data に入れ、アラームごとに別の PendingIntent にする。どのアラームでも計画を作り直して置き直すだけなので、extra は持たない
     * （記録したキーを読み直さないので、版を上げて時点の名前を変えても取り消せる）。
     */
    private fun alarmIntent(key: String): Intent = Intent(context, LiveCountdownAlarmReceiver::class.java)
        .setAction(ACTION_COUNTDOWN_ALARM)
        .setData(Uri.fromParts(URI_SCHEME, key, null))

    private companion object {
        const val TAG = "LiveCountdownScheduler"
        const val URI_SCHEME = "oshikatsu-countdown"
    }
}

/**
 * 当日のカウントダウンのアラーム（出し始める・開演・終える）で置き直す。出し始める時刻なら、そのカウントダウンを知らせて出す
 * （知らせるかは置き直しの中で記録から決める。`countdownsToAlert`）。
 */
class LiveCountdownAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACTION_COUNTDOWN_ALARM) return

        val scheduler = EntryPointAccessors.fromApplication<ReminderEntryPoint>(context).liveCountdownScheduler()
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                scheduler.refresh()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
