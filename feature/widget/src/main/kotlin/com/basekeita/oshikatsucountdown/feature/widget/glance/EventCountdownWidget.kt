package com.basekeita.oshikatsucountdown.feature.widget.glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.basekeita.oshikatsucountdown.feature.widget.R
import com.basekeita.oshikatsucountdown.feature.widget.SELECTED_EVENT_ID_KEY
import com.basekeita.oshikatsucountdown.feature.widget.WidgetEntryPoint
import com.basekeita.oshikatsucountdown.feature.widget.scheduleDayChangeRefresh
import com.basekeita.oshikatsucountdown.feature.widget.uimodel.WidgetEventStatusUiModel
import com.basekeita.oshikatsucountdown.feature.widget.uimodel.WidgetEventUiModel
import com.basekeita.oshikatsucountdown.feature.widget.uimodel.findWidgetEvent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import com.basekeita.oshikatsucountdown.ui.R as UiR

/** 残り日数の数字の大きさの上限と下限。 */
private const val MAX_NUMBER_FONT_SIZE = 56f
private const val MIN_NUMBER_FONT_SIZE = 28f

/** 太字の数字 1 桁の幅（文字の大きさに対する割合）。桁数から数字の大きさを決めるのに使う。 */
private const val DIGIT_WIDTH_RATIO = 0.62f

/** 数字の両隣の「あと」「日」と間の幅。 */
private val countdownLabelsWidth = 48.dp

private val contentPadding = 16.dp

/** これより低いウィジェットではタイトルを 1 行にする。 */
private val twoLineTitleMinHeight = 150.dp

/**
 * 選んだ予定 1 件のウィジェット。推し色の塗りに白い文字で、推しの名前・タイトル・「あと N 日」を出す。
 * 押すとアプリで予定の詳細を開く。予定が無くなったら、押して選び直せるようにする。
 */
class EventCountdownWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

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
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        scheduleDayChangeRefresh(context, clock)

        provideContent {
            val events by eventRepository.events.collectAsState(initialEvents)
            val oshis by oshiRepository.oshis.collectAsState(initialOshis)
            // 描き直しの要求（update / updateAll）が来ると状態が読み直されてここから組み直されるので、日付もそのたびに読む
            val selectedEventId = currentState(SELECTED_EVENT_ID_KEY)
            val event = buildEventSchedule(events, LocalDate.now(clock)).findWidgetEvent(selectedEventId, oshis)
            if (event == null) {
                EventNotFoundContent(onClick = actionStartActivity(configureIntent(context, appWidgetId)))
            } else {
                EventCountdownContent(event = event, onClick = actionStartActivity(openEventIntent(context, event.id)))
            }
        }
    }
}

class EventCountdownWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EventCountdownWidget()
}

@Composable
private fun EventCountdownContent(
    event: WidgetEventUiModel,
    onClick: Action,
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val onColor = event.color.onFillColor()
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .widgetBackground()
            .background(event.color.fillColor())
            .padding(contentPadding)
            .clickable(onClick),
    ) {
        Text(
            text = event.oshiName ?: context.getString(event.type.labelRes),
            style = TextStyle(color = onColor, fontSize = 13.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        Text(
            text = event.title,
            style = TextStyle(color = onColor, fontSize = 12.sp),
            maxLines = if (size.height < twoLineTitleMinHeight) {
                1
            } else {
                2
            },
        )
        CountdownLine(event = event, color = onColor, widgetWidth = size.width)
    }
}

/** 「あと 11 日」。当日は「今日！」、終わった予定は「3 日前」。 */
@Composable
private fun CountdownLine(
    event: WidgetEventUiModel,
    color: ColorProvider,
    widgetWidth: Dp,
) {
    val context = LocalContext.current
    when (event.status) {
        WidgetEventStatusUiModel.TODAY -> {
            Text(
                text = context.getString(UiR.string.countdown_today),
                style = TextStyle(color = color, fontSize = 32.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
        }

        WidgetEventStatusUiModel.ENDED -> {
            val daysAgo = (-event.daysUntil).toInt()
            Text(
                text = context.resources.getQuantityString(UiR.plurals.countdown_days_ago, daysAgo, daysAgo),
                style = TextStyle(color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
        }

        WidgetEventStatusUiModel.UPCOMING -> {
            val daysText = event.daysUntil.toString()
            val labelStyle = TextStyle(color = color, fontSize = 13.sp)
            // 下端でそろえると小さい文字のほうが下がるので、数字の下の余白の分だけ持ち上げる
            val labelModifier = GlanceModifier.padding(bottom = 8.dp)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = context.getString(UiR.string.countdown_prefix), style = labelStyle, modifier = labelModifier)
                Spacer(modifier = GlanceModifier.width(3.dp))
                Text(
                    text = daysText,
                    style = TextStyle(
                        color = color,
                        fontSize = numberFontSize(widgetWidth = widgetWidth, digits = daysText.length),
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
                Spacer(modifier = GlanceModifier.width(3.dp))
                Text(
                    text = context.getString(UiR.string.countdown_day_unit),
                    style = labelStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    modifier = labelModifier,
                )
            }
        }
    }
}

/** 数字の大きさ。桁が多い・ウィジェットが狭いときは、1 行に収まるよう小さくする。 */
private fun numberFontSize(
    widgetWidth: Dp,
    digits: Int,
): TextUnit {
    val numberWidth = widgetWidth - contentPadding * 2 - countdownLabelsWidth
    return (numberWidth.value / (digits * DIGIT_WIDTH_RATIO)).coerceIn(MIN_NUMBER_FONT_SIZE, MAX_NUMBER_FONT_SIZE).sp
}

/** 選んだ予定が無い（削除された）。押すと予定を選び直す画面を開く。 */
@Composable
private fun EventNotFoundContent(onClick: Action) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .widgetBackground()
            .background(WidgetColors.surface)
            .padding(contentPadding)
            .clickable(onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = context.getString(R.string.widget_event_not_found),
            style = TextStyle(color = WidgetColors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold),
        )
        Text(
            text = context.getString(R.string.widget_event_choose_again),
            style = TextStyle(color = WidgetColors.onSurfaceVariant, fontSize = 12.sp),
        )
    }
}
