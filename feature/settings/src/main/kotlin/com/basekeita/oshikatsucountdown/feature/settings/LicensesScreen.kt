package com.basekeita.oshikatsucountdown.feature.settings

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.basekeita.oshikatsucountdown.ui.R as UiR

/**
 * アプリで使っているライブラリと、そのライセンスの一覧。行を押すとライセンスの本文を出す。
 * 一覧は app の依存からビルド時に作った JSON で、読み込みと表示は AboutLibraries の部品に任せる。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LicensesScreen(
    @RawRes librariesResId: Int,
    onNavigationEvent: (LicensesNavigationEvent) -> Unit,
) {
    val libraries by produceLibraries(librariesResId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.licenses_title)) },
                navigationIcon = {
                    IconButton(onClick = { onNavigationEvent(LicensesNavigationEvent.Back) }) {
                        Icon(
                            painter = painterResource(UiR.drawable.ic_arrow_back),
                            contentDescription = stringResource(UiR.string.back),
                        )
                    }
                },
                // システムバーの分は app が空ける
                windowInsets = WindowInsets(0.dp),
            )
        },
        contentWindowInsets = WindowInsets(0.dp),
    ) { innerPadding ->
        LibrariesContainer(
            libraries = libraries,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            licenseDialogConfirmText = stringResource(UiR.string.close),
        )
    }
}
