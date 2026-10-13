package com.basekeita.oshikatsucountdown.feature.oshi.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.basekeita.oshikatsucountdown.feature.oshi.OshiEditorNavigationEvent
import com.basekeita.oshikatsucountdown.feature.oshi.R
import com.basekeita.oshikatsucountdown.feature.oshi.editor.OshiEditorViewModel.UiState
import com.basekeita.oshikatsucountdown.feature.oshi.editor.OshiEditorViewModel.ViewState
import com.basekeita.oshikatsucountdown.ui.component.molecules.OshiPhoto
import com.basekeita.oshikatsucountdown.ui.component.molecules.WriteFailureSnackbar
import com.basekeita.oshikatsucountdown.ui.theme.OshikatsuTheme
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel
import com.basekeita.oshikatsucountdown.ui.R as UiR

/** 入力欄の幅の上限。タブレットで横に伸びすぎないようにする。 */
private val formMaxWidth = 560.dp

/** 推し色の見本を 1 行に並べる数。12 色を 2 行に収める。 */
private const val SWATCH_COLUMNS = 6

/**
 * 推しの登録・編集の Route。写真（Photo Picker）・名前・推し色を入れる。
 * 初回の導線では「ステップ 1 / 2」として開き、「あとで」で登録せずに次へ進める。
 */
@Composable
internal fun OshiEditorScreen(
    oshiId: String?,
    onboarding: Boolean,
    onNavigationEvent: (OshiEditorNavigationEvent) -> Unit,
    viewModel: OshiEditorViewModel = hiltViewModel<OshiEditorViewModel, OshiEditorViewModel.Factory>(
        creationCallback = { factory -> factory.create(oshiId, onboarding) },
    ),
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val finish = { onNavigationEvent(OshiEditorNavigationEvent.Finished) }

    val isFinished = (viewState as? ViewState.Success)?.uiState?.isFinished == true
    LaunchedEffect(isFinished) {
        if (isFinished) finish()
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onPhotoPick(uri.toString())
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
            OshiEditorContent(
                uiState = state.uiState,
                onClose = finish,
                onSave = viewModel::onSave,
                onDelete = viewModel::onDelete,
                onPickPhoto = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onRemovePhoto = viewModel::onPhotoRemove,
                onNameChange = viewModel::onNameChange,
                onColorSelect = viewModel::onColorSelect,
                onWriteFailureShown = viewModel::onWriteFailureShown,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OshiEditorContent(
    uiState: UiState,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onNameChange: (String) -> Unit,
    onColorSelect: (OshiColorUiModel) -> Unit,
    onWriteFailureShown: () -> Unit,
) {
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }

    // 保存・削除の途中で閉じると viewModelScope ごと書き込みが止まるため、戻る操作を受けて何もしない
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
                            stringResource(R.string.oshi_editor_onboarding_step)
                        } else if (uiState.isNew) {
                            stringResource(R.string.oshi_editor_title_add)
                        } else {
                            stringResource(R.string.oshi_editor_title_edit)
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
                            Text(text = stringResource(R.string.oshi_editor_onboarding_skip))
                        }
                    } else if (!uiState.isNew) {
                        IconButton(onClick = { confirmingDelete = true }) {
                            Icon(
                                painter = painterResource(UiR.drawable.ic_delete),
                                contentDescription = stringResource(R.string.oshi_editor_delete),
                            )
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
                PhotoArea(
                    photoSource = uiState.photoSource,
                    color = uiState.color,
                    onPickPhoto = onPickPhoto,
                    onRemovePhoto = onRemovePhoto,
                )
                if (uiState.onboarding) {
                    Text(
                        text = stringResource(R.string.oshi_editor_onboarding_heading),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.semantics { heading() },
                    )
                }
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = onNameChange,
                    label = { Text(text = stringResource(R.string.oshi_editor_name_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                ColorPalette(selected = uiState.color, onSelect = onColorSelect)
                Text(
                    text = stringResource(R.string.oshi_editor_privacy_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = OshikatsuTheme.colorScheme.onSurfaceVariant,
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
                            stringResource(R.string.oshi_editor_onboarding_next)
                        } else {
                            stringResource(UiR.string.save)
                        },
                    )
                }
            }
        }
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text(text = stringResource(R.string.oshi_editor_delete_confirm_title, uiState.name)) },
            text = {
                Text(
                    text = if (uiState.eventCount > 0) {
                        pluralStringResource(
                            R.plurals.oshi_editor_delete_confirm_message_with_events,
                            uiState.eventCount,
                            uiState.eventCount,
                        )
                    } else {
                        stringResource(R.string.oshi_editor_delete_confirm_message)
                    },
                )
            },
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

/** 写真の場所。写真が無ければ推し色の縞を敷き、真ん中に「写真を選ぶ」を置く。 */
@Composable
private fun PhotoArea(
    photoSource: String?,
    color: OshiColorUiModel,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    val colorScheme = OshikatsuTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(28.dp)),
    ) {
        OshiPhoto(photoSource = photoSource, color = color, modifier = Modifier.fillMaxSize())
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                onClick = onPickPhoto,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = colorScheme.surface.copy(alpha = 0.9f),
                    contentColor = colorScheme.onSurface,
                ),
            ) {
                Icon(
                    painter = painterResource(UiR.drawable.ic_add_photo_alternate),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Text(
                    text = if (photoSource == null) {
                        stringResource(R.string.oshi_editor_pick_photo)
                    } else {
                        stringResource(R.string.oshi_editor_change_photo)
                    },
                    modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
                )
            }
            if (photoSource != null) {
                OutlinedButton(
                    onClick = onRemovePhoto,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = colorScheme.surface.copy(alpha = 0.9f),
                        contentColor = colorScheme.onSurface,
                    ),
                    border = BorderStroke(1.dp, colorScheme.outline),
                ) {
                    Text(text = stringResource(R.string.oshi_editor_remove_photo))
                }
            }
        }
    }
}

