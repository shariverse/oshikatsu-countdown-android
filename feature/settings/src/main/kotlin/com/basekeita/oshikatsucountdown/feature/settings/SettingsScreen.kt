package com.basekeita.oshikatsucountdown.feature.settings

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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.basekeita.oshikatsucountdown.feature.settings.SettingsViewModel.UiState
import com.basekeita.oshikatsucountdown.feature.settings.SettingsViewModel.ViewState
import com.basekeita.oshikatsucountdown.ui.component.molecules.WriteFailureSnackbar
import com.basekeita.oshikatsucountdown.ui.theme.OshikatsuTheme
import com.basekeita.oshikatsucountdown.ui.R as UiR

/** 中身の幅の上限。タブレットで行が横に伸びすぎないようにする。 */
private val contentMaxWidth = 640.dp

/** 設定の Route。終わった予定の表示、データの保存先の説明、アプリのバージョンとライセンスを置く。 */
@Composable
internal fun SettingsScreen(
    onNavigationEvent: (SettingsNavigationEvent) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    SettingsContent(
        viewState = viewState,
        onShowPastEventsChange = viewModel::onShowPastEventsChange,
        onLicensesClick = { onNavigationEvent(SettingsNavigationEvent.OpenLicenses) },
        onWriteFailureShown = viewModel::onWriteFailureShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    viewState: ViewState,
    onShowPastEventsChange: (Boolean) -> Unit,
    onLicensesClick: () -> Unit,
    onWriteFailureShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    WriteFailureSnackbar(
        failure = (viewState as? ViewState.Success)?.uiState?.writeFailure,
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
                    SettingsList(
                        uiState = viewState.uiState,
                        onShowPastEventsChange = onShowPastEventsChange,
                        onLicensesClick = onLicensesClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsList(
    uiState: UiState,
    onShowPastEventsChange: (Boolean) -> Unit,
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
        // 行全体を押してもスイッチが切り替わるよう、行に toggleable を付けてスイッチ自体は押下を受けない
        ListItem(
            modifier = Modifier.toggleable(
                value = uiState.showPastEvents,
                role = Role.Switch,
                onValueChange = onShowPastEventsChange,
            ),
            colors = transparent,
            headlineContent = { Text(text = stringResource(R.string.settings_show_past_title)) },
            supportingContent = { Text(text = stringResource(R.string.settings_show_past_supporting)) },
            trailingContent = { Switch(checked = uiState.showPastEvents, onCheckedChange = null) },
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

@Preview(widthDp = 400, heightDp = 640)
@Composable
private fun SettingsContentPreview() {
    OshikatsuTheme {
        SettingsContent(
            viewState = ViewState.Success(UiState(showPastEvents = true, appVersionName = "1.0", writeFailure = null)),
            onShowPastEventsChange = {},
            onLicensesClick = {},
            onWriteFailureShown = {},
        )
    }
}
