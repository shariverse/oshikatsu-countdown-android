package com.basekeita.oshikatsucountdown

import com.basekeita.oshikatsucountdown.data.definition.model.ReminderSettings
import com.basekeita.oshikatsucountdown.data.definition.model.ReminderTiming
import com.basekeita.oshikatsucountdown.data.definition.model.UserSettings
import com.basekeita.oshikatsucountdown.data.definition.repository.UserSettingsRepository
import com.basekeita.oshikatsucountdown.navigation.EventDetailRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.LocalTime

/** 導線を出すかどうか、外から開く画面、予定を保存した直後に通知の許可を求めるか。 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeUserSettingsRepository(
        initial: UserSettings,
        private val failsToWrite: Boolean = false,
    ) : UserSettingsRepository {
        override val settings = MutableStateFlow(initial)

        override suspend fun setShowPastEvents(show: Boolean) = Unit

        override suspend fun setOnboardingCompleted(completed: Boolean) {
            if (failsToWrite) throw IOException("disk full")
            settings.value = settings.value.copy(onboardingCompleted = completed)
        }

        override suspend fun setRemindersEnabled(enabled: Boolean) = Unit

        override suspend fun setReminderTimingEnabled(
            timing: ReminderTiming,
            enabled: Boolean,
        ) = Unit

        override suspend fun setReminderTimeOfDay(time: LocalTime) = Unit

        override suspend fun setNotificationPermissionRequested(requested: Boolean) {
            if (failsToWrite) throw IOException("disk full")
            settings.value = settings.value.copy(notificationPermissionRequested = requested)
        }
    }

    @Test
    fun `読めたら、読み込み中を終えて導線を終えたかを出す`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = MainViewModel(FakeUserSettingsRepository(UserSettings(onboardingCompleted = true)))

        assertEquals(MainViewModel.UiState(isLoading = false, onboardingCompleted = true), viewModel.uiState.value)
    }

    @Test
    fun `設定を読めなければ、読めなかったことを出す`() = runTest(mainDispatcherRule.dispatcher) {
        val failingRepository = object : UserSettingsRepository by FakeUserSettingsRepository(UserSettings()) {
            override val settings: Flow<UserSettings> = flow { throw IllegalStateException("読めません") }
        }

        val viewModel = MainViewModel(failingRepository)

        assertEquals(MainViewModel.UiState(isLoading = false, isLoadFailed = true), viewModel.uiState.value)
    }

    @Test
    fun `導線を終えたことを保存できなくても、このプロセスの間は終えたものとして扱う`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = MainViewModel(FakeUserSettingsRepository(UserSettings(), failsToWrite = true))

        viewModel.onOnboardingFinish()
        runCurrent()

        assertEquals(true, viewModel.uiState.value.onboardingCompleted)
    }

    @Test
    fun `外から頼まれた画面を出し、開いたら戻す`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = MainViewModel(FakeUserSettingsRepository(UserSettings()))

        viewModel.onLaunchRequest(EventDetailRoute("event-id"))
        runCurrent()
        assertEquals(EventDetailRoute("event-id"), viewModel.uiState.value.launchRoute)

        viewModel.onLaunchRouteHandle()
        runCurrent()
        assertEquals(null, viewModel.uiState.value.launchRoute)
    }

    @Test
    fun `まだ一度も求めていなければ、保存した直後に求める`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = MainViewModel(FakeUserSettingsRepository(UserSettings()))

        viewModel.onEventSave(canRequestNotificationPermission = true, notificationsGranted = false)
        runCurrent()

        assertEquals(true, viewModel.uiState.value.isRequestingNotificationPermission)
    }

    @Test
    fun `予定を通知するがOFFでも、当日のカウントダウンのために求める`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = MainViewModel(FakeUserSettingsRepository(UserSettings(reminders = ReminderSettings(enabled = false))))

        viewModel.onEventSave(canRequestNotificationPermission = true, notificationsGranted = false)
        runCurrent()

        assertEquals(true, viewModel.uiState.value.isRequestingNotificationPermission)
    }

    @Test
    fun `ダイアログを閉じたら求めた印を残し、次からは求めない`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeUserSettingsRepository(UserSettings())
        val viewModel = MainViewModel(repository)
        viewModel.onEventSave(canRequestNotificationPermission = true, notificationsGranted = false)
        runCurrent()

        viewModel.onNotificationPermissionResult()
        runCurrent()

        assertEquals(true, repository.settings.value.notificationPermissionRequested)
        assertEquals(false, viewModel.uiState.value.isRequestingNotificationPermission)

        viewModel.onEventSave(canRequestNotificationPermission = true, notificationsGranted = false)
        runCurrent()
        assertEquals(false, viewModel.uiState.value.isRequestingNotificationPermission)
    }

    @Test
    fun `求めた印を残せなくても、ダイアログは閉じたことにする`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = MainViewModel(FakeUserSettingsRepository(UserSettings(), failsToWrite = true))
        viewModel.onEventSave(canRequestNotificationPermission = true, notificationsGranted = false)
        runCurrent()

        viewModel.onNotificationPermissionResult()
        runCurrent()

        assertEquals(false, viewModel.uiState.value.isRequestingNotificationPermission)
    }

    @Test
    fun `許可を求めるかは、版・許可・印・求めている途中で決まる`() {
        assertEquals(true, asksNotificationPermissionOnSave(canRequest = true, granted = false, requested = false, requesting = false))
        // Android 12 以前は許可のダイアログが無い
        assertEquals(false, asksNotificationPermissionOnSave(canRequest = false, granted = false, requested = false, requesting = false))
        // もう許可されている
        assertEquals(false, asksNotificationPermissionOnSave(canRequest = true, granted = true, requested = false, requesting = false))
        // 一度求めた
        assertEquals(false, asksNotificationPermissionOnSave(canRequest = true, granted = false, requested = true, requesting = false))
        // 求めている途中に続けて保存した
        assertEquals(false, asksNotificationPermissionOnSave(canRequest = true, granted = false, requested = false, requesting = true))
    }
}
