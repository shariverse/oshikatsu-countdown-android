package com.basekeita.oshikatsucountdown

import com.basekeita.oshikatsucountdown.data.definition.model.ReminderSettings
import com.basekeita.oshikatsucountdown.data.definition.model.ReminderTiming
import com.basekeita.oshikatsucountdown.data.definition.model.UserSettings
import com.basekeita.oshikatsucountdown.data.definition.repository.UserSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalTime

/** 予定を保存した直後に通知の許可を求めるかの判断。 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeUserSettingsRepository(
        initial: UserSettings,
    ) : UserSettingsRepository {
        override val settings = MutableStateFlow(initial)

        override suspend fun setShowPastEvents(show: Boolean) = Unit

        override suspend fun setOnboardingCompleted(completed: Boolean) = Unit

        override suspend fun setRemindersEnabled(enabled: Boolean) = Unit

        override suspend fun setReminderTimingEnabled(
            timing: ReminderTiming,
            enabled: Boolean,
        ) = Unit

        override suspend fun setReminderTimeOfDay(time: LocalTime) = Unit

        override suspend fun setNotificationPermissionRequested(requested: Boolean) {
            settings.value = settings.value.copy(notificationPermissionRequested = requested)
        }
    }

    private fun MainViewModel.asksNotificationPermissionOnSave(): Boolean? = (viewState.value as? MainViewModel.ViewState.Success)?.uiState?.asksNotificationPermissionOnSave

    @Test
    fun `まだ一度も求めていなければ、保存した直後に求める`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = MainViewModel(FakeUserSettingsRepository(UserSettings()))

        assertEquals(true, viewModel.asksNotificationPermissionOnSave())
    }

    @Test
    fun `予定を通知するがOFFでも、当日のカウントダウンのために求める`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = MainViewModel(FakeUserSettingsRepository(UserSettings(reminders = ReminderSettings(enabled = false))))

        assertEquals(true, viewModel.asksNotificationPermissionOnSave())
    }

    @Test
    fun `一度求めたら、次からは求めない`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeUserSettingsRepository(UserSettings())
        val viewModel = MainViewModel(repository)

        viewModel.onNotificationPermissionRequested()
        runCurrent()

        assertEquals(true, repository.settings.value.notificationPermissionRequested)
        assertEquals(false, viewModel.asksNotificationPermissionOnSave())

        val requested = MainViewModel(FakeUserSettingsRepository(UserSettings(notificationPermissionRequested = true)))
        assertEquals(false, requested.asksNotificationPermissionOnSave())
    }
}
