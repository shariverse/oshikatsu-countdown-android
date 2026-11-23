package com.basekeita.oshikatsucountdown

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import com.basekeita.oshikatsucountdown.data.definition.repository.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * 初回の導線を出すかどうかと、アプリの外（ウィジェット）から開くように頼まれた画面。
 * 起動画面は導線を出すかどうかが読めるまで出しておく（読む前に導線やタブを一瞬出さないため）ので、画面の購読を待たずに読み始める。
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository,
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

    /**
     * @property launchRoute アプリの外から開くように頼まれた画面。開いたら null に戻す。
     * @property asksNotificationPermissionOnSave 予定を保存した直後に通知の許可を求める（まだ一度も求めていない）。
     *   設定の「予定を通知する」が OFF でも求める。当日のカウントダウンは「予定を通知する」とは別で、Android では同じ通知の許可が要るため。
     */
    data class UiState(
        val onboardingCompleted: Boolean,
        val launchRoute: NavKey?,
        val asksNotificationPermissionOnSave: Boolean = false,
    )

    private val launchRoute = MutableStateFlow<NavKey?>(null)

    /**
     * 導線を終えたことを保存できなくても、終えた入力画面に取り残さないよう、このプロセスの間は終えたものとして扱う。
     * 保存できなかったときは次に起動したときにもう一度導線を出す。
     */
    private val onboardingFinishedInProcess = MutableStateFlow(false)

    private val onboardingCompleted: Flow<Boolean> =
        combine(userSettingsRepository.settings, onboardingFinishedInProcess) { settings, finishedInProcess ->
            settings.onboardingCompleted || finishedInProcess
        }
            .distinctUntilChanged()

    private val asksNotificationPermissionOnSave: Flow<Boolean> =
        userSettingsRepository.settings
            .map { settings -> !settings.notificationPermissionRequested }
            .distinctUntilChanged()

    val viewState: StateFlow<ViewState> =
        combine(onboardingCompleted, launchRoute, asksNotificationPermissionOnSave) { completed, route, asksPermission ->
            ViewState.Success(
                UiState(onboardingCompleted = completed, launchRoute = route, asksNotificationPermissionOnSave = asksPermission),
            ) as ViewState
        }
            .catch { error -> emit(ViewState.Error(error.message.orEmpty())) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, ViewState.Loading)

    /** 初回の導線を終えた（2 歩目で保存した、または「あとで」を押した）。 */
    fun onOnboardingFinish() {
        onboardingFinishedInProcess.value = true
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
        launchRoute.value = route
    }

    /** 予定を保存した直後に通知の許可を求めた。次からは求めない（設定の画面で「設定を開く」を出す）。 */
    fun onNotificationPermissionRequested() {
        viewModelScope.launch {
            try {
                userSettingsRepository.setNotificationPermissionRequested(true)
            } catch (exception: IOException) {
                // 印を残せなくても、次に保存したときにもう一度求めるだけ
            }
        }
    }

    /** 頼まれた画面を開いた（導線の途中で開かなかったときも含む）。 */
    fun onLaunchRouteHandle() {
        launchRoute.value = null
    }
}
