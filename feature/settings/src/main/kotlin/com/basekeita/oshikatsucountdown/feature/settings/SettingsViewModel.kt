package com.basekeita.oshikatsucountdown.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basekeita.oshikatsucountdown.data.definition.repository.UserSettingsRepository
import com.basekeita.oshikatsucountdown.ui.uimodel.WriteFailureUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/** 設定。値は保存と読み込みだけなので Repository を直接呼ぶ。 */
@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val userSettingsRepository: UserSettingsRepository,
    @param:AppVersionName private val appVersionName: String,
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

    /** @property writeFailure 設定の保存に失敗した。Snackbar で知らせ終えたら `onWriteFailureShown` で戻す。 */
    data class UiState(
        val showPastEvents: Boolean,
        val appVersionName: String,
        val writeFailure: WriteFailureUiModel?,
    )

    private val writeFailure = MutableStateFlow<WriteFailureUiModel?>(null)

    val viewState: StateFlow<ViewState> =
        combine(userSettingsRepository.settings, writeFailure) { settings, failure ->
            ViewState.Success(
                UiState(showPastEvents = settings.showPastEvents, appVersionName = appVersionName, writeFailure = failure),
            ) as ViewState
        }
            .catch { error -> emit(ViewState.Error(error.message.orEmpty())) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ViewState.Loading)

    fun onShowPastEventsChange(show: Boolean) {
        viewModelScope.launch {
            try {
                userSettingsRepository.setShowPastEvents(show)
            } catch (exception: IOException) {
                writeFailure.value = WriteFailureUiModel.SAVE
            }
        }
    }

    fun onWriteFailureShown() {
        writeFailure.value = null
    }
}
