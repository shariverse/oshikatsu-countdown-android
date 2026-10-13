package com.basekeita.oshikatsucountdown.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel

/** ダークの地で推し色を文字に使うとき、白に寄せる割合。どの推し色もダークの地に 4.5:1 以上になる値。 */
internal const val DARK_TEXT_LIGHTEN_FRACTION = 0.45f

/**
 * 推し色を地の上の文字・細い線に使うときの色。ダークの地では暗い推し色（ブラック・ネイビー）が沈むので白に寄せる。
 * 4.5:1 を保てるのは画面の地（background）とカードの地（surface）の上だけで、ライトの surfaceContainer の上には置かない。
 */
@Composable
@ReadOnlyComposable
fun OshiColorUiModel.onSurfaceColor(): Color = onSurfaceColor(OshikatsuTheme.colorScheme.isDark)

/** 外観を引数で受ける `onSurfaceColor`。Compose のテーマの外（ウィジェットのライトとダークの色を両方渡すところ）で使う。 */
fun OshiColorUiModel.onSurfaceColor(isDark: Boolean): Color = accentTextColor(color, isDark)

/** 推し色を薄く敷いた地（選択中のチップなど）。文字は `OshikatsuTheme.colorScheme.onSurface`。 */
@Composable
@ReadOnlyComposable
fun OshiColorUiModel.containerColor(): Color {
    val scheme = OshikatsuTheme.colorScheme
    return if (scheme.isDark) {
        lerp(scheme.surface, color, 0.35f)
    } else {
        lerp(Color.White, color, 0.18f)
    }
}

internal fun accentTextColor(
    accent: Color,
    isDark: Boolean,
): Color = if (isDark) {
    lerp(accent, Color.White, DARK_TEXT_LIGHTEN_FRACTION)
} else {
    accent
}
