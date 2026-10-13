package com.basekeita.oshikatsucountdown.feature.oshi.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basekeita.oshikatsucountdown.data.definition.repository.EventRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.OshiRepository
import com.basekeita.oshikatsucountdown.feature.oshi.uimodel.OshiListItemUiModel
import com.basekeita.oshikatsucountdown.feature.oshi.uimodel.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** 推しの一覧。登録した順に、結び付いた予定の数を添えて出す。 */
@HiltViewModel
class OshiListViewModel
@Inject
constructor(
    oshiRepository: OshiRepository,
    eventRepository: EventRepository,
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

    data class UiState(
        val oshis: List<OshiListItemUiModel>,
    )

    val viewState: StateFlow<ViewState> =
        combine(oshiRepository.oshis, eventRepository.events) { oshis, events ->
            val eventCounts = events.groupingBy { it.oshiId }.eachCount()
            val items = oshis.map { oshi -> OshiListItemUiModel(oshi = oshi.toUiModel(), eventCount = eventCounts[oshi.id] ?: 0) }
            ViewState.Success(UiState(oshis = items)) as ViewState
        }
            .catch { error -> emit(ViewState.Error(error.message.orEmpty())) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ViewState.Loading)
}
