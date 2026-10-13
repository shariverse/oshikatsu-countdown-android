package com.basekeita.oshikatsucountdown.feature.home.ui.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.basekeita.oshikatsucountdown.feature.home.R
import com.basekeita.oshikatsucountdown.feature.home.uimodel.EventStatusUiModel
import com.basekeita.oshikatsucountdown.feature.home.uimodel.HomeEventUiModel
import com.basekeita.oshikatsucountdown.ui.rule.formatDateBadge
import com.basekeita.oshikatsucountdown.ui.rule.formatTime
import com.basekeita.oshikatsucountdown.ui.theme.OshikatsuTheme
import com.basekeita.oshikatsucountdown.ui.theme.OshikatsuTypography
import com.basekeita.oshikatsucountdown.ui.uimodel.EventTypeUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel
import java.time.LocalDate
import java.time.LocalTime

/** 先頭の予定のカード。推し色の塗りに白い文字で、残り日数を大きく出す。 */
@Composable
internal fun FeaturedEventCard(
    event: HomeEventUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onColor = event.color.onColor
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = event.color.color, contentColor = onColor),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = onColor.copy(alpha = 0.22f),
                    contentColor = onColor,
                ) {
                    Text(
                        text = if (event.pinned) {
                            stringResource(R.string.home_featured_pinned)
                        } else {
                            stringResource(R.string.home_featured_nearest)
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                if (event.oshiName != null) {
                    Text(text = event.oshiName, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                }
            }
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
            Row(verticalAlignment = Alignment.Bottom) {
                DaysCountdown(
                    daysUntil = event.daysUntil,
                    status = event.status,
                    color = onColor,
                    mutedColor = onColor,
                    numberStyle = OshikatsuTypography.countdownLarge,
                    unitStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    showPrefix = true,
                    modifier = Modifier.weight(1f),
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatDateBadge(event.occurrenceDate),
                        style = OshikatsuTypography.dateBadge,
                    )
                    if (event.startTime != null) {
                        Text(
                            text = stringResource(
                                R.string.home_start_time,
                                stringResource(event.type.startLabelRes),
                                formatTime(event.startTime),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }
    }
}

@Preview(widthDp = 380)
@Composable
private fun FeaturedEventCardPreview() {
    OshikatsuTheme {
        FeaturedEventCard(
            event = HomeEventUiModel(
                id = "1",
                title = "LUMINA 5th LIVE 東京公演",
                type = EventTypeUiModel.LIVE,
                oshiName = "LUMINA",
                initial = "L",
                color = OshiColorUiModel.PINK,
                photoPath = null,
                occurrenceDate = LocalDate.of(2026, 10, 18),
                startTime = LocalTime.of(17, 0),
                repeatsYearly = false,
                pinned = true,
                daysUntil = 11,
                status = EventStatusUiModel.UPCOMING,
            ),
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
