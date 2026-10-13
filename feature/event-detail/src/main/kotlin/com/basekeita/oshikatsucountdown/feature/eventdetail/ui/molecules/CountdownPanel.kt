package com.basekeita.oshikatsucountdown.feature.eventdetail.ui.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.basekeita.oshikatsucountdown.feature.eventdetail.R
import com.basekeita.oshikatsucountdown.feature.eventdetail.uimodel.CountdownStatusUiModel
import com.basekeita.oshikatsucountdown.feature.eventdetail.uimodel.CountdownUiModel
import com.basekeita.oshikatsucountdown.ui.rule.formatShortDate
import com.basekeita.oshikatsucountdown.ui.theme.OshikatsuTheme
import com.basekeita.oshikatsucountdown.ui.theme.OshikatsuTypography
import com.basekeita.oshikatsucountdown.ui.theme.onSurfaceColor
import com.basekeita.oshikatsucountdown.ui.uimodel.EventTypeUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt
import com.basekeita.oshikatsucountdown.ui.R as UiR

/**
 * 開始までの残り（日と時:分:秒）と、登録から当日までの進み具合。
 * 秒は毎秒変わるので読み上げは分までにまとめ、変わるたびには読み上げない。
 */
@Composable
internal fun CountdownPanel(
    countdown: CountdownUiModel,
    type: EventTypeUiModel,
    hasStartTime: Boolean,
    occurrenceDate: LocalDate,
    color: OshiColorUiModel,
    modifier: Modifier = Modifier,
) {
    val accent = color.onSurfaceColor()
    val muted = OshikatsuTheme.colorScheme.onSurfaceVariant
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (countdown.status) {
            CountdownStatusUiModel.UPCOMING -> {
                Text(
                    text = if (hasStartTime) {
                        stringResource(R.string.detail_until_start, stringResource(type.startLabelRes))
                    } else {
                        stringResource(R.string.detail_until_day)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = muted,
                )
                val remainingDescription = stringResource(
                    R.string.detail_remaining_description,
                    countdown.days,
                    countdown.hours,
                    countdown.minutes,
                )
                Row(
                    modifier = Modifier.clearAndSetSemantics { contentDescription = remainingDescription },
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = countdown.days.toString(),
                        style = OshikatsuTypography.countdownHero,
                        color = accent,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Text(
                        text = stringResource(UiR.string.countdown_day_unit),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Text(
                        text = String.format(Locale.ROOT, "%02d:%02d:%02d", countdown.hours, countdown.minutes, countdown.seconds),
                        style = OshikatsuTypography.countdownClock,
                        color = OshikatsuTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .alignByBaseline()
                            .padding(start = 6.dp),
                    )
                }
            }

            CountdownStatusUiModel.TODAY -> {
                Text(
                    text = stringResource(R.string.detail_today_title),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = accent,
                )
                Text(
                    text = stringResource(R.string.detail_today_message),
                    style = MaterialTheme.typography.bodyLarge,
                    color = muted,
                )
            }

            CountdownStatusUiModel.ENDED -> {
                Text(
                    text = stringResource(R.string.detail_ended_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = muted,
                )
                val daysAgo = -countdown.daysUntil
                Text(
                    text = pluralStringResource(UiR.plurals.countdown_days_ago, daysAgo.toInt(), daysAgo),
                    style = MaterialTheme.typography.bodyLarge,
                    color = muted,
                )
            }
        }

        val progress = countdown.progress
        if (progress != null) {
            ProgressBar(
                progress = progress,
                startDate = countdown.progressStartDate,
                endDate = occurrenceDate,
                color = color,
            )
        }
    }
}

@Composable
private fun ProgressBar(
    progress: Float,
    startDate: LocalDate,
    endDate: LocalDate,
    color: OshiColorUiModel,
) {
    val muted = OshikatsuTheme.colorScheme.onSurfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color.onSurfaceColor(),
            trackColor = OshikatsuTheme.colorScheme.surfaceContainerHigh,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.detail_progress_start, formatShortDate(startDate)),
                style = MaterialTheme.typography.labelMedium,
                color = muted,
            )
            Text(
                text = stringResource(R.string.detail_progress_percent, (progress * 100).roundToInt()),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = muted,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.detail_progress_end, formatShortDate(endDate)),
                style = MaterialTheme.typography.labelMedium,
                color = muted,
            )
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun CountdownPanelPreview() {
    OshikatsuTheme {
        CountdownPanel(
            countdown = CountdownUiModel(
                status = CountdownStatusUiModel.UPCOMING,
                days = 11,
                hours = 6,
                minutes = 42,
                seconds = 18,
                daysUntil = 11,
                progress = 0.76f,
                progressStartDate = LocalDate.of(2026, 9, 2),
            ),
            type = EventTypeUiModel.LIVE,
            hasStartTime = true,
            occurrenceDate = LocalDate.of(2026, 10, 18),
            color = OshiColorUiModel.PINK,
            modifier = Modifier.padding(20.dp),
        )
    }
}
