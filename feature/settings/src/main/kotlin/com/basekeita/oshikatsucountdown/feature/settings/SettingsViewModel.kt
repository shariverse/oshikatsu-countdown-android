package com.basekeita.oshikatsucountdown.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basekeita.oshikatsucountdown.data.definition.repository.UserSettingsRepository
import com.basekeita.oshikatsucountdown.feature.settings.uimodel.NotificationPermissionUiModel
import com.basekeita.oshikatsucountdown.feature.settings.uimodel.ReminderSettingsUiModel
import com.basekeita.oshikatsucountdown.feature.settings.uimodel.ReminderTimingUiModel
import com.basekeita.oshikatsucountdown.feature.settings.uimodel.notificationPermissionOf
import com.basekeita.oshikatsucountdown.feature.settings.uimodel.toModel
import com.basekeita.oshikatsucountdown.feature.settings.uimodel.toUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.WriteFailureUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalTime
import javax.inject.Inject

/**
 * 設定。値は保存と読み込みだけなので Repository を直接呼ぶ。
 * 通知の予約し直しは、設定の変更を見ている `ReminderSync`（feature/reminder）が行う。
 * 通知の許可の状態は端末から読むので、画面が前面に来るたびに `onNotificationPermissionCheck` で渡してもらう。
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository,
    @param:AppVersionName private val appVersionName: String,
) : ViewModel() {
    /**
     * @property notificationPermission 通知の許可の状態。端末から読むまでは GRANTED（案内を出さない）。
     * @property isRequestingNotificationPermission 許可のダイアログを出す。画面が出し終えたら `onNotificationPermissionResult` で戻す。
     * @property writeFailure 設定の保存に失敗した。Snackbar で知らせ終えたら `onWriteFailureShown` で戻す。
     */
    data class UiState(
        val isLoading: Boolean = true,
        val isLoadFailed: Boolean = false,
        val showPastEvents: Boolean = false,
        val reminders: ReminderSettingsUiModel = ReminderSettingsUiModel(),
        val notificationPermission: NotificationPermissionUiModel = NotificationPermissionUiModel.GRANTED,
        val isRequestingNotificationPermission: Boolean = false,
        val appVersionName: String = "",
        val writeFailure: WriteFailureUiModel? = null,
    )

    /**
     * 画面が持つ状態。
     *
     * @property notificationsGranted 端末から読んだ、通知を出せるか。読むまでは null。
     * @property canRequestNotificationPermission 許可のダイアログを出せる版（Android 13 以降）。
     * @property exactAlarmsAllowed 端末から読んだ、正確なアラームを使えるか。
     */
    private data class ScreenState(
        val notificationsGranted: Boolean? = null,
        val canRequestNotificationPermission: Boolean = false,
        val exactAlarmsAllowed: Boolean = true,
        val isRequestingNotificationPermission: Boolean = false,
        val writeFailure: WriteFailureUiModel? = null,
    )

    private val screenState = MutableStateFlow(ScreenState())

    val uiState: StateFlow<UiState> =
        combine(userSettingsRepository.settings, screenState) { settings, screen ->
            val granted = screen.notificationsGranted
            UiState(
                isLoading = false,
                showPastEvents = settings.showPastEvents,
                reminders = settings.reminders.toUiModel(),
                notificationPermission = if (granted == null) {
                    NotificationPermissionUiModel.GRANTED
                } else {
                    notificationPermissionOf(
                        granted = granted,
                        canRequest = screen.canRequestNotificationPermission,
                        requested = settings.notificationPermissionRequested,
                        exactAlarmsAllowed = screen.exactAlarmsAllowed,
                    )
                },
                isRequestingNotificationPermission = screen.isRequestingNotificationPermission,
                appVersionName = appVersionName,
                writeFailure = screen.writeFailure,
            )
        }
            .catch { emit(UiState(isLoading = false, isLoadFailed = true, appVersionName = appVersionName)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun onShowPastEventsChange(show: Boolean) {
        save { userSettingsRepository.setShowPastEvents(show) }
    }

    /** ON にしたとき、まだ許可を求めていなければ求める（iOS 版と同じ）。 */
    fun onRemindersEnabledChange(enabled: Boolean) {
        save {
            userSettingsRepository.setRemindersEnabled(enabled)
            if (enabled && shouldRequestNotificationPermission()) {
                screenState.update { it.copy(isRequestingNotificationPermission = true) }
            }
        }
    }

    fun onReminderTimingChange(
        timing: ReminderTimingUiModel,
        enabled: Boolean,
    ) {
        save { userSettingsRepository.setReminderTimingEnabled(timing.toModel(), enabled) }
    }

    fun onReminderTimeChange(time: LocalTime) {
        save { userSettingsRepository.setReminderTimeOfDay(time) }
    }

    fun onAllowNotificationsClick() {
        screenState.update { it.copy(isRequestingNotificationPermission = true) }
    }

    /** 画面が端末から読んだ許可の状態。前面に来るたびに渡す（端末の設定で変えて戻ってきたときも案内を出し直す）。 */
    fun onNotificationPermissionCheck(
        granted: Boolean,
        canRequest: Boolean,
        exactAlarmsAllowed: Boolean,
    ) {
        screenState.update {
            it.copy(notificationsGranted = granted, canRequestNotificationPermission = canRequest, exactAlarmsAllowed = exactAlarmsAllowed)
        }
    }

    /** 許可のダイアログを閉じた。求めた印を残し、次からは「設定を開く」を出す。 */
    fun onNotificationPermissionResult(granted: Boolean) {
        screenState.update { it.copy(notificationsGranted = granted, isRequestingNotificationPermission = false) }
        viewModelScope.launch {
            try {
                userSettingsRepository.setNotificationPermissionRequested(true)
            } catch (exception: IOException) {
                // 画面に出ない印なので「保存できませんでした」は出さない。残せなくても、次にもう一度ダイアログを出すだけ
            }
        }
    }

    fun onWriteFailureShown() {
        screenState.update { it.copy(writeFailure = null) }
    }

    private suspend fun shouldRequestNotificationPermission(): Boolean {
        val screen = screenState.value
        return screen.canRequestNotificationPermission &&
            screen.notificationsGranted == false &&
            !userSettingsRepository.settings.first().notificationPermissionRequested
    }

    private fun save(write: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                write()
            } catch (exception: IOException) {
                screenState.update { it.copy(writeFailure = WriteFailureUiModel.SAVE) }
            }
        }
    }
}
