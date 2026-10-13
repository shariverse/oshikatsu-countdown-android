package com.basekeita.oshikatsucountdown.feature.oshi.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.basekeita.oshikatsucountdown.feature.oshi.OshiListNavigationEvent
import com.basekeita.oshikatsucountdown.feature.oshi.R
import com.basekeita.oshikatsucountdown.feature.oshi.list.OshiListViewModel.UiState
import com.basekeita.oshikatsucountdown.feature.oshi.list.OshiListViewModel.ViewState
import com.basekeita.oshikatsucountdown.feature.oshi.uimodel.OshiListItemUiModel
import com.basekeita.oshikatsucountdown.ui.component.atoms.OshiAvatar
import com.basekeita.oshikatsucountdown.ui.theme.OshikatsuTheme
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiUiModel
import com.basekeita.oshikatsucountdown.ui.R as UiR

/** 一覧の幅の上限。タブレットで行が横に伸びすぎないようにする。 */
private val contentMaxWidth = 640.dp

/** 右下の「推しを登録」に最後の行が隠れないよう、一覧の下に空ける高さ。 */
private val fabClearance = 96.dp

/** 推しの一覧の Route。行を押すと編集、右下のボタンで登録。 */
@Composable
internal fun OshiListScreen(
    onNavigationEvent: (OshiListNavigationEvent) -> Unit,
    viewModel: OshiListViewModel = hiltViewModel(),
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    OshiListContent(
        viewState = viewState,
        onOshiClick = { oshiId -> onNavigationEvent(OshiListNavigationEvent.OpenOshi(oshiId)) },
        onAddClick = { onNavigationEvent(OshiListNavigationEvent.AddOshi) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OshiListContent(
    viewState: ViewState,
    onOshiClick: (String) -> Unit,
    onAddClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.oshi_list_title), fontWeight = FontWeight.Bold) },
                // システムバーの分は app が空ける
                windowInsets = WindowInsets(0.dp),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(painter = painterResource(UiR.drawable.ic_add), contentDescription = null) },
                text = { Text(text = stringResource(R.string.oshi_list_add)) },
            )
        },
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
                    OshiList(uiState = viewState.uiState, onOshiClick = onOshiClick, onAddClick = onAddClick)
                }
            }
        }
    }
}

@Composable
private fun OshiList(
    uiState: UiState,
    onOshiClick: (String) -> Unit,
    onAddClick: () -> Unit,
) {
    if (uiState.oshis.isEmpty()) {
        EmptyOshis(onAddClick = onAddClick)
        return
    }

    LazyColumn(
        modifier = Modifier
            .widthIn(max = contentMaxWidth)
            .fillMaxSize(),
        contentPadding = PaddingValues(bottom = fabClearance),
    ) {
        items(items = uiState.oshis, key = { item -> item.oshi.id }) { item ->
            ListItem(
                modifier = Modifier.clickable { onOshiClick(item.oshi.id) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = {
                    OshiAvatar(
                        initial = item.oshi.initial,
                        color = item.oshi.color,
                        photoPath = item.oshi.photoPath,
                        size = 48.dp,
                    )
                },
                headlineContent = { Text(text = item.oshi.name) },
                supportingContent = {
                    Text(text = pluralStringResource(R.plurals.oshi_list_event_count, item.eventCount, item.eventCount))
                },
            )
        }
    }
}

@Composable
private fun EmptyOshis(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(UiR.drawable.ic_favorite),
            contentDescription = null,
            tint = OshikatsuTheme.colorScheme.brand,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = stringResource(R.string.oshi_list_empty_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.oshi_list_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = OshikatsuTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        FilledTonalButton(onClick = onAddClick) {
            Text(text = stringResource(R.string.oshi_list_add))
        }
    }
}

@Preview(widthDp = 400, heightDp = 600)
@Composable
private fun OshiListContentPreview() {
    OshikatsuTheme {
        OshiListContent(
            viewState = ViewState.Success(
                UiState(
                    oshis = listOf(
                        OshiListItemUiModel(
                            oshi = OshiUiModel(id = "l", name = "LUMINA", initial = "L", color = OshiColorUiModel.PINK, photoPath = null),
                            eventCount = 2,
                        ),
                        OshiListItemUiModel(
                            oshi = OshiUiModel(id = "m", name = "ミナト", initial = "ミ", color = OshiColorUiModel.BLUE, photoPath = null),
                            eventCount = 1,
                        ),
                    ),
                ),
            ),
            onOshiClick = {},
            onAddClick = {},
        )
    }
}
