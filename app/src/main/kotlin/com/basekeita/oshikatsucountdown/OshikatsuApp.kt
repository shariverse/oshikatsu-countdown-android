package com.basekeita.oshikatsucountdown

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.basekeita.oshikatsucountdown.feature.eventdetail.EventDetailNavigationEvent
import com.basekeita.oshikatsucountdown.feature.eventdetail.eventDetailEntry
import com.basekeita.oshikatsucountdown.feature.eventeditor.EventEditorNavigationEvent
import com.basekeita.oshikatsucountdown.feature.eventeditor.eventEditorEntry
import com.basekeita.oshikatsucountdown.feature.home.HomeNavigationEvent
import com.basekeita.oshikatsucountdown.feature.home.homeEntry
import com.basekeita.oshikatsucountdown.feature.oshi.OshiEditorNavigationEvent
import com.basekeita.oshikatsucountdown.feature.oshi.OshiListNavigationEvent
import com.basekeita.oshikatsucountdown.feature.oshi.oshiEditorEntry
import com.basekeita.oshikatsucountdown.feature.oshi.oshiListEntry
import com.basekeita.oshikatsucountdown.feature.settings.LicensesNavigationEvent
import com.basekeita.oshikatsucountdown.feature.settings.SettingsNavigationEvent
import com.basekeita.oshikatsucountdown.feature.settings.licensesEntry
import com.basekeita.oshikatsucountdown.feature.settings.settingsEntry
import com.basekeita.oshikatsucountdown.navigation.EventDetailRoute
import com.basekeita.oshikatsucountdown.navigation.EventEditorRoute
import com.basekeita.oshikatsucountdown.navigation.HomeRoute
import com.basekeita.oshikatsucountdown.navigation.LicensesRoute
import com.basekeita.oshikatsucountdown.navigation.OshiEditorRoute
import com.basekeita.oshikatsucountdown.navigation.OshiListRoute
import com.basekeita.oshikatsucountdown.navigation.SettingsRoute
import com.basekeita.oshikatsucountdown.ui.R as UiR

/** タブ。 */
private enum class OshikatsuTab(
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    HOME(R.string.tab_home, UiR.drawable.ic_home),
    OSHI(R.string.tab_oshi, UiR.drawable.ic_favorite),
    SETTINGS(R.string.tab_settings, UiR.drawable.ic_settings),
}

/**
 * アプリの画面。初回の導線を終えるまでは導線（推しの登録 → 最初の予定）、終えたらタブを出す。
 *
 * @param launchRoute アプリの外（ウィジェット）から開くように頼まれた画面。ホームのタブに積む。
 * @param onLaunchRouteHandle `launchRoute` を開いた（導線の途中で開かなかったときも含む）。
 * @param onOnboardingFinish 導線の 2 歩目で保存した、または「あとで」を押した。
 */
@Composable
fun OshikatsuApp(
    onboardingCompleted: Boolean,
    launchRoute: NavKey?,
    onLaunchRouteHandle: () -> Unit,
    onOnboardingFinish: () -> Unit,
) {
    if (onboardingCompleted) {
        OshikatsuTabs(launchRoute = launchRoute, onLaunchRouteHandle = onLaunchRouteHandle)
    } else {
        // 導線の途中では開かない（導線を終えた後に急に開かないよう、ここで捨てる）
        LaunchedEffect(launchRoute) {
            if (launchRoute != null) onLaunchRouteHandle()
        }
        OnboardingFlow(onFinish = onOnboardingFinish)
    }
}

/**
 * 初回の導線。推しを登録（または「あとで」）したら最初の予定の登録に進む。
 * 1 歩目には戻らない（登録した推しを同じ画面でもう一度登録させないため、1 歩目を 2 歩目に置き換える）。
 */
@Composable
private fun OnboardingFlow(onFinish: () -> Unit) {
    val backStack = rememberNavBackStack(OshiEditorRoute(onboarding = true))
    val entries = rememberDecoratedNavEntries(
        backStack,
        rememberEntryDecorators(),
        entryProvider {
            oshiEditorEntry { event ->
                when (event) {
                    OshiEditorNavigationEvent.Finished -> backStack[backStack.lastIndex] = EventEditorRoute(onboarding = true)
                }
            }
            eventEditorEntry { event ->
                when (event) {
                    EventEditorNavigationEvent.Finished -> onFinish()
                }
            }
        },
    )
    NavDisplay(
        entries = entries,
        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
        onBack = { backStack.popIfNotRoot() },
    )
}

/**
 * ホーム・推し・設定の 3 画面をタブで切り替える。
 *
 * タブは `NavigationSuiteScaffold` で、ウィンドウの大きさに応じて M3 の基準でナビゲーションバーとレールを切り替える。
 * タブごとにバックスタックを持ち、切り替えても各タブの画面の状態（ViewModel を含む）を残す。
 * 詳細や入力の画面はそのタブのバックスタックに積み、積んでいる間はナビゲーションを隠して画面を広く使う。
 * アプリの外から開くように頼まれた画面は、ホームのタブを最初の画面まで戻してから積む。
 */
