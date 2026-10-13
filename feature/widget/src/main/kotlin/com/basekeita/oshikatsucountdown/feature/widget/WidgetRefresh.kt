package com.basekeita.oshikatsucountdown.feature.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.basekeita.oshikatsucountdown.data.definition.repository.EventRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.OshiRepository
import com.basekeita.oshikatsucountdown.feature.widget.glance.EventCountdownWidget
import com.basekeita.oshikatsucountdown.feature.widget.glance.UpcomingEventsWidget
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * 日付が変わってからウィジェットを描き直すまでの許容の幅。正確なアラームの権限を持たないアプリが
 * `setWindow` に渡せる幅の下限（10 分）で、0:00 から 10 分以内に数え直す。
 */
private const val DAY_CHANGE_WINDOW_MILLIS = 10 * 60 * 1000L

private const val ACTION_DAY_CHANGED = "com.basekeita.oshikatsucountdown.feature.widget.action.DAY_CHANGED"

/** 0:00 の予約と、端末の時刻（`TIME_SET`）・タイムゾーンの変更。 */
private val dayChangeActions = setOf(ACTION_DAY_CHANGED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)

/** ウィジェットをすべて描き直す。置かれていないウィジェットは何もしない。 */
internal suspend fun refreshAllWidgets(context: Context) {
    EventCountdownWidget().updateAll(context)
    UpcomingEventsWidget().updateAll(context)
}

/**
 * 次の 0:00（端末のタイムゾーン）に描き直す。端末が眠っている間は起こさず、次に起きたときに描き直す
 * （ウィジェットが見えるのは画面が点いているときだけなので）。同じ PendingIntent で前の予約を置き換える。
 */
internal fun scheduleDayChangeRefresh(
    context: Context,
    clock: Clock,
) {
    val nextMidnight = LocalDate.now(clock).plusDays(1).atStartOfDay(clock.zone).toInstant().toEpochMilli()
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, WidgetRefreshReceiver::class.java).setAction(ACTION_DAY_CHANGED),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    context.getSystemService(AlarmManager::class.java)
        .setWindow(AlarmManager.RTC, nextMidnight, DAY_CHANGE_WINDOW_MILLIS, pendingIntent)
}

/** 日付が変わった（0:00 の予約、端末の時刻・タイムゾーンの変更）ときにウィジェットを数え直す。 */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action !in dayChangeActions) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                refreshAllWidgets(context)
                // 描き直しが動いているセッションに届いたときは provideGlance を通らないので、次の予約はここでも入れる
                if (hasAnyWidget(context)) {
                    val clock = EntryPointAccessors.fromApplication<WidgetEntryPoint>(context).clock()
                    scheduleDayChangeRefresh(context, clock)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun hasAnyWidget(context: Context): Boolean {
        val manager = GlanceAppWidgetManager(context)
        return manager.getGlanceIds(EventCountdownWidget::class.java).isNotEmpty() ||
            manager.getGlanceIds(UpcomingEventsWidget::class.java).isNotEmpty()
    }
}

/**
 * 推しと予定が変わるたびにウィジェットを描き直す。推しと予定はアプリの中でしか変わらないので、
 * アプリのプロセスが動いている間だけ見ていればよい。
 */
class WidgetDataSync
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
    private val eventRepository: EventRepository,
    private val oshiRepository: OshiRepository,
) {
    fun start(scope: CoroutineScope) {
        combine(eventRepository.events, oshiRepository.oshis) { events, oshis -> events to oshis }
            .distinctUntilChanged()
            // 最初の値は描いてあるウィジェットと同じなので描き直さない
            .drop(1)
            .onEach { refreshAllWidgets(context) }
            .launchIn(scope)
    }
}