/**
 * 推し色の見本（6 列 × 2 行）。1 つだけ選べるのでラジオボタンとして読み上げる。
 * 選んだ色は白い縁と推し色の外輪で囲む。
 */
@Composable
private fun ColorPalette(
    selected: OshiColorUiModel,
    onSelect: (OshiColorUiModel) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.oshi_editor_color_label),
            style = MaterialTheme.typography.labelLarge,
            color = OshikatsuTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 間隔を 8dp にすると、幅 360dp の画面でも見本 1 つが 48dp になる
            OshiColorUiModel.entries.chunked(SWATCH_COLUMNS).forEach { rowColors ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowColors.forEach { oshiColor ->
                        ColorSwatch(
                            oshiColor = oshiColor,
                            selected = oshiColor == selected,
                            onClick = { onSelect(oshiColor) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    oshiColor: OshiColorUiModel,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(oshiColor.labelRes)
    val ringColor = if (selected) {
        oshiColor.color
    } else {
        Color.Transparent
    }
    Box(
        // 押せる範囲は外輪まで含めた見本全体
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label }
            .border(width = 3.dp, color = ringColor, shape = CircleShape)
            .padding(5.dp)
            .clip(CircleShape)
            .background(oshiColor.color),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                painter = painterResource(UiR.drawable.ic_check),
                contentDescription = null,
                tint = oshiColor.onColor,
            )
        }
    }
}

@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun OshiEditorContentPreview() {
    OshikatsuTheme {
        OshiEditorContent(
            uiState = UiState(
                name = "LUMINA",
                color = OshiColorUiModel.PINK,
                photoSource = null,
                isNew = true,
                onboarding = true,
                eventCount = 0,
                canSave = true,
                canClose = true,
                isFinished = false,
                writeFailure = null,
            ),
            onClose = {},
            onSave = {},
            onDelete = {},
            onPickPhoto = {},
            onRemovePhoto = {},
            onNameChange = {},
            onColorSelect = {},
            onWriteFailureShown = {},
        )
    }
}
