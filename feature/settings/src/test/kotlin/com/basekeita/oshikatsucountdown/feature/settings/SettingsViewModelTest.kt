package com.basekeita.oshikatsucountdown.feature.settings

import com.basekeita.oshikatsucountdown.data.definition.model.UserSettings
import com.basekeita.oshikatsucountdown.data.definition.repository.UserSettingsRepository
import com.basekeita.oshikatsucountdown.feature.settings.SettingsViewModel.ViewState
import com.basekeita.oshikatsucountdown.ui.uimodel.WriteFailureUiModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeUserSettingsRepository : UserSettingsRepository {
        override val settings = MutableStateFlow(UserSettings())

        override suspend fun setShowPastEvents(show: Boolean) {
            settings.value = settings.value.copy(showPastEvents = show)
        }

        override suspend fun setOnboardingCompleted(completed: Boolean) = Unit
    }

    @Test
    fun `終わった予定の表示を保存し、アプリのバージョンと一緒に出す`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeUserSettingsRepository()
        val viewModel = SettingsViewModel(repository, appVersionName = "1.2.3")
        backgroundScope.launch { viewModel.viewState.collect {} }
        runCurrent()
        assertEquals(ViewState.Success(SettingsViewModel.UiState(showPastEvents = false, appVersionName = "1.2.3", writeFailure = null)), viewModel.viewState.value)

        viewModel.onShowPastEventsChange(true)
        runCurrent()

        assertEquals(true, repository.settings.value.showPastEvents)
        assertEquals(ViewState.Success(SettingsViewModel.UiState(showPastEvents = true, appVersionName = "1.2.3", writeFailure = null)), viewModel.viewState.value)
    }

    @Test
    fun `設定を読めなければErrorを出す`() = runTest(mainDispatcherRule.dispatcher) {
        val failingRepository = object : UserSettingsRepository {
            override val settings: Flow<UserSettings> = flow { throw IllegalStateException("読めません") }

            override suspend fun setShowPastEvents(show: Boolean) = Unit

            override suspend fun setOnboardingCompleted(completed: Boolean) = Unit
        }
        val viewModel = SettingsViewModel(failingRepository, appVersionName = "1.2.3")
        backgroundScope.launch { viewModel.viewState.collect {} }
        runCurrent()

        assertEquals(ViewState.Error("読めません"), viewModel.viewState.value)
    }

    @Test
    fun `保存に失敗したら知らせて前の値のまま出す`() = runTest(mainDispatcherRule.dispatcher) {
        val failingRepository = object : UserSettingsRepository {
            override val settings = MutableStateFlow(UserSettings())

            override suspend fun setShowPastEvents(show: Boolean): Unit = throw IOException("disk full")

            override suspend fun setOnboardingCompleted(completed: Boolean) = Unit
        }
        val viewModel = SettingsViewModel(failingRepository, appVersionName = "1.2.3")
        backgroundScope.launch { viewModel.viewState.collect {} }
        runCurrent()

        viewModel.onShowPastEventsChange(true)
        runCurrent()

        assertEquals(
            ViewState.Success(SettingsViewModel.UiState(showPastEvents = false, appVersionName = "1.2.3", writeFailure = WriteFailureUiModel.SAVE)),
            viewModel.viewState.value,
        )
    }
}
