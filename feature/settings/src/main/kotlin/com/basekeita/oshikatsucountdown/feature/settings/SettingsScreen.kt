package com.basekeita.oshikatsucountdown.feature.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.basekeita.oshikatsucountdown.feature.settings.SettingsViewModel.UiState
import com.basekeita.oshikatsucountdown.feature.settings.uimodel.NotificationPermissionUiModel
import com.basekeita.oshikatsucountdown.feature.settings.uimodel.ReminderSettingsUiModel
import com.basekeita.oshikatsucountdown.feature.settings.uimodel.ReminderTimingUiModel
import com.basekeita.oshikatsucountdown.ui.canPostNotifications
import com.basekeita.oshikatsucountdown.ui.canScheduleExactAlarms
import com.basekeita.oshikatsucountdown.ui.component.molecules.WriteFailureSnackbar
import com.basekeita.oshikatsucountdown.ui.rule.formatTime
import com.basekeita.oshikatsucountdown.ui.theme.OshikatsuTheme
import java.time.LocalTime
import com.basekeita.oshikatsucountdown.ui.R as UiR

/** 中身の幅の上限。タブレットで行が横に伸びすぎないようにする。 */
private val contentMaxWidth = 640.dp

/** 許可のダイアログを出せる版（Android 13 以降）。 */
private val canRequestNotificationPermission: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/** 設定の Route。終わった予定の表示、通知、データの保存先の説明、アプリのバージョンとライセンスを置く。 */
@Composable
internal fun SettingsScreen(
    onNavigationEvent: (SettingsNavigationEvent) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 端末の設定で許可を変えて戻ってきたときも、案内を出し直す
    LifecycleResumeEffect(Unit) {
        viewModel.onNotificationPermissionCheck(
            granted = canPostNotifications(context),
            canRequest = canRequestNotificationPermission,
            exactAlarmsAllowed = canScheduleExactAlarms(context),
        )
        onPauseOrDispose {}
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onNotificationPermissionResult(granted && canPostNotifications(context))
    }
    LaunchedEffect(uiState.isRequestingNotificationPermission) {
        if (!uiState.isRequestingNotificationPermission) return@LaunchedEffect

        if (canRequestNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.onNotificationPermissionResult(canPostNotifications(context))
        }
    }

    SettingsContent(
        uiState = uiState,
        onShowPastEventsChange = viewModel::onShowPastEventsChange,
        onRemindersEnabledChange = viewModel::onRemindersEnabledChange,
        onReminderTimingChange = viewModel::onReminderTimingChange,
        onReminderTimeChange = viewModel::onReminderTimeChange,
        onAllowNotificationsClick = viewModel::onAllowNotificationsClick,
        onOpenNotificationSettingsClick = { context.startActivity(notificationSettingsIntent(context)) },
        onOpenExactAlarmSettingsClick = { context.startActivity(exactAlarmSettingsIntent(context)) },
        onLicensesClick = { onNavigationEvent(SettingsNavigationEvent.OpenLicenses) },
        onWriteFailureShown = viewModel::onWriteFailureShown,
    )
}

/** 端末の設定の、このアプリのアラームの画面。正確なアラームを使えないのは Android 12 だけなので、その版でだけ開く。 */
private fun exactAlarmSettingsIntent(context: Context): Intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.fromParts("package", context.packageName, null))
} else {
    notificationSettingsIntent(context)
}