@Composable
private fun OshikatsuTabs(
    launchRoute: NavKey?,
    onLaunchRouteHandle: () -> Unit,
) {
    var currentTab by rememberSaveable { mutableStateOf(OshikatsuTab.HOME) }
    val homeBackStack = rememberNavBackStack(HomeRoute)
    val oshiBackStack = rememberNavBackStack(OshiListRoute)
    val settingsBackStack = rememberNavBackStack(SettingsRoute)

    LaunchedEffect(launchRoute) {
        if (launchRoute == null) return@LaunchedEffect

        currentTab = OshikatsuTab.HOME
        homeBackStack.popToRoot()
        homeBackStack.add(launchRoute)
        onLaunchRouteHandle()
    }

    // どのタブも毎回組み立て、選んでいないタブのエントリの状態も破棄しない
    val homeEntries = rememberTabEntries(homeBackStack)
    val oshiEntries = rememberTabEntries(oshiBackStack)
    val settingsEntries = rememberTabEntries(settingsBackStack)

    val currentBackStack = when (currentTab) {
        OshikatsuTab.HOME -> homeBackStack
        OshikatsuTab.OSHI -> oshiBackStack
        OshikatsuTab.SETTINGS -> settingsBackStack
    }
    val currentEntries = when (currentTab) {
        OshikatsuTab.HOME -> homeEntries
        OshikatsuTab.OSHI -> oshiEntries
        OshikatsuTab.SETTINGS -> settingsEntries
    }

    val navigationSuiteType = if (currentBackStack.size > 1) {
        NavigationSuiteType.None
    } else {
        NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfo())
    }

    NavigationSuiteScaffold(
        navigationItems = {
            OshikatsuTab.entries.forEach { tab ->
                NavigationSuiteItem(
                    selected = tab == currentTab,
                    onClick = { currentTab = tab },
                    icon = { Icon(painter = painterResource(tab.iconRes), contentDescription = null) },
                    label = { Text(text = stringResource(tab.labelRes)) },
                )
            }
        },
        navigationSuiteType = navigationSuiteType,
    ) {
        NavDisplay(
            entries = currentEntries,
            // ナビゲーションの分は NavigationSuiteScaffold が除くので、残りのシステムバーの分だけ空ける
            modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
            onBack = { currentBackStack.popIfNotRoot() },
        )
    }
}

/** 画面の保存状態と ViewModel をエントリごとに持たせる。 */
@Composable
private fun rememberEntryDecorators(): List<NavEntryDecorator<NavKey>> = listOf(
    rememberSaveableStateHolderNavEntryDecorator(),
    rememberViewModelStoreNavEntryDecorator(),
)

/** 1 つのタブのエントリ。 */
@Composable
private fun rememberTabEntries(backStack: NavBackStack<NavKey>): List<NavEntry<NavKey>> = rememberDecoratedNavEntries(backStack, rememberEntryDecorators(), tabEntryProvider(backStack))

/** 1 つのタブで出す画面。各画面の遷移の要求は、そのタブのバックスタックに積む・外す。 */
private fun tabEntryProvider(backStack: NavBackStack<NavKey>): (NavKey) -> NavEntry<NavKey> = entryProvider {
    homeEntry { event ->
        when (event) {
            is HomeNavigationEvent.OpenEvent -> backStack.add(EventDetailRoute(event.eventId))
            HomeNavigationEvent.AddEvent -> backStack.add(EventEditorRoute())
        }
    }
    eventDetailEntry { event ->
        when (event) {
            EventDetailNavigationEvent.Back -> backStack.popIfNotRoot()
            is EventDetailNavigationEvent.Edit -> backStack.add(EventEditorRoute(eventId = event.eventId))
        }
    }
    eventEditorEntry { event ->
        when (event) {
            EventEditorNavigationEvent.Finished -> backStack.popIfNotRoot()
        }
    }
    oshiListEntry { event ->
        when (event) {
            is OshiListNavigationEvent.OpenOshi -> backStack.add(OshiEditorRoute(oshiId = event.oshiId))
            OshiListNavigationEvent.AddOshi -> backStack.add(OshiEditorRoute())
        }
    }
    oshiEditorEntry { event ->
        when (event) {
            OshiEditorNavigationEvent.Finished -> backStack.popIfNotRoot()
        }
    }
    settingsEntry { event ->
        when (event) {
            SettingsNavigationEvent.OpenLicenses -> backStack.add(LicensesRoute)
        }
    }
    licensesEntry(librariesResId = R.raw.aboutlibraries) { event ->
        when (event) {
            LicensesNavigationEvent.Back -> backStack.popIfNotRoot()
        }
    }
}

/**
 * いちばん上の画面を外す。タブの最初の画面は外さない（戻る操作と、予定が消えたときの詳細の「戻る」が重なっても空にしないため）。
 */
private fun NavBackStack<NavKey>.popIfNotRoot() {
    if (size > 1) removeAt(lastIndex)
}

/** タブの最初の画面まで戻す。 */
private fun NavBackStack<NavKey>.popToRoot() {
    while (size > 1) {
        removeAt(lastIndex)
    }
}
