package com.basekeita.oshikatsucountdown.ui.component.molecules

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import com.basekeita.oshikatsucountdown.ui.uimodel.WriteFailureUiModel

/**
 * 書き込みの失敗を [snackbarHostState] の Snackbar で知らせ、消えたら [onShown] を呼ぶ。
 * 出す前に [onShown] を呼ぶと [failure] が null に戻って LaunchedEffect ごと Snackbar が消えるため、消えてから呼ぶ。
 */
@Composable
fun WriteFailureSnackbar(
    failure: WriteFailureUiModel?,
    snackbarHostState: SnackbarHostState,
    onShown: () -> Unit,
) {
    val message = failure?.let { stringResource(it.messageRes) }
    val currentOnShown by rememberUpdatedState(onShown)
    LaunchedEffect(message) {
        if (message == null) return@LaunchedEffect

        snackbarHostState.showSnackbar(message)
        currentOnShown()
    }
}
