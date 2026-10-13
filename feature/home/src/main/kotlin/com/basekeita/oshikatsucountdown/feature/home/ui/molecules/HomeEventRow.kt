package com.basekeita.oshikatsucountdown.feature.home.ui.molecules

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.basekeita.oshikatsucountdown.feature.home.R
import com.basekeita.oshikatsucountdown.feature.home.uimodel.EventStatusUiModel
import com.basekeita.oshikatsucountdown.feature.home.uimodel.HomeEventUiModel
import com.basekeita.oshikatsucountdown.ui.component.atoms.OshiAvatar
import com.basekeita.oshikatsucountdown.ui.rule.formatMonthDayWeekday
import com.basekeita.oshikatsucountdown.ui.theme.OshikatsuTheme
import com.basekeita.oshikatsucountdown.ui.theme.OshikatsuTypography
import com.basekeita.oshikatsucountdown.ui.theme.onSurfaceColor
import com.basekeita.oshikatsucountdown.ui.uimodel.EventTypeUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel
import java.time.LocalDate
import com.basekeita.oshikatsucountdown.ui.R as UiR

/** 一覧の予定 1 行（M3 の `ListItem`）。補足は日付と推しの名前、毎年くり返す予定は「毎年」。 */
@Composable
internal fun HomeEventRow(
    event: HomeEventUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = OshikatsuTheme.colorScheme
    val detail = if (event.repeatsYearly) {
        stringResource(UiR.string.event_repeats_yearly)
    } else {
        event.oshiName ?: stringResource(UiR.string.event_no_oshi)
    }
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            OshiAvatar(initial = event.initial, color = event.color, photoPath = event.photoPath)
        },
        headlineContent = {
            Text(text = event.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                text = stringResource(R.string.home_row_supporting, formatMonthDayWeekday(event.occurrenceDate), detail),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            DaysCountdown(
                daysUntil = event.daysUntil,
                status = event.status,
                color = event.color.onSurfaceColor(),
                mutedColor = colorScheme.onSurfaceVariant,
                numberStyle = OshikatsuTypography.countdownSmall,
                unitStyle = MaterialTheme.typography.labelMedium,
            )
        },
    )
}

@Preview(widthDp = 380)
@Composable
private fun HomeEventRowPreview() {
    OshikatsuTheme {
        HomeEventRow(
            event = HomeEventUiModel(
                id = "1",
                title = "1st写真集 発売日",
                type = EventTypeUiModel.RELEASE,
                oshiName = "ミナト",
                initial = "ミ",
                color = OshiColorUiModel.BLUE,
                photoPath = null,
                occurrenceDate = LocalDate.of(2026, 10, 10),
                startTime = null,
                repeatsYearly = false,
                pinned = false,
                daysUntil = 3,
                status = EventStatusUiModel.UPCOMING,
            ),
            onClick = {},
        )
    }
}
