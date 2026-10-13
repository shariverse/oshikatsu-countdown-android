package com.basekeita.oshikatsucountdown.feature.eventdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basekeita.oshikatsucountdown.data.definition.model.EventType
import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.model.OshiColor
import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import com.basekeita.oshikatsucountdown.data.definition.repository.EventRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.OshiRepository
import com.basekeita.oshikatsucountdown.domain.countdown.CalculateCountdownUseCase
import com.basekeita.oshikatsucountdown.domain.countdown.CountdownStatus
import com.basekeita.oshikatsucountdown.domain.countdown.EventCountdown
import com.basekeita.oshikatsucountdown.feature.eventdetail.uimodel.CountdownStatusUiModel
import com.basekeita.oshikatsucountdown.feature.eventdetail.uimodel.CountdownUiModel
import com.basekeita.oshikatsucountdown.feature.eventdetail.uimodel.EventDetailUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.EventTypeUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.WriteFailureUiModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.util.UUID

/** 1 秒。カウントダウンを進める間隔。 */
internal const val TICK_INTERVAL_MILLIS = 1_000L

/** 予定の詳細。開始までの残りを秒単位で数え、ピン留めの切り替えと削除を受ける。 */
@HiltViewModel(assistedFactory = EventDetailViewModel.Factory::class)
class EventDetailViewModel
@AssistedInject
constructor(
    @Assisted private val eventId: String,
    private val eventRepository: EventRepository,
    oshiRepository: OshiRepository,
    private val calculateCountdown: CalculateCountdownUseCase,
    private val clock: Clock,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(eventId: String): EventDetailViewModel
    }

    sealed interface ViewState {
        data object Loading : ViewState

        data class Error(
            val message: String,
        ) : ViewState

        /** 予定が無い（削除された、または id が読めない）。画面は前の画面に戻る。 */
        data object NotFound : ViewState

        data class Success(
            val uiState: UiState,
        ) : ViewState
    }

    /**
     * @property canClose 削除の途中でない。途中で戻ると `viewModelScope` ごと書き込みが止まるため、戻る操作を止める。
     * @property writeFailure ピン留め・削除に失敗した。Snackbar で知らせ終えたら `onWriteFailureShown` で戻す。
     */
    data class UiState(
        val event: EventDetailUiModel,
        val countdown: CountdownUiModel,
        val canClose: Boolean,
        val writeFailure: WriteFailureUiModel?,
    )

    private val eventUuid: UUID? = runCatching { UUID.fromString(eventId) }.getOrNull()

    private val deleting = MutableStateFlow(false)
    private val writeFailure = MutableStateFlow<WriteFailureUiModel?>(null)

    private val event: Flow<OshiEvent?> = eventRepository.events.map { events -> events.firstOrNull { it.id == eventUuid } }

    /** 秒の変わり目にそろえて今の時刻を流す。 */
    private val now: Flow<Instant> = flow {
        while (true) {
            val current = clock.instant()
            emit(current)
            delay(TICK_INTERVAL_MILLIS - current.toEpochMilli().mod(TICK_INTERVAL_MILLIS))
        }
    }

    val viewState: StateFlow<ViewState> =
        combine(event, oshiRepository.oshis, now, deleting, writeFailure) { currentEvent, oshis, currentTime, isDeleting, failure ->
            if (currentEvent == null) {
                ViewState.NotFound
            } else {
                val oshi = oshis.firstOrNull { it.id == currentEvent.oshiId }
                val countdown = calculateCountdown(currentEvent, currentTime, clock.zone)
                ViewState.Success(
                    UiState(
                        event = currentEvent.toUiModel(oshi, countdown),
                        countdown = countdown.toUiModel(),
                        canClose = !isDeleting,
                        writeFailure = failure,
                    ),
                )
            }
        }
            .catch { error -> emit(ViewState.Error(error.message.orEmpty())) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ViewState.Loading)

    fun onPinnedChange(pinned: Boolean) {
        val id = eventUuid ?: return
        viewModelScope.launch {
            try {
                eventRepository.setPinned(id, pinned)
            } catch (exception: IOException) {
                writeFailure.value = WriteFailureUiModel.SAVE
            }
        }
    }

    /**
     * 削除する。消し終えると `NotFound` になり、画面はそれを見て前の画面に戻る
     * （先に戻ると ViewModel ごと `viewModelScope` が止まり、書き込みが途中で切れるため）。
     */
    fun onDelete() {
        val id = eventUuid ?: return

        if (deleting.value) return

        deleting.value = true
        viewModelScope.launch {
            try {
                eventRepository.delete(id)
            } catch (exception: IOException) {
                writeFailure.value = WriteFailureUiModel.DELETE
                deleting.value = false
            }
        }
    }

    fun onWriteFailureShown() {
        writeFailure.value = null
    }
}

private fun OshiEvent.toUiModel(
    oshi: Oshi?,
    countdown: EventCountdown,
): EventDetailUiModel = EventDetailUiModel(
    title = title,
    type = type.toUiModel(),
    oshiName = oshi?.name,
    color = oshi?.color?.toUiModel() ?: OshiColorUiModel.BLACK,
    photoPath = oshi?.photoPath,
    occurrenceDate = countdown.occurrenceDate,
    startTime = startTime,
    venue = venue,
    repeatsYearly = repeatsYearly,
    pinned = pinned,
)

private fun EventCountdown.toUiModel(): CountdownUiModel {
    val totalSeconds = remaining.seconds
    return CountdownUiModel(
        status = when (status) {
            CountdownStatus.UPCOMING -> CountdownStatusUiModel.UPCOMING
            CountdownStatus.TODAY -> CountdownStatusUiModel.TODAY
            CountdownStatus.ENDED -> CountdownStatusUiModel.ENDED
        },
        days = totalSeconds / SECONDS_PER_DAY,
        hours = ((totalSeconds % SECONDS_PER_DAY) / SECONDS_PER_HOUR).toInt(),
        minutes = ((totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE).toInt(),
        seconds = (totalSeconds % SECONDS_PER_MINUTE).toInt(),
        daysUntil = daysUntil,
        progress = progress?.toFloat(),
        progressStartDate = progressStartDate,
    )
}

private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 60L * SECONDS_PER_MINUTE
private const val SECONDS_PER_DAY = 24L * SECONDS_PER_HOUR

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
