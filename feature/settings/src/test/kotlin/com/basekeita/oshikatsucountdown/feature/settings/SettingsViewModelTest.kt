package com.basekeita.oshikatsucountdown.feature.settings

import com.basekeita.oshikatsucountdown.data.definition.model.ReminderTiming
import com.basekeita.oshikatsucountdown.data.definition.model.UserSettings
import com.basekeita.oshikatsucountdown.data.definition.repository.UserSettingsRepository
import com.basekeita.oshikatsucountdown.feature.settings.SettingsViewModel.UiState
import com.basekeita.oshikatsucountdown.feature.settings.uimodel.NotificationPermissionUiModel
import com.basekeita.oshikatsucountdown.feature.settings.uimodel.ReminderSettingsUiModel
import com.basekeita.oshikatsucountdown.feature.settings.uimodel.ReminderTimingUiModel
import com.basekeita.oshikatsucountdown.feature.settings.uimodel.notificationPermissionOf
import com.basekeita.oshikatsucountdown.ui.uimodel.WriteFailureUiModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private open class FakeUserSettingsRepository(
        initial: UserSettings = UserSettings(),
    ) : UserSettingsRepository {
        override val settings: Flow<UserSettings> get() = state
        val state = MutableStateFlow(initial)

        override suspend fun setShowPastEvents(show: Boolean) {
            state.value = state.value.copy(showPastEvents = show)
        }

        override suspend fun setOnboardingCompleted(completed: Boolean) = Unit

        override suspend fun setRemindersEnabled(enabled: Boolean) {
            state.value = state.value.copy(reminders = state.value.reminders.copy(enabled = enabled))
        }

        override suspend fun setReminderTimingEnabled(
            timing: ReminderTiming,
            enabled: Boolean,
        ) {
            val timings = state.value.reminders.timings
            val updated = if (enabled) {
                timings + timing
            } else {
                timings - timing
            }
            state.value = state.value.copy(reminders = state.value.reminders.copy(timings = updated))
        }

        override suspend fun setReminderTimeOfDay(time: LocalTime) {
            state.value = state.value.copy(reminders = state.value.reminders.copy(timeOfDay = time))
        }

        override suspend fun setNotificationPermissionRequested(requested: Boolean) {
            state.value = state.value.copy(notificationPermissionRequested = requested)
        }
    }

    private fun TestScope.viewModel(repository: UserSettingsRepository): SettingsViewModel {
        val viewModel = SettingsViewModel(repository, appVersionName = "1.2.3")
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        return viewModel
    }

    @Test
    fun `終わった予定の表示を保存し、アプリのバージョンと一緒に出す`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeUserSettingsRepository()
        val viewModel = viewModel(repository)
        assertEquals(UiState(isLoading = false, showPastEvents = false, appVersionName = "1.2.3"), viewModel.uiState.value)

        viewModel.onShowPastEventsChange(true)
        runCurrent()

        assertEquals(true, repository.state.value.showPastEvents)
        assertEquals(UiState(isLoading = false, showPastEvents = true, appVersionName = "1.2.3"), viewModel.uiState.value)
    }

    @Test
    fun `設定を読めなければ読めなかったことを出す`() = runTest(mainDispatcherRule.dispatcher) {
        val failingRepository = object : FakeUserSettingsRepository() {
            override val settings: Flow<UserSettings> = flow { throw IllegalStateException("読めません") }
        }

        val viewModel = viewModel(failingRepository)

        assertEquals(UiState(isLoading = false, isLoadFailed = true, appVersionName = "1.2.3"), viewModel.uiState.value)
    }

    @Test
    fun `保存に失敗したら知らせて前の値のまま出す`() = runTest(mainDispatcherRule.dispatcher) {
        val failingRepository = object : FakeUserSettingsRepository() {
            override suspend fun setShowPastEvents(show: Boolean): Unit = throw IOException("disk full")
        }
        val viewModel = viewModel(failingRepository)

        viewModel.onShowPastEventsChange(true)
        runCurrent()

        assertEquals(
            UiState(isLoading = false, showPastEvents = false, appVersionName = "1.2.3", writeFailure = WriteFailureUiModel.SAVE),
            viewModel.uiState.value,
        )

        viewModel.onWriteFailureShown()
        runCurrent()

        assertEquals(null, viewModel.uiState.value.writeFailure)
    }

    @Test
    fun `通知の設定を保存して出す`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeUserSettingsRepository()
        val viewModel = viewModel(repository)
        assertEquals(ReminderSettingsUiModel(), viewModel.uiState.value.reminders)

        viewModel.onReminderTimingChange(ReminderTimingUiModel.THIRTY_DAYS_BEFORE, false)
        viewModel.onReminderTimeChange(LocalTime.of(20, 30))
        viewModel.onRemindersEnabledChange(false)
        runCurrent()

        assertEquals(
            ReminderSettingsUiModel(
                enabled = false,
                enabledTimings = ReminderTimingUiModel.entries.toSet() - ReminderTimingUiModel.THIRTY_DAYS_BEFORE,
                timeOfDay = LocalTime.of(20, 30),
            ),
            viewModel.uiState.value.reminders,
        )
    }

    @Test
    fun `通知をONにしたとき、まだ許可を求めていなければ求め、閉じたら求めた印を残す`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeUserSettingsRepository(UserSettings(reminders = UserSettings().reminders.copy(enabled = false)))
        val viewModel = viewModel(repository)
        viewModel.onNotificationPermissionCheck(granted = false, canRequest = true, exactAlarmsAllowed = true)
        runCurrent()
        assertEquals(NotificationPermissionUiModel.NOT_REQUESTED, viewModel.uiState.value.notificationPermission)

        viewModel.onRemindersEnabledChange(true)
        runCurrent()

        assertTrue(viewModel.uiState.value.isRequestingNotificationPermission)

        viewModel.onNotificationPermissionResult(granted = false)
        runCurrent()

        assertFalse(viewModel.uiState.value.isRequestingNotificationPermission)
        assertTrue(repository.state.value.notificationPermissionRequested)
        assertEquals(NotificationPermissionUiModel.DENIED, viewModel.uiState.value.notificationPermission)
    }

    @Test
    fun `一度求めたか、許可されていれば、通知をONにしても求めない`() = runTest(mainDispatcherRule.dispatcher) {
        val requested = FakeUserSettingsRepository(UserSettings(notificationPermissionRequested = true))
        val requestedViewModel = viewModel(requested)
        requestedViewModel.onNotificationPermissionCheck(granted = false, canRequest = true, exactAlarmsAllowed = true)
        requestedViewModel.onRemindersEnabledChange(true)
        runCurrent()

        assertFalse(requestedViewModel.uiState.value.isRequestingNotificationPermission)

        val granted = FakeUserSettingsRepository()
        val grantedViewModel = viewModel(granted)
        grantedViewModel.onNotificationPermissionCheck(granted = true, canRequest = true, exactAlarmsAllowed = true)
        grantedViewModel.onRemindersEnabledChange(true)
        runCurrent()

        assertFalse(grantedViewModel.uiState.value.isRequestingNotificationPermission)
        assertEquals(NotificationPermissionUiModel.GRANTED, grantedViewModel.uiState.value.notificationPermission)
    }

    @Test
    fun `許可の状態は、許可・まだ求めていない・許可されていない・アラームを使えないに分ける`() {
        assertEquals(NotificationPermissionUiModel.GRANTED, notificationPermissionOf(granted = true, canRequest = true, requested = false, exactAlarmsAllowed = true))
        assertEquals(NotificationPermissionUiModel.NOT_REQUESTED, notificationPermissionOf(granted = false, canRequest = true, requested = false, exactAlarmsAllowed = true))
        assertEquals(NotificationPermissionUiModel.DENIED, notificationPermissionOf(granted = false, canRequest = true, requested = true, exactAlarmsAllowed = true))
        // 許可のダイアログが無い版（Android 12 以前）では、端末の設定で ON にしてもらう
        assertEquals(NotificationPermissionUiModel.DENIED, notificationPermissionOf(granted = false, canRequest = false, requested = false, exactAlarmsAllowed = true))
        // Android 12 で正確なアラームを使えなければ、アラームの画面を案内する。通知を出せなければ、そちらを先に案内する
        assertEquals(NotificationPermissionUiModel.EXACT_ALARM_DENIED, notificationPermissionOf(granted = true, canRequest = false, requested = false, exactAlarmsAllowed = false))
        assertEquals(NotificationPermissionUiModel.DENIED, notificationPermissionOf(granted = false, canRequest = false, requested = false, exactAlarmsAllowed = false))
        assertEquals(NotificationPermissionUiModel.NOT_REQUESTED, notificationPermissionOf(granted = false, canRequest = true, requested = false, exactAlarmsAllowed = false))
    }

    @Test
    fun `端末から許可の状態を読む前は、案内を出さない`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel(FakeUserSettingsRepository())

        assertEquals(NotificationPermissionUiModel.GRANTED, viewModel.uiState.value.notificationPermission)
    }

    @Test
    fun `通知を許可するを押すと求め、許可されたら案内を消す`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeUserSettingsRepository()
        val viewModel = viewModel(repository)
        viewModel.onNotificationPermissionCheck(granted = false, canRequest = true, exactAlarmsAllowed = true)

        viewModel.onAllowNotificationsClick()
        runCurrent()

        assertTrue(viewModel.uiState.value.isRequestingNotificationPermission)

        viewModel.onNotificationPermissionResult(granted = true)
        runCurrent()

        assertFalse(viewModel.uiState.value.isRequestingNotificationPermission)
        assertTrue(repository.state.value.notificationPermissionRequested)
        assertEquals(NotificationPermissionUiModel.GRANTED, viewModel.uiState.value.notificationPermission)
    }

    @Test
    fun `許可のダイアログが無い版では、通知をONにしても求めない`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeUserSettingsRepository(UserSettings(reminders = UserSettings().reminders.copy(enabled = false)))
        val viewModel = viewModel(repository)
        viewModel.onNotificationPermissionCheck(granted = false, canRequest = false, exactAlarmsAllowed = true)

        viewModel.onRemindersEnabledChange(true)
        runCurrent()

        assertFalse(viewModel.uiState.value.isRequestingNotificationPermission)
        assertEquals(NotificationPermissionUiModel.DENIED, viewModel.uiState.value.notificationPermission)
    }

    @Test
    fun `通知の設定の保存に失敗したら知らせる`() = runTest(mainDispatcherRule.dispatcher) {
        val failingRepository = object : FakeUserSettingsRepository() {
            override suspend fun setRemindersEnabled(enabled: Boolean): Unit = throw IOException("disk full")

            override suspend fun setReminderTimingEnabled(
                timing: ReminderTiming,
                enabled: Boolean,
            ): Unit = throw IOException("disk full")

            override suspend fun setReminderTimeOfDay(time: LocalTime): Unit = throw IOException("disk full")
        }
        val viewModel = viewModel(failingRepository)

        listOf<() -> Unit>(
            { viewModel.onRemindersEnabledChange(false) },
            { viewModel.onReminderTimingChange(ReminderTimingUiModel.DAY_BEFORE, false) },
            { viewModel.onReminderTimeChange(LocalTime.of(20, 0)) },
        ).forEach { change ->
            change()
            runCurrent()

            assertEquals(WriteFailureUiModel.SAVE, viewModel.uiState.value.writeFailure)
            assertEquals(ReminderSettingsUiModel(), viewModel.uiState.value.reminders)

            viewModel.onWriteFailureShown()
            runCurrent()
        }
    }

    @Test
    fun `許可を求めた印を残せなくても、保存できなかったとは知らせない`() = runTest(mainDispatcherRule.dispatcher) {
        val failingRepository = object : FakeUserSettingsRepository() {
            override suspend fun setNotificationPermissionRequested(requested: Boolean): Unit = throw IOException("disk full")
        }
        val viewModel = viewModel(failingRepository)

        viewModel.onNotificationPermissionResult(granted = false)
        runCurrent()

        assertEquals(null, viewModel.uiState.value.writeFailure)
    }

    @Test
    fun `正確なアラームを使えなければ、アラームの画面を案内する`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel(FakeUserSettingsRepository())

        viewModel.onNotificationPermissionCheck(granted = true, canRequest = false, exactAlarmsAllowed = false)
        runCurrent()

        assertEquals(NotificationPermissionUiModel.EXACT_ALARM_DENIED, viewModel.uiState.value.notificationPermission)
    }
}