/** 端末の設定の、このアプリの通知の画面。 */
private fun notificationSettingsIntent(context: Context): Intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    uiState: UiState,
    onShowPastEventsChange: (Boolean) -> Unit,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onReminderTimingChange: (ReminderTimingUiModel, Boolean) -> Unit,
    onReminderTimeChange: (LocalTime) -> Unit,
    onAllowNotificationsClick: () -> Unit,
    onOpenNotificationSettingsClick: () -> Unit,
    onOpenExactAlarmSettingsClick: () -> Unit,
    onLicensesClick: () -> Unit,
    onWriteFailureShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    WriteFailureSnackbar(
        failure = uiState.writeFailure,
        snackbarHostState = snackbarHostState,
        onShown = onWriteFailureShown,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
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
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (uiState.isLoadFailed) {
                Text(
                    text = stringResource(UiR.string.load_error),
                    style = MaterialTheme.typography.bodyLarge,
                    color = OshikatsuTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            } else if (!uiState.isLoading) {
                SettingsList(
                    uiState = uiState,
                    onShowPastEventsChange = onShowPastEventsChange,
                    onRemindersEnabledChange = onRemindersEnabledChange,
                    onReminderTimingChange = onReminderTimingChange,
                    onReminderTimeChange = onReminderTimeChange,
                    onAllowNotificationsClick = onAllowNotificationsClick,
                    onOpenNotificationSettingsClick = onOpenNotificationSettingsClick,
                    onOpenExactAlarmSettingsClick = onOpenExactAlarmSettingsClick,
                    onLicensesClick = onLicensesClick,
                )
            }
        }
    }
}

@Composable
private fun SettingsList(
    uiState: UiState,
    onShowPastEventsChange: (Boolean) -> Unit,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onReminderTimingChange: (ReminderTimingUiModel, Boolean) -> Unit,
    onReminderTimeChange: (LocalTime) -> Unit,
    onAllowNotificationsClick: () -> Unit,
    onOpenNotificationSettingsClick: () -> Unit,
    onOpenExactAlarmSettingsClick: () -> Unit,
    onLicensesClick: () -> Unit,
) {
    val transparent = ListItemDefaults.colors(containerColor = Color.Transparent)
    Column(
        modifier = Modifier
            .widthIn(max = contentMaxWidth)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        SectionHeader(title = stringResource(R.string.settings_display_header))
        SwitchRow(
            title = stringResource(R.string.settings_show_past_title),
            supporting = stringResource(R.string.settings_show_past_supporting),
            checked = uiState.showPastEvents,
            onCheckedChange = onShowPastEventsChange,
        )

        SectionHeader(title = stringResource(R.string.settings_reminder_header))
        ReminderSection(
            reminders = uiState.reminders,
            permission = uiState.notificationPermission,
            onEnabledChange = onRemindersEnabledChange,
            onTimingChange = onReminderTimingChange,
            onTimeChange = onReminderTimeChange,
            onAllowNotificationsClick = onAllowNotificationsClick,
            onOpenNotificationSettingsClick = onOpenNotificationSettingsClick,
            onOpenExactAlarmSettingsClick = onOpenExactAlarmSettingsClick,
        )

        SectionHeader(title = stringResource(R.string.settings_data_header))
        ListItem(
            colors = transparent,
            headlineContent = { Text(text = stringResource(R.string.settings_data_local_title)) },
            supportingContent = { Text(text = stringResource(R.string.settings_data_local_supporting)) },
        )

        SectionHeader(title = stringResource(R.string.settings_about_header))
        ListItem(
            colors = transparent,
            headlineContent = { Text(text = stringResource(R.string.settings_version_title)) },
            supportingContent = { Text(text = uiState.appVersionName) },
        )
        ListItem(
            modifier = Modifier.clickable(onClick = onLicensesClick),
            colors = transparent,
            headlineContent = { Text(text = stringResource(R.string.settings_licenses_title)) },
            supportingContent = { Text(text = stringResource(R.string.settings_licenses_supporting)) },
        )
    }
}

/**
 * 通知の節。「予定を通知する」が OFF のときは、タイミングと時刻の項目とその説明を隠す。
 * 許可が無ければ、OFF でも、許可を求めるか端末の設定を開くボタンを出す。
 */
