package com.basekeita.oshikatsucountdown.feature.home.ui.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.basekeita.oshikatsucountdown.feature.home.uimodel.EventStatusUiModel
import com.basekeita.oshikatsucountdown.ui.R as UiR

/**
 * 残り日数（`11 日`）。当日は「今日！」、終わった予定は「3 日前」。
 *
 * @param numberStyle 数字の文字。単位は `unitStyle`。
 * @param showPrefix 数字の前に「あと」を置く。
 */
@Composable
internal fun DaysCountdown(
    daysUntil: Long,
    status: EventStatusUiModel,
    color: Color,
    mutedColor: Color,
    numberStyle: TextStyle,
    unitStyle: TextStyle,
    modifier: Modifier = Modifier,
    showPrefix: Boolean = false,
) {
    when (status) {
        EventStatusUiModel.TODAY -> {
            Text(
                text = stringResource(UiR.string.countdown_today),
                style = unitStyle.copy(fontWeight = FontWeight.Bold),
                color = color,
                modifier = modifier,
            )
        }

        EventStatusUiModel.ENDED -> {
            val daysAgo = -daysUntil
            Text(
                text = pluralStringResource(UiR.plurals.countdown_days_ago, daysAgo.toInt(), daysAgo),
                style = MaterialTheme.typography.bodyMedium,
                color = mutedColor,
                modifier = modifier,
            )
        }

        EventStatusUiModel.UPCOMING -> {
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (showPrefix) {
                    Text(
                        text = stringResource(UiR.string.countdown_prefix),
                        style = unitStyle,
                        color = color,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
                Text(
                    text = daysUntil.toString(),
                    style = numberStyle,
                    color = color,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    text = stringResource(UiR.string.countdown_day_unit),
                    style = unitStyle,
                    color = if (showPrefix) {
                        color
                    } else {
                        mutedColor
                    },
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
    }
}
