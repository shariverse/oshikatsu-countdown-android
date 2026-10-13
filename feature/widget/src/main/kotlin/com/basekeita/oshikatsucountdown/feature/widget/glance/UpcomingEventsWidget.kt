package com.basekeita.oshikatsucountdown.feature.widget.glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.basekeita.oshikatsucountdown.feature.widget.R
import com.basekeita.oshikatsucountdown.feature.widget.WidgetEntryPoint
import com.basekeita.oshikatsucountdown.feature.widget.scheduleDayChangeRefresh
import com.basekeita.oshikatsucountdown.feature.widget.uimodel.WidgetEventStatusUiModel
import com.basekeita.oshikatsucountdown.feature.widget.uimodel.WidgetEventUiModel
import com.basekeita.oshikatsucountdown.feature.widget.uimodel.upcomingWidgetEvents
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import com.basekeita.oshikatsucountdown.ui.R as UiR

/** 押せる行・ボタンの大きさ（M3 の押せる大きさ）。 */
private val touchTargetSize = 48.dp

/** ＋ の丸の見た目の大きさ。押せる範囲は `touchTargetSize`。 */
private val addButtonSize = 32.dp

/**
 * ウィジェットの端から中身（推し色の丸・見出し・残り日数・＋ の丸）までの余白。
 * ＋ は押せる範囲が見た目より外に広がるので、外枠の余白は右上だけ狭め、その分を中身の側で空ける。
 */
private val contentInset = 16.dp
private val touchTargetOverhang = (touchTargetSize - addButtonSize) / 2

/**
 * 「推しカウント」。当日とこれからの予定を近い順に、推し色の印・タイトル・残り日数で並べる。
 * 行を押すとアプリで予定の詳細、右上の ＋ で予定の追加を開く。
 */
class UpcomingEventsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val entryPoint = EntryPointAccessors.fromApplication<WidgetEntryPoint>(context)
        val eventRepository = entryPoint.eventRepository()
        val oshiRepository = entryPoint.oshiRepository()
        val buildEventSchedule = entryPoint.buildEventSchedule()
        val clock = entryPoint.clock()
        val initialEvents = eventRepository.events.first()
        val initialOshis = oshiRepository.oshis.first()
        scheduleDayChangeRefresh(context, clock)

        provideContent {
            val events by eventRepository.events.collectAsState(initialEvents)
            val oshis by oshiRepository.oshis.collectAsState(initialOshis)
            // 状態を読んでおくと、描き直しの要求（update / updateAll）のたびにここから組み直され、日付も読み直す
            currentState<Preferences>()
            val upcoming = buildEventSchedule(events, LocalDate.now(clock)).upcomingWidgetEvents(oshis)
            UpcomingEventsContent(
                events = upcoming,
                onTitleClick = actionStartActivity(openAppIntent(context)),
                onAddClick = actionStartActivity(addEventIntent(context)),
                eventClickAction = { eventId -> actionStartActivity(openEventIntent(context, eventId)) },
            )
        }
    }
}

class UpcomingEventsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UpcomingEventsWidget()
}

@Composable
private fun UpcomingEventsContent(
    events: List<WidgetEventUiModel>,
    onTitleClick: Action,
    onAddClick: Action,
    eventClickAction: (eventId: String) -> Action,
) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .widgetBackground()
            .background(WidgetColors.surface)
            .padding(
                start = contentInset,
                end = contentInset - touchTargetOverhang,
                top = contentInset - touchTargetOverhang,
                bottom = contentInset - touchTargetOverhang,
            ),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = context.getString(R.string.widget_upcoming_title),
                style = TextStyle(color = WidgetColors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight().clickable(onTitleClick),
            )
            AddEventButton(onClick = onAddClick)
        }
        if (events.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize().padding(end = touchTargetOverhang),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = context.getString(R.string.widget_upcoming_empty),
                    style = TextStyle(color = WidgetColors.onSurfaceVariant, fontSize = 13.sp),
                )
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(events) { event ->
                    UpcomingEventRow(event = event, onClick = eventClickAction(event.id))
                }
            }
        }
    }
}

/** 予定の追加。見た目はデザインの 32dp の丸、押せる範囲は 48dp。 */
@Composable
private fun AddEventButton(onClick: Action) {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier.size(touchTargetSize).clickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = GlanceModifier
                .size(addButtonSize)
                .background(ImageProvider(R.drawable.widget_circle), colorFilter = ColorFilter.tint(WidgetColors.brandContainer)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(UiR.drawable.ic_add),
                contentDescription = context.getString(R.string.widget_add_event),
                colorFilter = ColorFilter.tint(WidgetColors.onBrandContainer),
                modifier = GlanceModifier.size(18.dp),
            )
        }
    }
}

/** 予定 1 行。推し色の丸・タイトル・残り日数（推し色）。丸も文字と同じく、ダークでは白に寄せた推し色にする。 */
@Composable
private fun UpcomingEventRow(
    event: WidgetEventUiModel,
    onClick: Action,
) {
    val context = LocalContext.current
    val accent = event.color.onSurfaceTextColor()
    Row(
        modifier = GlanceModifier.fillMaxWidth().height(touchTargetSize).padding(end = touchTargetOverhang).clickable(onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(R.drawable.widget_circle),
            contentDescription = null,
            colorFilter = ColorFilter.tint(accent),
            modifier = GlanceModifier.size(10.dp),
        )
        Spacer(modifier = GlanceModifier.width(10.dp))
        Text(
            text = event.title,
            style = TextStyle(color = WidgetColors.onSurface, fontSize = 14.sp),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            text = if (event.status == WidgetEventStatusUiModel.TODAY) {
                context.getString(UiR.string.countdown_today)
            } else {
                context.getString(R.string.widget_days, event.daysUntil)
            },
            style = TextStyle(color = accent, fontSize = 18.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
    }
}
