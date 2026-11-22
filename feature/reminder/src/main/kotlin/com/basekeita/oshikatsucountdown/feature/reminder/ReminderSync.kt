package com.basekeita.oshikatsucountdown.feature.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import com.basekeita.oshikatsucountdown.data.definition.repository.EventRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.OshiRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.UserSettingsRepository
import com.basekeita.oshikatsucountdown.domain.reminder.PlanRemindersUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 予定・推し・通知の設定が変わるたびに、予定の通知と当日のカウントダウンを置き直す。これらはアプリの中でしか変わらないので、
 * アプリのプロセスが動いている間だけ見ていればよい。最初の値（プロセスの開始）でも置き直す。
 *
 * - 予定の通知: 予定と通知の設定の変更。推しの名前は通知を出すときに読むので、推しの変更では予約し直さない
 * - 当日のカウントダウン: 予定と推しの変更（中身に推しの名前と推し色が入る）。設定の「予定を通知する」とは別なので、設定では置き直さない
 * - どちらも、日付が変わったとき（`ACTION_DATE_CHANGED`）に置き直す。iOS 版（`significantTimeChange`）と同じく、アプリのプロセスが動いている間だけ受ける
 */
@Singleton
class ReminderSync
@Inject
internal constructor(
    @param:ApplicationContext private val context: Context,
    private val eventRepository: EventRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val oshiRepository: OshiRepository,
    private val reminderScheduler: ReminderScheduler,
    private val liveCountdownScheduler: LiveCountdownScheduler,
) {
    /** プロセスが動いている間ずっと続ける scope（`start` で受け取る）。画面の都合で取り消されないところで置き直す。 */
    private var scope: CoroutineScope? = null

    /** 日付が変わったら置き直す（システムからの知らせなので、exported でなくても届く）。 */
    private val dateChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent,
        ) {
            requestRefreshAll()
        }
    }

    /** アプリのプロセスの開始で呼ぶ。通知のチャンネルを作り、変更を見始める。 */
    fun start(scope: CoroutineScope) {
        this.scope = scope
        createNotificationChannels(context)
        ContextCompat.registerReceiver(
            context,
            dateChangedReceiver,
            IntentFilter(Intent.ACTION_DATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        combine(eventRepository.events, userSettingsRepository.settings.map { it.reminders }) { events, reminders -> events to reminders }
            .distinctUntilChanged()
            .onEach { reminderScheduler.reschedule() }
            // 受けないとアプリのプロセスが落ちる（scope に例外のハンドラが無い）。止まっても、前面に来たときと通知を出したときに予約し直す
            .catch { exception -> Log.w(TAG, "通知の予約し直しを止めます", exception) }
            .launchIn(scope)
        combine(eventRepository.events, oshiRepository.oshis) { events, oshis -> events to oshis }
            .distinctUntilChanged()
            .onEach { liveCountdownScheduler.refresh() }
            .catch { exception -> Log.w(TAG, "当日のカウントダウンの置き直しを止めます", exception) }
            .launchIn(scope)
    }

    /**
     * 予定の通知と当日のカウントダウンを、`start` で受け取った scope（メインスレッドの外）で置き直す。アプリが前面に来たときに呼ぶ
     * （出た通知のぶん空いた枠を埋める・許可を変えて戻ってきたときに反映する）。画面を回して Activity が作り直されても途中で止まらない。
     */
    fun requestRefreshAll() {
        val scope = scope ?: return
        scope.launch { refreshAll() }
    }

    /**
     * 予定の通知と当日のカウントダウンを置き直す。システムからのきっかけ（再起動・更新・時刻・タイムゾーン・正確なアラームの許可）と、
     * 予定の通知を出したときに呼ぶ。
     */
    suspend fun refreshAll() {
        reminderScheduler.reschedule()
        liveCountdownScheduler.refresh()
    }

    private companion object {
        const val TAG = "ReminderSync"
    }
}

/** BroadcastReceiver は Hilt が作らないので、使うものはここから受け取る。 */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface ReminderEntryPoint {
    fun eventRepository(): EventRepository

    fun oshiRepository(): OshiRepository

    fun userSettingsRepository(): UserSettingsRepository

    fun planReminders(): PlanRemindersUseCase

    /** 端末のタイムゾーンを読み直すため、呼ぶたびに作られる。 */
    fun clock(): Clock

    fun liveCountdownScheduler(): LiveCountdownScheduler

    fun reminderSync(): ReminderSync
}
