package com.basekeita.oshikatsucountdown.feature.eventeditor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.basekeita.oshikatsucountdown.feature.eventeditor.EventEditorViewModel.UiState
import com.basekeita.oshikatsucountdown.feature.eventeditor.EventEditorViewModel.ViewState
import com.basekeita.oshikatsucountdown.feature.eventeditor.uimodel.EventDraftUiModel
import com.basekeita.oshikatsucountdown.ui.component.atoms.OshiAvatar
import com.basekeita.oshikatsucountdown.ui.component.molecules.WriteFailureSnackbar
import com.basekeita.oshikatsucountdown.ui.rule.formatFullDate
import com.basekeita.oshikatsucountdown.ui.rule.formatTime
import com.basekeita.oshikatsucountdown.ui.theme.OshikatsuTheme
import com.basekeita.oshikatsucountdown.ui.uimodel.EventTypeUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiUiModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import com.basekeita.oshikatsucountdown.ui.R as UiR

/** 入力欄の幅の上限。タブレットで横に伸びすぎないようにする。 */
private val formMaxWidth = 560.dp

/**
 * 予定の追加・編集の Route。全画面のダイアログ（M3）の形で、推し・種類・タイトル・日付・開始時刻・会場・くり返しを入れる。
 * 初回の導線では「ステップ 2 / 2」として開き、「あとで」で保存せずに進める。
 */
