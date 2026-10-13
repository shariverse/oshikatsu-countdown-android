package com.basekeita.oshikatsucountdown.feature.widget

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.basekeita.oshikatsucountdown.feature.widget.WidgetConfigureViewModel.UiState
import com.basekeita.oshikatsucountdown.feature.widget.WidgetConfigureViewModel.ViewState
import com.basekeita.oshikatsucountdown.feature.widget.uimodel.WidgetEventStatusUiModel
import com.basekeita.oshikatsucountdown.feature.widget.uimodel.WidgetEventUiModel
import com.basekeita.oshikatsucountdown.ui.component.atoms.OshiAvatar
import com.basekeita.oshikatsucountdown.ui.component.molecules.WriteFailureSnackbar
import com.basekeita.oshikatsucountdown.ui.rule.formatMonthDayWeekday
import com.basekeita.oshikatsucountdown.ui.theme.OshikatsuTheme
import com.basekeita.oshikatsucountdown.ui.theme.onSurfaceColor
import com.basekeita.oshikatsucountdown.ui.uimodel.EventTypeUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel
import java.time.LocalDate
import com.basekeita.oshikatsucountdown.ui.R as UiR

/** 中身の幅の上限。タブレットで横に伸びすぎないようにする。 */
private val contentMaxWidth = 640.dp

/**
 * 1 件のウィジェットに出す予定を選ぶ画面の Route。選んで保存し終えたら `onFinish`、選ばずに閉じたら `onClose`。
 */
@Composable
internal fun WidgetConfigureScreen(
    appWidgetId: Int,
    onFinish: () -> Unit,
    onClose: () -> Unit,
    viewModel: WidgetConfigureViewModel = hiltViewModel<WidgetConfigureViewModel, WidgetConfigureViewModel.Factory>(
        creationCallback = { factory -> factory.create(appWidgetId) },
    ),
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    val isFinished = (viewState as? ViewState.Success)?.uiState?.isFinished == true
    LaunchedEffect(isFinished) {
        if (isFinished) onFinish()
    }

    WidgetConfigureContent(
        viewState = viewState,
        onClose = onClose,
        onEventSelect = viewModel::onEventSelect,
        onWriteFailureShown = viewModel::onWriteFailureShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigureContent(
    viewState: ViewState,
    onClose: () -> Unit,
    onEventSelect: (String) -> Unit,
    onWriteFailureShown: () -> Unit,
) {
    val uiState = (viewState as? ViewState.Success)?.uiState
    val canClose = uiState?.canClose ?: true

    // 保存中に閉じると viewModelScope ごと書き込みが止まるため、戻る操作を受けて何もしない
    BackHandler(enabled = !canClose) {}

    val snackbarHostState = remember { SnackbarHostState() }
    WriteFailureSnackbar(
        failure = uiState?.writeFailure,
        snackbarHostState = snackbarHostState,
        onShown = onWriteFailureShown,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.configure_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose, enabled = canClose) {
                        Icon(painter = painterResource(UiR.drawable.ic_close), contentDescription = stringResource(UiR.string.close))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            when (viewState) {
                ViewState.Loading -> Unit

                is ViewState.Error -> {
                    Text(
                        text = stringResource(UiR.string.load_error),
                        style = MaterialTheme.typography.bodyLarge,
                        color = OshikatsuTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                }

                is ViewState.Success -> {
                    EventChoices(uiState = viewState.uiState, onEventSelect = onEventSelect)
                }
            }
        }
    }
}

@Composable
private fun EventChoices(
    uiState: UiState,
    onEventSelect: (String) -> Unit,
) {
    if (uiState.events.isEmpty()) {
        Text(
            text = stringResource(R.string.configure_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = OshikatsuTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp),
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .widthIn(max = contentMaxWidth)
            .fillMaxSize(),
    ) {
        items(items = uiState.events, key = { event -> event.id }) { event ->
            EventChoiceRow(
                event = event,
                selected = event.id == uiState.selectedEventId,
                onClick = { onEventSelect(event.id) },
            )
        }
    }
}

/** 予定 1 行。押すとその予定に決める。今出している予定には印を付ける。 */
@Composable
private fun EventChoiceRow(
    event: WidgetEventUiModel,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val detail = event.oshiName ?: stringResource(UiR.string.event_no_oshi)
    ListItem(
        modifier = Modifier.selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            OshiAvatar(initial = event.initial, color = event.color, photoPath = event.photoPath)
        },
        headlineContent = {
            Text(text = event.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                text = stringResource(R.string.configure_row_supporting, formatMonthDayWeekday(event.occurrenceDate), detail),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (event.status == WidgetEventStatusUiModel.TODAY) {
                        stringResource(UiR.string.countdown_today)
                    } else {
                        stringResource(R.string.widget_days, event.daysUntil)
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = event.color.onSurfaceColor(),
                )
                if (selected) {
                    Icon(
                        painter = painterResource(UiR.drawable.ic_check),
                        contentDescription = stringResource(R.string.configure_selected),
                        tint = OshikatsuTheme.colorScheme.brand,
                    )
                }
            }
        },
    )
}

@Preview(widthDp = 380)
@Composable
private fun EventChoiceRowPreview() {
    OshikatsuTheme {
        EventChoiceRow(
            event = WidgetEventUiModel(
                id = "1",
                title = "LUMINA 5th LIVE 東京公演",
                type = EventTypeUiModel.LIVE,
                oshiName = "LUMINA",
                initial = "L",
                color = OshiColorUiModel.PINK,
                photoPath = null,
                occurrenceDate = LocalDate.of(2026, 10, 18),
                daysUntil = 11,
                status = WidgetEventStatusUiModel.UPCOMING,
            ),
            selected = true,
            onClick = {},
        )
    }
}