@Composable
private fun ReminderSection(
    reminders: ReminderSettingsUiModel,
    permission: NotificationPermissionUiModel,
    onEnabledChange: (Boolean) -> Unit,
    onTimingChange: (ReminderTimingUiModel, Boolean) -> Unit,
    onTimeChange: (LocalTime) -> Unit,
    onAllowNotificationsClick: () -> Unit,
    onOpenNotificationSettingsClick: () -> Unit,
    onOpenExactAlarmSettingsClick: () -> Unit,
) {
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    val transparent = ListItemDefaults.colors(containerColor = Color.Transparent)

    SwitchRow(
        title = stringResource(R.string.settings_reminder_enabled_title),
        supporting = null,
        checked = reminders.enabled,
        onCheckedChange = onEnabledChange,
    )
    if (reminders.enabled) {
        ReminderTimingUiModel.entries.forEach { timing ->
            SwitchRow(
                title = stringResource(timing.labelRes),
                supporting = null,
                checked = timing in reminders.enabledTimings,
                onCheckedChange = { enabled -> onTimingChange(timing, enabled) },
            )
        }
        ListItem(
            modifier = Modifier.clickable { showTimePicker = true },
            colors = transparent,
            headlineContent = { Text(text = stringResource(R.string.settings_reminder_time_title)) },
            trailingContent = { Text(text = formatTime(reminders.timeOfDay), style = MaterialTheme.typography.bodyLarge) },
        )
        Text(
            text = stringResource(R.string.settings_reminder_footer),
            style = MaterialTheme.typography.bodySmall,
            color = OshikatsuTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    // 許可の案内は「予定を通知する」が OFF でも出す（当日のカウントダウンは「予定を通知する」とは別で、同じ許可が要る）
    when (permission) {
        NotificationPermissionUiModel.GRANTED -> Unit

        NotificationPermissionUiModel.NOT_REQUESTED -> {
            TextButton(onClick = onAllowNotificationsClick, modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(text = stringResource(R.string.settings_reminder_allow))
            }
        }

        NotificationPermissionUiModel.DENIED -> {
            Text(
                text = stringResource(R.string.settings_reminder_denied),
                style = MaterialTheme.typography.bodySmall,
                color = OshikatsuTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
            )
            TextButton(onClick = onOpenNotificationSettingsClick, modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(text = stringResource(R.string.settings_reminder_open_settings))
            }
        }

        NotificationPermissionUiModel.EXACT_ALARM_DENIED -> {
            Text(
                text = stringResource(R.string.settings_reminder_exact_alarm_denied),
                style = MaterialTheme.typography.bodySmall,
                color = OshikatsuTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
            )
            TextButton(onClick = onOpenExactAlarmSettingsClick, modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(text = stringResource(R.string.settings_reminder_open_settings))
            }
        }
    }

    if (showTimePicker) {
        ReminderTimePickerDialog(
            initialTime = reminders.timeOfDay,
            onConfirm = { time ->
                onTimeChange(time)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

/** 行全体を押してもスイッチが切り替わるよう、行に toggleable を付けてスイッチ自体は押下を受けない。 */
@Composable
private fun SwitchRow(
    title: String,
    supporting: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        modifier = Modifier.toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(text = title) },
        supportingContent = supporting?.let { text -> { Text(text = text) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePickerDialog(
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
                Text(text = stringResource(R.string.settings_ok))
            }
        },
        title = { Text(text = stringResource(R.string.settings_reminder_time_dialog_title)) },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(UiR.string.cancel))
            }
        },
    ) {
        TimePicker(state = timePickerState)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = OshikatsuTheme.colorScheme.brand,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
            .semantics { heading() },
    )
}

@Preview(widthDp = 400, heightDp = 1200)
@Composable
private fun SettingsContentPreview() {
    OshikatsuTheme {
        SettingsContent(
            uiState = UiState(
                isLoading = false,
                showPastEvents = true,
                notificationPermission = NotificationPermissionUiModel.DENIED,
                appVersionName = "1.0",
            ),
            onShowPastEventsChange = {},
            onRemindersEnabledChange = {},
            onReminderTimingChange = { _, _ -> },
            onReminderTimeChange = {},
            onAllowNotificationsClick = {},
            onOpenNotificationSettingsClick = {},
            onOpenExactAlarmSettingsClick = {},
            onLicensesClick = {},
            onWriteFailureShown = {},
        )
    }
}
