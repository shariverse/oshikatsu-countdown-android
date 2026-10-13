package com.basekeita.oshikatsucountdown.feature.eventdetail

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.basekeita.oshikatsucountdown.feature.eventdetail.EventDetailViewModel.UiState
import com.basekeita.oshikatsucountdown.feature.eventdetail.EventDetailViewModel.ViewState
import com.basekeita.oshikatsucountdown.feature.eventdetail.ui.molecules.CountdownPanel
import com.basekeita.oshikatsucountdown.feature.eventdetail.uimodel.CountdownStatusUiModel
import com.basekeita.oshikatsucountdown.feature.eventdetail.uimodel.CountdownUiModel
import com.basekeita.oshikatsucountdown.feature.eventdetail.uimodel.EventDetailUiModel
import com.basekeita.oshikatsucountdown.ui.component.molecules.OshiPhoto
import com.basekeita.oshikatsucountdown.ui.component.molecules.WriteFailureSnackbar
import com.basekeita.oshikatsucountdown.ui.rule.formatMonthDayWeekday
import com.basekeita.oshikatsucountdown.ui.rule.formatTime
import com.basekeita.oshikatsucountdown.ui.theme.OshikatsuTheme
import com.basekeita.oshikatsucountdown.ui.theme.containerColor
import com.basekeita.oshikatsucountdown.ui.uimodel.EventTypeUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel
import java.time.LocalDate
import java.time.LocalTime
import com.basekeita.oshikatsucountdown.ui.R as UiR

/** 写真の高さ。中身のカードは下端に少し重ねる。 */
private val photoHeight = 300.dp

/** 中身のカードを写真に重ねる幅。 */
private val cardOverlap = 40.dp

/** 中身の幅の上限。タブレットで横に伸びすぎないようにする。 */
private val contentMaxWidth = 640.dp

/**
 * 予定の詳細の Route。推しの写真の上に、開始までの残り（秒単位）・進み具合・日時と会場を出す。
 * 予定が無くなったら（削除した後を含む）前の画面に戻る。
 */
@Composable
internal fun EventDetailScreen(
    eventId: String,
    onNavigationEvent: (EventDetailNavigationEvent) -> Unit,
    viewModel: EventDetailViewModel = hiltViewModel<EventDetailViewModel, EventDetailViewModel.Factory>(
        creationCallback = { factory -> factory.create(eventId) },
    ),
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    // 残りは毎秒変わるので、予定が無いかどうかだけで起動し直す
    val notFound = viewState is ViewState.NotFound
    LaunchedEffect(notFound) {
        if (notFound) onNavigationEvent(EventDetailNavigationEvent.Back)
    }

    when (val state = viewState) {
        ViewState.Loading, ViewState.NotFound -> Unit

        is ViewState.Error -> {
            Text(
                text = stringResource(UiR.string.load_error),
                style = MaterialTheme.typography.bodyLarge,
                color = OshikatsuTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
        }

        is ViewState.Success -> {
            EventDetailContent(
                uiState = state.uiState,
                onBack = { onNavigationEvent(EventDetailNavigationEvent.Back) },
                onEdit = { onNavigationEvent(EventDetailNavigationEvent.Edit(eventId)) },
                onPinnedChange = viewModel::onPinnedChange,
                onDelete = viewModel::onDelete,
                onWriteFailureShown = viewModel::onWriteFailureShown,
            )
        }
    }
}

@Composable
private fun EventDetailContent(
    uiState: UiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onPinnedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onWriteFailureShown: () -> Unit,
) {
    val event = uiState.event
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }

    // 削除の途中で戻ると viewModelScope ごと書き込みが止まるため、戻る操作を受けて何もしない
    BackHandler(enabled = !uiState.canClose) {}

    val snackbarHostState = remember { SnackbarHostState() }
    WriteFailureSnackbar(
        failure = uiState.writeFailure,
        snackbarHostState = snackbarHostState,
        onShown = onWriteFailureShown,
    )

    // カードの下が短いときも地をカードとそろえる
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OshikatsuTheme.colorScheme.surface),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Box {
                OshiPhoto(
                    photoSource = event.photoPath,
                    color = event.color,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(photoHeight),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = photoHeight - cardOverlap),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    DetailCard(
                        event = event,
                        countdown = uiState.countdown,
                        modifier = Modifier
                            .widthIn(max = contentMaxWidth)
                            .fillMaxWidth(),
                    )
                }
            }
        }
        DetailTopBar(
            pinned = event.pinned,
            canClose = uiState.canClose,
            onBack = onBack,
            onEdit = onEdit,
            onPinnedChange = onPinnedChange,
            onDeleteClick = { confirmingDelete = true },
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text(text = stringResource(R.string.detail_delete_confirm_title)) },
            text = { Text(text = stringResource(R.string.detail_delete_confirm_message, event.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingDelete = false
                        onDelete()
                    },
                ) {
                    Text(text = stringResource(UiR.string.delete), color = OshikatsuTheme.colorScheme.destructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) {
                    Text(text = stringResource(UiR.string.cancel))
                }
            },
        )
    }
}

