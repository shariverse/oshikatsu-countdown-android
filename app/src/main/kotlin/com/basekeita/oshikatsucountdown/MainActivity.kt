package com.basekeita.oshikatsucountdown

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.basekeita.oshikatsucountdown.navigation.EventDetailRoute
import com.basekeita.oshikatsucountdown.navigation.EventEditorRoute
import com.basekeita.oshikatsucountdown.navigation.ExternalLaunchExtras
import com.basekeita.oshikatsucountdown.ui.theme.OshikatsuTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { viewModel.viewState.value is MainViewModel.ViewState.Loading }
        // 作り直し（画面の回転など）では、開いたときの Intent をもう一度受けない
        if (savedInstanceState == null) {
            intent.toLaunchRoute()?.let(viewModel::onLaunchRequest)
        }
        enableEdgeToEdge()
        setContent {
            OshikatsuTheme {
                val viewState by viewModel.viewState.collectAsStateWithLifecycle()
                when (val state = viewState) {
                    MainViewModel.ViewState.Loading -> Unit

                    // 設定が読めなくても使えるよう、導線は出さずにタブを出す
                    is MainViewModel.ViewState.Error -> {
                        OshikatsuApp(
                            onboardingCompleted = true,
                            launchRoute = null,
                            onLaunchRouteHandle = viewModel::onLaunchRouteHandle,
                            onOnboardingFinish = viewModel::onOnboardingFinish,
                        )
                    }

                    is MainViewModel.ViewState.Success -> {
                        OshikatsuApp(
                            onboardingCompleted = state.uiState.onboardingCompleted,
                            launchRoute = state.uiState.launchRoute,
                            onLaunchRouteHandle = viewModel::onLaunchRouteHandle,
                            onOnboardingFinish = viewModel::onOnboardingFinish,
                        )
                    }
                }
            }
        }
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
