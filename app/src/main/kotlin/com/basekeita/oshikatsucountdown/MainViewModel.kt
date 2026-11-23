package com.basekeita.oshikatsucountdown

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import com.basekeita.oshikatsucountdown.data.definition.repository.UserSettingsRepository
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
import javax.inject.Inject

/**
 * 初回の導線を出すかどうか、アプリの外（ウィジェット）から開くように頼まれた画面、予定を保存した直後に通知の許可を求めるか。
 * 起動画面は導線を出すかどうかが読めるまで出しておく（読む前に導線やタブを一瞬出さないため）ので、画面の購読を待たずに読み始める（`Eagerly`）。
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository,
) : ViewModel() {
    /**
     * @property isLoading 導線を出すかどうかを読んでいる。読み終えるまで起動画面を出しておく。
     * @property isLoadFailed 設定を読めなかった。使えるよう、導線は出さずにタブを出す。
     * @property onboardingCompleted 初回の導線を終えた。
     * @property launchRoute アプリの外から開くように頼まれた画面。開いたら `onLaunchRouteHandle` で null に戻す。
     * @property isRequestingNotificationPermission 通知の許可のダイアログを出す。閉じたら `onNotificationPermissionResult` で戻す。
     */
    data class UiState(
        val isLoading: Boolean = true,
        val isLoadFailed: Boolean = false,
        val onboardingCompleted: Boolean = false,
        val launchRoute: NavKey? = null,
        val isRequestingNotificationPermission: Boolean = false,
    )

    /**
     * 画面が持つ状態。
     *
     * @property onboardingFinishedInProcess 導線を終えたことを保存できなくても、終えた入力画面に取り残さないよう、このプロセスの間は終えたものとして扱う。
     *   保存できなかったときは、次に起動したときにもう一度導線を出す。
     */
    private data class ScreenState(
        val launchRoute: NavKey? = null,
        val onboardingFinishedInProcess: Boolean = false,
        val isRequestingNotificationPermission: Boolean = false,
    )

    private val screenState = MutableStateFlow(ScreenState())

    val uiState: StateFlow<UiState> =
        combine(userSettingsRepository.settings, screenState) { settings, screen ->
            UiState(
                isLoading = false,
                onboardingCompleted = settings.onboardingCompleted || screen.onboardingFinishedInProcess,
                launchRoute = screen.launchRoute,
                isRequestingNotificationPermission = screen.isRequestingNotificationPermission,
            )
        }
            .catch { emit(UiState(isLoading = false, isLoadFailed = true)) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    /** 初回の導線を終えた（2 歩目で保存した、または「あとで」を押した）。 */
    fun onOnboardingFinish() {
        screenState.update { it.copy(onboardingFinishedInProcess = true) }
        viewModelScope.launch {
            try {
                userSettingsRepository.setOnboardingCompleted(true)
            } catch (exception: IOException) {
                // 次に起動したときに導線をもう一度出すだけで、登録した推しと予定は残っている
            }
        }
    }

    /** アプリの外（ウィジェット）から画面を開くように頼まれた。 */
    fun onLaunchRequest(route: NavKey) {
        screenState.update { it.copy(launchRoute = route) }
    }

    /** 頼まれた画面を開いた（導線の途中で開かなかったときも含む）。 */
    fun onLaunchRouteHandle() {
        screenState.update { it.copy(launchRoute = null) }
    }

    /**
     * 予定を保存し終えた。まだ一度も求めていなければ、通知の許可を求める（`asksNotificationPermissionOnSave`）。
     * 端末の状態（許可のダイアログを出せる版か・もう許可されているか）は画面が読んで渡す。
     */
    fun onEventSave(
        canRequestNotificationPermission: Boolean,
        notificationsGranted: Boolean,
    ) {
        viewModelScope.launch {
            val requested = userSettingsRepository.settings.first().notificationPermissionRequested
            screenState.update { screen ->
                val asks = asksNotificationPermissionOnSave(
                    canRequest = canRequestNotificationPermission,
                    granted = notificationsGranted,
                    requested = requested,
                    requesting = screen.isRequestingNotificationPermission,
                )
                if (asks) {
                    screen.copy(isRequestingNotificationPermission = true)
                } else {
                    screen
                }
            }
        }
    }

    /** 許可のダイアログを閉じた。求めた印を残し、次からは求めない（設定の画面で「設定を開く」を出す）。 */
    fun onNotificationPermissionResult() {
        screenState.update { it.copy(isRequestingNotificationPermission = false) }
        viewModelScope.launch {
            try {
                userSettingsRepository.setNotificationPermissionRequested(true)
            } catch (exception: IOException) {
                // 印を残せなくても、次に保存したときにもう一度求めるだけ
            }
        }
    }
}