/** 写真の上に浮かべるボタン。写真の明るさに依らず見えるよう、カードの地を敷いた丸いボタンにする。 */
@Composable
private fun DetailTopBar(
    pinned: Boolean,
    canClose: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onPinnedChange: (Boolean) -> Unit,
    onDeleteClick: () -> Unit,
) {
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FloatingIconButton(
            iconRes = UiR.drawable.ic_arrow_back,
            contentDescription = stringResource(UiR.string.back),
            onClick = onBack,
            enabled = canClose,
        )
        Spacer(modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (pinned) {
                FloatingIconButton(
                    iconRes = UiR.drawable.ic_keep_off,
                    contentDescription = stringResource(R.string.detail_unpin),
                    onClick = { onPinnedChange(false) },
                )
            } else {
                FloatingIconButton(
                    iconRes = UiR.drawable.ic_push_pin,
                    contentDescription = stringResource(R.string.detail_pin),
                    onClick = { onPinnedChange(true) },
                )
            }
            FloatingIconButton(
                iconRes = UiR.drawable.ic_edit,
                contentDescription = stringResource(UiR.string.edit),
                onClick = onEdit,
            )
            Box {
                FloatingIconButton(
                    iconRes = UiR.drawable.ic_more,
                    contentDescription = stringResource(UiR.string.more_options),
                    onClick = { menuExpanded = true },
                )
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.detail_delete)) },
                        leadingIcon = { Icon(painter = painterResource(UiR.drawable.ic_delete), contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onDeleteClick()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val colorScheme = OshikatsuTheme.colorScheme
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = colorScheme.surface.copy(alpha = 0.88f),
            contentColor = colorScheme.onSurface,
        ),
    ) {
        Icon(painter = painterResource(iconRes), contentDescription = contentDescription)
    }
}

@Composable
private fun DetailCard(
    event: EventDetailUiModel,
    countdown: CountdownUiModel,
    modifier: Modifier = Modifier,
) {
    val colorScheme = OshikatsuTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(shape = RoundedCornerShape(8.dp), color = event.color.containerColor()) {
                            Text(
                                text = stringResource(event.type.labelRes),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                        Text(
                            text = event.oshiName ?: stringResource(UiR.string.event_no_oshi),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.semantics { heading() },
                    )
                }
                CountdownPanel(
                    countdown = countdown,
                    type = event.type,
                    hasStartTime = event.startTime != null,
                    occurrenceDate = event.occurrenceDate,
                    color = event.color,
                )
            }
            HorizontalDivider(color = colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 20.dp))
            val dateText = formatMonthDayWeekday(event.occurrenceDate)
            InfoRow(
                iconRes = UiR.drawable.ic_schedule,
                title = stringResource(R.string.detail_date_title),
                value = if (event.startTime != null) {
                    stringResource(
                        R.string.detail_date_time,
                        dateText,
                        stringResource(event.type.startLabelRes),
                        formatTime(event.startTime),
                    )
                } else {
                    dateText
                },
            )
            if (event.venue.isNotEmpty()) {
                InfoRow(
                    iconRes = UiR.drawable.ic_location_on,
                    title = stringResource(R.string.detail_venue_title),
                    value = event.venue,
                )
            }
            if (event.repeatsYearly) {
                InfoRow(
                    iconRes = UiR.drawable.ic_event,
                    title = stringResource(R.string.detail_repeat_title),
                    value = stringResource(R.string.detail_repeat_yearly),
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    @DrawableRes iconRes: Int,
    title: String,
    value: String,
) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Icon(painter = painterResource(iconRes), contentDescription = null) },
        overlineContent = { Text(text = title) },
        headlineContent = { Text(text = value) },
    )
}

@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun EventDetailContentPreview() {
    OshikatsuTheme {
        Surface(color = OshikatsuTheme.colorScheme.surface) {
            EventDetailContent(
                uiState = UiState(
                    event = EventDetailUiModel(
                        title = "5th LIVE 東京公演",
                        type = EventTypeUiModel.LIVE,
                        oshiName = "LUMINA",
                        color = OshiColorUiModel.PINK,
                        photoPath = null,
                        occurrenceDate = LocalDate.of(2026, 10, 18),
                        startTime = LocalTime.of(17, 0),
                        venue = "東京ドーム",
                        repeatsYearly = false,
                        pinned = true,
                    ),
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
                    canClose = true,
                    writeFailure = null,
                ),
                onBack = {},
                onEdit = {},
                onPinnedChange = {},
                onDelete = {},
                onWriteFailureShown = {},
            )
        }
    }
}
