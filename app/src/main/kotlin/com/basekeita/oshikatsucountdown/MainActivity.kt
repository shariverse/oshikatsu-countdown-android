package com.basekeita.oshikatsucountdown

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.basekeita.oshikatsucountdown.feature.reminder.ReminderSync
import com.basekeita.oshikatsucountdown.navigation.EventDetailRoute
import com.basekeita.oshikatsucountdown.navigation.EventEditorRoute
import com.basekeita.oshikatsucountdown.navigation.ExternalLaunchExtras
import com.basekeita.oshikatsucountdown.ui.theme.OshikatsuTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var reminderSync: ReminderSync

    /** 通知の許可のダイアログ。許可されたかどうかは、閉じたときの onResume で予約し直して反映する。 */
    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onNotificationPermissionResult()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { viewModel.uiState.value.isLoading }
        // 作り直し（画面の回転など）では、開いたときの Intent をもう一度受けない
        if (savedInstanceState == null) {
            intent.toLaunchRoute()?.let(viewModel::onLaunchRequest)
        }
        enableEdgeToEdge()
        setContent {
            OshikatsuTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(uiState.isRequestingNotificationPermission) {
                    if (uiState.isRequestingNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                if (!uiState.isLoading) {
                    OshikatsuApp(
                        // 設定が読めなくても使えるよう、読めなければ導線は出さずにタブを出す
                        onboardingCompleted = uiState.onboardingCompleted || uiState.isLoadFailed,
                        launchRoute = uiState.launchRoute,
                        onLaunchRouteHandle = viewModel::onLaunchRouteHandle,
                        onOnboardingFinish = viewModel::onOnboardingFinish,
                        onEventSave = ::onEventSave,
                    )
                }
            }
        }
    }

    /**
     * アプリが前面に来たら、予定の通知と当日のカウントダウンを置き直す（出た通知のぶん空いた枠を埋める・許可を変えて戻ってきたときに反映する）。
     * 許可のダイアログを閉じたときも onResume を通る。置き直しはアプリのプロセスの scope で動かす（メインスレッドを止めず、画面を回しても途中で止まらない）。
     */
    override fun onResume() {
        super.onResume()
        reminderSync.requestRefreshAll()
    }

    /** 予定を保存し終えた。端末の通知の許可の状態を読んで渡す（求めるかは ViewModel が決める）。 */
    private fun onEventSave() {
        val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        viewModel.onEventSave(
            canRequestNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
            notificationsGranted = notificationsGranted,
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.toLaunchRoute()?.let(viewModel::onLaunchRequest)
    }
}

/** ウィジェットから開くように頼まれた画面。ランチャーから開いたときは null。 */
private fun Intent.toLaunchRoute(): NavKey? {
    val eventId = getStringExtra(ExternalLaunchExtras.EVENT_ID)
    if (eventId != null) return EventDetailRoute(eventId)

    if (getBooleanExtra(ExternalLaunchExtras.ADD_EVENT, false)) return EventEditorRoute()

    return null
}
