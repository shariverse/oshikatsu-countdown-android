package com.basekeita.oshikatsucountdown.feature.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basekeita.oshikatsucountdown.data.definition.repository.EventRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.OshiRepository
import com.basekeita.oshikatsucountdown.domain.countdown.BuildEventScheduleUseCase
import com.basekeita.oshikatsucountdown.feature.widget.uimodel.WidgetEventUiModel
import com.basekeita.oshikatsucountdown.feature.widget.uimodel.upcomingWidgetEvents
import com.basekeita.oshikatsucountdown.ui.uimodel.WriteFailureUiModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Clock
import java.time.LocalDate

/**
 * 1 件のウィジェットに出す予定を選ぶ。当日とこれからの予定を近い順に出し、選んだら保存してウィジェットを描き直す。
 * 保存し終えると `isFinished` を立て、画面はそれを見て閉じる（先に閉じると `viewModelScope` が止まり、書き込みが途中で切れるため）。
 */
@HiltViewModel(assistedFactory = WidgetConfigureViewModel.Factory::class)
class WidgetConfigureViewModel
@AssistedInject
constructor(
    @Assisted private val appWidgetId: Int,
    eventRepository: EventRepository,
    oshiRepository: OshiRepository,
    buildEventSchedule: BuildEventScheduleUseCase,
    private val selectionStore: WidgetEventSelectionStore,
    clock: Clock,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(appWidgetId: Int): WidgetConfigureViewModel
    }

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
     * @property selectedEventId 今ウィジェットに出している予定。置いたばかりのときは null。
     * @property canClose 保存中でない。保存中に閉じると `viewModelScope` ごと書き込みが止まるため、閉じる操作と戻る操作を止める。
     * @property isFinished 保存し終えた。
     * @property writeFailure 保存に失敗した。Snackbar で知らせ終えたら `onWriteFailureShown` で戻す。
     */
    data class UiState(
        val events: List<WidgetEventUiModel>,
        val selectedEventId: String?,
        val canClose: Boolean,
        val isFinished: Boolean,
        val writeFailure: WriteFailureUiModel?,
    )

    /** `combine` が型付きで受ける Flow は 5 つまでなので、保存の進み具合を 1 つにまとめる。 */
    private data class SaveProgress(
        val isSaving: Boolean,
        val isFinished: Boolean,
        val failure: WriteFailureUiModel?,
    )

    private val today: LocalDate = LocalDate.now(clock)

    private val savedEventId = flow { emit(selectionStore.load(appWidgetId)) }

    private val saving = MutableStateFlow(false)
    private val finished = MutableStateFlow(false)
    private val writeFailure = MutableStateFlow<WriteFailureUiModel?>(null)
    private val saveProgress = combine(saving, finished, writeFailure, ::SaveProgress)

    val viewState: StateFlow<ViewState> =
        combine(eventRepository.events, oshiRepository.oshis, savedEventId, saveProgress) { events, oshis, selectedId, progress ->
            val uiState = UiState(
                events = buildEventSchedule(events, today).upcomingWidgetEvents(oshis),
                selectedEventId = selectedId,
                canClose = !progress.isSaving,
                isFinished = progress.isFinished,
                writeFailure = progress.failure,
            )
            ViewState.Success(uiState) as ViewState
        }
            .catch { error -> emit(ViewState.Error(error.message.orEmpty())) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ViewState.Loading)

    fun onEventSelect(eventId: String) {
        if (saving.value || finished.value) return

        saving.value = true
        viewModelScope.launch {
            try {
                selectionStore.save(appWidgetId, eventId)
                finished.value = true
            } catch (exception: IOException) {
                writeFailure.value = WriteFailureUiModel.SAVE
                saving.value = false
            }
        }
    }

    fun onWriteFailureShown() {
        writeFailure.value = null
    }
}
