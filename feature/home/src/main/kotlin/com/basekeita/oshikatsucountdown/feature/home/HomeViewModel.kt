package com.basekeita.oshikatsucountdown.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basekeita.oshikatsucountdown.data.definition.model.EventType
import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.model.OshiColor
import com.basekeita.oshikatsucountdown.data.definition.model.firstCharacter
import com.basekeita.oshikatsucountdown.data.definition.repository.EventRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.OshiRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.UserSettingsRepository
import com.basekeita.oshikatsucountdown.domain.countdown.BuildEventScheduleUseCase
import com.basekeita.oshikatsucountdown.domain.countdown.CountdownStatus
import com.basekeita.oshikatsucountdown.domain.countdown.ScheduledEvent
import com.basekeita.oshikatsucountdown.feature.home.uimodel.EventStatusUiModel
import com.basekeita.oshikatsucountdown.feature.home.uimodel.HomeEventUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.EventTypeUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** 日付が変わったことに気付くまでの間隔。 */
internal const val TODAY_CHECK_INTERVAL_MILLIS = 60_000L

/**
 * ホーム。推しで絞り込み、先頭の予定と、これからの予定・終わった予定を出す。
 * 並びは `BuildEventScheduleUseCase` が決め、日付が変わったら並べ直す。
 */
@HiltViewModel
class HomeViewModel
@Inject
constructor(
    eventRepository: EventRepository,
    oshiRepository: OshiRepository,
    userSettingsRepository: UserSettingsRepository,
    private val buildEventSchedule: BuildEventScheduleUseCase,
    private val clock: Clock,
) : ViewModel() {
    sealed interface ViewState {
        data object Loading : ViewState

        data class Error(
            val message: String,
        ) : ViewState

        data class Success(
            val uiState: UiState,
        ) : ViewState
    }

    /**
     * @property oshis 絞り込みのチップに出す推し。
     * @property selectedOshiId 絞り込み中の推し。すべてのときは null。
     * @property hasAnyEvent 絞り込む前の予定が 1 件でもある。
     */
    data class UiState(
        val oshis: List<OshiUiModel>,
        val selectedOshiId: String?,
        val featured: HomeEventUiModel?,
        val upcoming: List<HomeEventUiModel>,
        val past: List<HomeEventUiModel>,
        val hasAnyEvent: Boolean,
    )

    private val selectedOshiId = MutableStateFlow<String?>(null)

    private val today: Flow<LocalDate> = flow {
        while (true) {
            emit(LocalDate.now(clock))
            delay(TODAY_CHECK_INTERVAL_MILLIS)
        }
    }.distinctUntilChanged()

    val viewState: StateFlow<ViewState> =
        combine(
            eventRepository.events,
            oshiRepository.oshis,
            userSettingsRepository.settings,
            selectedOshiId,
            today,
        ) { events, oshis, settings, selectedId, currentDate ->
            val oshiById = oshis.associateBy { it.id.toString() }
            // 選んでいた推しが消えたら「すべて」に戻す
            val validSelectedId = selectedId?.takeIf { it in oshiById }
            val filteredEvents = if (validSelectedId == null) {
                events
            } else {
                events.filter { it.oshiId?.toString() == validSelectedId }
            }
            val schedule = buildEventSchedule(filteredEvents, currentDate)
            val toUiModel = { scheduled: ScheduledEvent -> scheduled.toUiModel(oshiById[scheduled.event.oshiId?.toString()]) }
            val uiState = UiState(
                oshis = oshis.map { it.toUiModel() },
                selectedOshiId = validSelectedId,
                featured = schedule.featured?.let(toUiModel),
                upcoming = schedule.upcoming.map(toUiModel),
                past = if (settings.showPastEvents) {
                    schedule.past.map(toUiModel)
                } else {
                    emptyList()
                },
                hasAnyEvent = events.isNotEmpty(),
            )
            ViewState.Success(uiState) as ViewState
        }
            .catch { error -> emit(ViewState.Error(error.message.orEmpty())) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ViewState.Loading)

    /** 推しで絞り込む。null ですべて。 */
    fun onOshiFilterSelect(oshiId: String?) {
        selectedOshiId.value = oshiId
    }
}

private fun ScheduledEvent.toUiModel(oshi: Oshi?): HomeEventUiModel = HomeEventUiModel(
    id = event.id.toString(),
    title = event.title,
    type = event.type.toUiModel(),
    oshiName = oshi?.name,
    initial = (oshi?.name ?: event.title).firstCharacter(),
    color = oshi?.color?.toUiModel() ?: NO_OSHI_COLOR,
    photoPath = oshi?.photoPath,
    occurrenceDate = occurrenceDate,
    startTime = event.startTime,
    repeatsYearly = event.repeatsYearly,
    pinned = event.pinned,
    daysUntil = daysUntil,
    status = when (status) {
        CountdownStatus.UPCOMING -> EventStatusUiModel.UPCOMING
        CountdownStatus.TODAY -> EventStatusUiModel.TODAY
        CountdownStatus.ENDED -> EventStatusUiModel.ENDED
    },
)

/** 推しなしの予定の色。どの推し色とも紛れない無彩色。 */
private val NO_OSHI_COLOR = OshiColorUiModel.BLACK

private fun Oshi.toUiModel(): OshiUiModel = OshiUiModel(
    id = id.toString(),
    name = name,
    initial = name.firstCharacter(),
    color = color.toUiModel(),
    photoPath = photoPath,
)

private fun OshiColor.toUiModel(): OshiColorUiModel = when (this) {
    OshiColor.PINK -> OshiColorUiModel.PINK
    OshiColor.RED -> OshiColorUiModel.RED
    OshiColor.ORANGE -> OshiColorUiModel.ORANGE
    OshiColor.YELLOW -> OshiColorUiModel.YELLOW
    OshiColor.GREEN -> OshiColorUiModel.GREEN
    OshiColor.MINT -> OshiColorUiModel.MINT
    OshiColor.BLUE -> OshiColorUiModel.BLUE
    OshiColor.NAVY -> OshiColorUiModel.NAVY
    OshiColor.PURPLE -> OshiColorUiModel.PURPLE
    OshiColor.LAVENDER -> OshiColorUiModel.LAVENDER
    OshiColor.BROWN -> OshiColorUiModel.BROWN
    OshiColor.BLACK -> OshiColorUiModel.BLACK
}

private fun EventType.toUiModel(): EventTypeUiModel = when (this) {
    EventType.LIVE -> EventTypeUiModel.LIVE
    EventType.BIRTHDAY -> EventTypeUiModel.BIRTHDAY
    EventType.RELEASE -> EventTypeUiModel.RELEASE
    EventType.STREAM -> EventTypeUiModel.STREAM
    EventType.OTHER -> EventTypeUiModel.OTHER
}