@Composable
internal fun EventEditorScreen(
    eventId: String?,
    onboarding: Boolean,
    onNavigationEvent: (EventEditorNavigationEvent) -> Unit,
    viewModel: EventEditorViewModel = hiltViewModel<EventEditorViewModel, EventEditorViewModel.Factory>(
        creationCallback = { factory -> factory.create(eventId, onboarding) },
    ),
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val finish = { onNavigationEvent(EventEditorNavigationEvent.Finished) }

    val isFinished = (viewState as? ViewState.Success)?.uiState?.isFinished == true
    LaunchedEffect(isFinished) {
        if (isFinished) finish()
    }

    when (val state = viewState) {
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
            EventEditorContent(
                uiState = state.uiState,
                onClose = finish,
                onSave = viewModel::onSave,
                onOshiSelect = viewModel::onOshiSelect,
                onTypeSelect = viewModel::onTypeSelect,
                onTitleChange = viewModel::onTitleChange,
                onDateChange = viewModel::onDateChange,
                onStartTimeChange = viewModel::onStartTimeChange,
                onVenueChange = viewModel::onVenueChange,
                onRepeatsYearlyChange = viewModel::onRepeatsYearlyChange,
                onWriteFailureShown = viewModel::onWriteFailureShown,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventEditorContent(
    uiState: UiState,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onOshiSelect: (String?) -> Unit,
    onTypeSelect: (EventTypeUiModel) -> Unit,
    onTitleChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onStartTimeChange: (LocalTime?) -> Unit,
    onVenueChange: (String) -> Unit,
    onRepeatsYearlyChange: (Boolean) -> Unit,
    onWriteFailureShown: () -> Unit,
) {
    // 保存中に閉じると viewModelScope ごと書き込みが止まるため、戻る操作を受けて何もしない
    BackHandler(enabled = !uiState.canClose) {}

    val snackbarHostState = remember { SnackbarHostState() }
    WriteFailureSnackbar(
        failure = uiState.writeFailure,
        snackbarHostState = snackbarHostState,
        onShown = onWriteFailureShown,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.onboarding) {
                            stringResource(R.string.editor_onboarding_step)
                        } else if (uiState.isNew) {
                            stringResource(R.string.editor_title_add)
                        } else {
                            stringResource(R.string.editor_title_edit)
                        },
                    )
                },
                navigationIcon = {
                    if (!uiState.onboarding) {
                        IconButton(onClick = onClose, enabled = uiState.canClose) {
                            Icon(
                                painter = painterResource(UiR.drawable.ic_close),
                                contentDescription = stringResource(UiR.string.close),
                            )
                        }
                    }
                },
                actions = {
                    if (uiState.onboarding) {
                        TextButton(onClick = onClose, enabled = uiState.canClose) {
                            Text(text = stringResource(R.string.editor_onboarding_skip))
                        }
                    }
                },
                // システムバーの分は app が空ける
                windowInsets = WindowInsets(0.dp),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0.dp),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = formMaxWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (uiState.onboarding) {
                    OnboardingHeading()
                }
                EventForm(
                    draft = uiState.draft,
                    oshis = uiState.oshis,
                    onOshiSelect = onOshiSelect,
                    onTypeSelect = onTypeSelect,
                    onTitleChange = onTitleChange,
                    onDateChange = onDateChange,
                    onStartTimeChange = onStartTimeChange,
                    onVenueChange = onVenueChange,
                    onRepeatsYearlyChange = onRepeatsYearlyChange,
                )
                Button(
                    onClick = onSave,
                    enabled = uiState.canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .padding(bottom = 16.dp),
                ) {
                    Text(
                        text = if (uiState.onboarding) {
                            stringResource(R.string.editor_onboarding_save)
                        } else {
                            stringResource(UiR.string.save)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingHeading() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.editor_onboarding_heading),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.editor_onboarding_message),
            style = MaterialTheme.typography.bodyMedium,
            color = OshikatsuTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.EventForm(
    draft: EventDraftUiModel,
    oshis: List<OshiUiModel>,
    onOshiSelect: (String?) -> Unit,
    onTypeSelect: (EventTypeUiModel) -> Unit,
    onTitleChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onStartTimeChange: (LocalTime?) -> Unit,
    onVenueChange: (String) -> Unit,
    onRepeatsYearlyChange: (Boolean) -> Unit,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    FormSection(label = stringResource(R.string.editor_oshi_label)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            oshis.forEach { oshi ->
                FilterChip(
                    selected = oshi.id == draft.oshiId,
                    onClick = { onOshiSelect(oshi.id) },
                    label = { Text(text = oshi.name) },
                    leadingIcon = {
                        OshiAvatar(
                            initial = oshi.initial,
                            color = oshi.color,
                            photoPath = oshi.photoPath,
                            size = FilterChipDefaults.IconSize,
                        )
                    },
                )
            }
            FilterChip(
                selected = draft.oshiId == null,
                onClick = { onOshiSelect(null) },
                label = { Text(text = stringResource(UiR.string.event_no_oshi)) },
            )
        }
    }

    FormSection(label = stringResource(R.string.editor_type_label)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EventTypeUiModel.entries.forEach { type ->
                val selected = type == draft.type
                FilterChip(
                    selected = selected,
                    onClick = { onTypeSelect(type) },
                    label = { Text(text = stringResource(type.labelRes)) },
                    leadingIcon = if (selected) {
                        {
                            Icon(
                                painter = painterResource(UiR.drawable.ic_check),
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }

    OutlinedTextField(
        value = draft.title,
        onValueChange = onTitleChange,
        label = { Text(text = stringResource(R.string.editor_title_label)) },
        placeholder = { Text(text = stringResource(R.string.editor_title_placeholder)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )

    PickerField(
        label = stringResource(R.string.editor_date_label),
        value = formatFullDate(draft.date),
        onClick = { showDatePicker = true },
        trailingIcon = {
            Icon(painter = painterResource(UiR.drawable.ic_event), contentDescription = null)
        },
    )

    PickerField(
        label = stringResource(R.string.editor_time_label),
        value = draft.startTime?.let(::formatTime) ?: stringResource(R.string.editor_time_placeholder),
        onClick = { showTimePicker = true },
        trailingIcon = {
            if (draft.startTime != null) {
                IconButton(onClick = { onStartTimeChange(null) }) {
                    Icon(
                        painter = painterResource(UiR.drawable.ic_close),
                        contentDescription = stringResource(R.string.editor_time_clear),
                    )
                }
            } else {
                Icon(painter = painterResource(UiR.drawable.ic_schedule), contentDescription = null)
            }
        },
    )

    OutlinedTextField(
        value = draft.venue,
        onValueChange = onVenueChange,
        label = { Text(text = stringResource(R.string.editor_venue_label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
    )

    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(text = stringResource(R.string.editor_repeat_title)) },
        supportingContent = { Text(text = stringResource(R.string.editor_repeat_supporting)) },
        trailingContent = {
            Switch(checked = draft.repeatsYearly, onCheckedChange = onRepeatsYearlyChange)
        },
    )

    if (showDatePicker) {
        EventDatePickerDialog(
            initialDate = draft.date,
            onConfirm = { date ->
                showDatePicker = false
                onDateChange(date)
            },
            onDismiss = { showDatePicker = false },
        )
    }

    if (showTimePicker) {
        StartTimePickerDialog(
            initialTime = draft.startTime ?: DEFAULT_START_TIME,
            onConfirm = { time ->
                showTimePicker = false
                onStartTimeChange(time)
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

/** 開始時刻を初めて選ぶときの初期値。夕方の開演が多いので 18:00 から選ばせる。 */
private val DEFAULT_START_TIME: LocalTime = LocalTime.of(18, 0)

@Composable
private fun FormSection(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = OshikatsuTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

/**
 * 押すとダイアログを開く、書き換えられない入力欄。押した操作は入力欄の `interactionSource` から受ける
 * （`clickable` を重ねると入力欄が押下を先に取るため）。
 */
@Composable
private fun PickerField(
    label: String,
    value: String,
    onClick: () -> Unit,
    trailingIcon: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) onClick()
        }
    }
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(text = label) },
        trailingIcon = trailingIcon,
        interactionSource = interactionSource,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** M3 の `DatePicker` は UTC の 0 時のミリ秒で日付を扱うので、`LocalDate` との間は UTC で変換する。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDatePickerDialog(
    initialDate: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis == null) {
                        onDismiss()
                    } else {
                        onConfirm(Instant.ofEpochMilli(selectedMillis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
            ) {
                Text(text = stringResource(R.string.editor_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(UiR.string.cancel))
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartTimePickerDialog(
    initialTime: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true,
    )
    TimePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(timePickerState.hour, timePickerState.minute)) }) {
                Text(text = stringResource(R.string.editor_ok))
            }
        },
        title = { Text(text = stringResource(R.string.editor_time_dialog_title)) },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(UiR.string.cancel))
            }
        },
    ) {
        TimePicker(state = timePickerState)
    }
}

@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun EventEditorContentPreview() {
    OshikatsuTheme {
        EventEditorContent(
            uiState = UiState(
                draft = EventDraftUiModel(
                    oshiId = "l",
                    type = EventTypeUiModel.LIVE,
                    title = "5th LIVE 東京公演",
                    date = LocalDate.of(2026, 10, 18),
                    startTime = LocalTime.of(17, 0),
                    venue = "",
                    repeatsYearly = false,
                ),
                oshis = listOf(
                    OshiUiModel(id = "l", name = "LUMINA", initial = "L", color = OshiColorUiModel.PINK, photoPath = null),
                    OshiUiModel(id = "m", name = "ミナト", initial = "ミ", color = OshiColorUiModel.BLUE, photoPath = null),
                ),
                isNew = true,
                onboarding = false,
                canSave = true,
                canClose = true,
                isFinished = false,
                writeFailure = null,
            ),
            onClose = {},
            onSave = {},
            onOshiSelect = {},
            onTypeSelect = {},
            onTitleChange = {},
            onDateChange = {},
            onStartTimeChange = {},
            onVenueChange = {},
            onRepeatsYearlyChange = {},
            onWriteFailureShown = {},
        )
    }
}
