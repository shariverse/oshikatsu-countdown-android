package com.basekeita.oshikatsucountdown.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.basekeita.oshikatsucountdown.feature.home.HomeViewModel.UiState
import com.basekeita.oshikatsucountdown.feature.home.HomeViewModel.ViewState
import com.basekeita.oshikatsucountdown.feature.home.ui.molecules.FeaturedEventCard
import com.basekeita.oshikatsucountdown.feature.home.ui.molecules.HomeEventRow
import com.basekeita.oshikatsucountdown.feature.home.uimodel.EventStatusUiModel
import com.basekeita.oshikatsucountdown.feature.home.uimodel.HomeEventUiModel
import com.basekeita.oshikatsucountdown.ui.theme.OshikatsuTheme
import com.basekeita.oshikatsucountdown.ui.uimodel.EventTypeUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiUiModel
import java.time.LocalDate
import java.time.LocalTime
import com.basekeita.oshikatsucountdown.ui.R as UiR

/** 一覧の幅の上限。タブレットで行が横に伸びすぎないようにする。 */
private val contentMaxWidth = 640.dp

/** 右下の「予定を追加」に最後の行が隠れないよう、一覧の下に空ける高さ。 */
private val fabClearance = 96.dp

/** ホームの Route。推しの絞り込み、先頭の予定、これからの予定・終わった予定、右下の「予定を追加」を置く。 */
@Composable
internal fun HomeScreen(
    onNavigationEvent: (HomeNavigationEvent) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    HomeContent(
        viewState = viewState,
        onOshiFilterSelect = viewModel::onOshiFilterSelect,
        onEventClick = { eventId -> onNavigationEvent(HomeNavigationEvent.OpenEvent(eventId)) },
        onAddEventClick = { onNavigationEvent(HomeNavigationEvent.AddEvent) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    viewState: ViewState,
    onOshiFilterSelect: (String?) -> Unit,
    onEventClick: (String) -> Unit,
    onAddEventClick: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.home_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
                scrollBehavior = scrollBehavior,
                // システムバーの分は app が空ける
                windowInsets = WindowInsets(0.dp),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddEventClick,
                icon = { Icon(painter = painterResource(UiR.drawable.ic_add), contentDescription = null) },
                text = { Text(text = stringResource(R.string.home_add_event)) },
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
                    HomeList(
                        uiState = viewState.uiState,
                        onOshiFilterSelect = onOshiFilterSelect,
                        onEventClick = onEventClick,
                        onAddEventClick = onAddEventClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeList(
    uiState: UiState,
    onOshiFilterSelect: (String?) -> Unit,
    onEventClick: (String) -> Unit,
    onAddEventClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .widthIn(max = contentMaxWidth)
            .fillMaxSize(),
        contentPadding = PaddingValues(bottom = fabClearance),
    ) {
        if (uiState.oshis.isNotEmpty()) {
            item(key = "filters") {
                OshiFilterRow(
                    oshis = uiState.oshis,
                    selectedOshiId = uiState.selectedOshiId,
                    onSelect = onOshiFilterSelect,
                )
            }
        }

        val featured = uiState.featured
        if (featured != null) {
            item(key = "featured") {
                FeaturedEventCard(
                    event = featured,
                    onClick = { onEventClick(featured.id) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        if (!uiState.hasAnyEvent) {
            item(key = "empty") {
                EmptyEvents(onAddEventClick = onAddEventClick)
            }
        } else if (uiState.upcoming.isEmpty() && uiState.past.isEmpty()) {
            item(key = "emptyFiltered") {
                SectionMessage(
                    text = if (uiState.selectedOshiId != null) {
                        stringResource(R.string.home_empty_filtered)
                    } else {
                        stringResource(R.string.home_upcoming_empty)
                    },
                )
            }
        } else {
            item(key = "upcomingHeader") {
                SectionHeader(title = stringResource(R.string.home_upcoming_title))
            }
            if (uiState.upcoming.isEmpty()) {
                item(key = "upcomingEmpty") {
                    SectionMessage(text = stringResource(R.string.home_upcoming_empty))
                }
            }
            items(items = uiState.upcoming, key = { event -> "upcoming-${event.id}" }) { event ->
                HomeEventRow(event = event, onClick = { onEventClick(event.id) })
            }
            if (uiState.past.isNotEmpty()) {
                item(key = "pastHeader") {
                    SectionHeader(title = stringResource(R.string.home_past_title))
                }
                items(items = uiState.past, key = { event -> "past-${event.id}" }) { event ->
                    HomeEventRow(event = event, onClick = { onEventClick(event.id) })
                }
            }
        }
    }
}

/** 推しの絞り込み（M3 の `FilterChip`）。横にスクロールする。 */
@Composable
private fun OshiFilterRow(
    oshis: List<OshiUiModel>,
    selectedOshiId: String?,
    onSelect: (String?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedOshiId == null,
            onClick = { onSelect(null) },
            label = { Text(text = stringResource(R.string.home_filter_all)) },
            leadingIcon = if (selectedOshiId == null) {
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
        oshis.forEach { oshi ->
            FilterChip(
                selected = oshi.id == selectedOshiId,
                onClick = { onSelect(oshi.id) },
                label = { Text(text = oshi.name) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(oshi.color.color),
                    )
                },
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = OshikatsuTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
            .semantics { heading() },
    )
}

@Composable
private fun SectionMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = OshikatsuTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
    )
}

/** 予定が 1 件も無いときの案内。 */
@Composable
private fun EmptyEvents(onAddEventClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(UiR.drawable.ic_event),
            contentDescription = null,
            tint = OshikatsuTheme.colorScheme.brand,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.home_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = OshikatsuTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        FilledTonalButton(onClick = onAddEventClick) {
            Text(text = stringResource(R.string.home_add_event))
        }
    }
}

@Preview(widthDp = 400, heightDp = 860)
@Composable
private fun HomeContentPreview() {
    val lumina = OshiUiModel(id = "l", name = "LUMINA", initial = "L", color = OshiColorUiModel.PINK, photoPath = null)
    val minato = OshiUiModel(id = "m", name = "ミナト", initial = "ミ", color = OshiColorUiModel.BLUE, photoPath = null)
    val live = HomeEventUiModel(
        id = "1",
        title = "5th LIVE 東京公演",
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
    )
    val release = live.copy(
        id = "2",
        title = "1st写真集 発売日",
        type = EventTypeUiModel.RELEASE,
        oshiName = "ミナト",
        initial = "ミ",
        color = OshiColorUiModel.BLUE,
        occurrenceDate = LocalDate.of(2026, 10, 10),
        startTime = null,
        pinned = false,
        daysUntil = 3,
    )
    OshikatsuTheme {
        HomeContent(
            viewState = ViewState.Success(
                UiState(
                    oshis = listOf(lumina, minato),
                    selectedOshiId = null,
                    featured = live,
                    upcoming = listOf(release, live),
                    past = emptyList(),
                    hasAnyEvent = true,
                ),
            ),
            onOshiFilterSelect = {},
            onEventClick = {},
            onAddEventClick = {},
        )
    }
}
