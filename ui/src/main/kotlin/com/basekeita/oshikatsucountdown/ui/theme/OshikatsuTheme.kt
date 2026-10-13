package com.basekeita.oshikatsucountdown.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalOshikatsuColorScheme = staticCompositionLocalOf { lightOshikatsuColorScheme }

/** アプリのテーマ。色は `OshikatsuTheme.colorScheme`（外観で変わる色）と推し色（`OshiColorUiModel`）から引く。 */
object OshikatsuTheme {
    val colorScheme: OshikatsuColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalOshikatsuColorScheme.current

    @Composable
    operator fun invoke(
        darkTheme: Boolean = isSystemInDarkTheme(),
        content: @Composable () -> Unit,
    ) {
        val oshikatsuColorScheme = if (darkTheme) {
            darkOshikatsuColorScheme
        } else {
            lightOshikatsuColorScheme
        }

        val materialColorScheme = if (darkTheme) {
            materialColorSchemeOf(darkOshikatsuColorScheme, darkColorScheme())
        } else {
            materialColorSchemeOf(lightOshikatsuColorScheme, lightColorScheme())
        }

        CompositionLocalProvider(LocalOshikatsuColorScheme provides oshikatsuColorScheme) {
            MaterialTheme(
                colorScheme = materialColorScheme,
                content = content,
            )
        }
    }
}

/** Material の部品が使う色を、アプリの配色に寄せる。部品の既定の紫を出さないため、使う役割をトークンで上書きする。 */
private fun materialColorSchemeOf(
    scheme: OshikatsuColorScheme,
    base: ColorScheme,
): ColorScheme = base.copy(
    primary = scheme.brand,
    onPrimary = scheme.onBrand,
    primaryContainer = scheme.brandContainer,
    onPrimaryContainer = scheme.onBrandContainer,
    secondary = scheme.onSurfaceVariant,
    onSecondary = scheme.surface,
    secondaryContainer = scheme.brandContainer,
    onSecondaryContainer = scheme.onBrandContainer,
    tertiary = scheme.brand,
    onTertiary = scheme.onBrand,
    background = scheme.background,
    onBackground = scheme.onSurface,
    surface = scheme.background,
    onSurface = scheme.onSurface,
    surfaceVariant = scheme.surfaceContainer,
    onSurfaceVariant = scheme.onSurfaceVariant,
    surfaceContainerLowest = scheme.surface,
    surfaceContainerLow = scheme.surface,
    surfaceContainer = scheme.surfaceContainer,
    surfaceContainerHigh = scheme.surface,
    surfaceContainerHighest = scheme.surfaceContainerHigh,
    surfaceTint = scheme.surface,
    outline = scheme.outline,
    outlineVariant = scheme.outlineVariant,
    error = scheme.destructive,
)
