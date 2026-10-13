package com.basekeita.oshikatsucountdown.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel
import org.junit.Assert.assertTrue
import org.junit.Test

class OshiColorContrastTest {
    private fun contrast(
        foreground: Color,
        background: Color,
    ): Float {
        val lighter = maxOf(foreground.luminance(), background.luminance())
        val darker = minOf(foreground.luminance(), background.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    @Test
    fun `推し色の塗りに乗せた白い文字は4_5以上`() {
        OshiColorUiModel.entries.forEach { oshiColor ->
            val ratio = contrast(oshiColor.onColor, oshiColor.color)
            assertTrue("${oshiColor.name}: $ratio", ratio >= 4.5f)
        }
    }

    @Test
    fun `推し色の文字は画面の地とカードの地に4_5以上`() {
        val lightGrounds = with(lightOshikatsuColorScheme) { listOf(background, surface) }
        val darkGrounds = with(darkOshikatsuColorScheme) { listOf(background, surface, surfaceContainer) }
        OshiColorUiModel.entries.forEach { oshiColor ->
            lightGrounds.forEach { ground ->
                val ratio = contrast(accentTextColor(oshiColor.color, isDark = false), ground)
                assertTrue("${oshiColor.name} light: $ratio", ratio >= 4.5f)
            }
            darkGrounds.forEach { ground ->
                val ratio = contrast(accentTextColor(oshiColor.color, isDark = true), ground)
                assertTrue("${oshiColor.name} dark: $ratio", ratio >= 4.5f)
            }
        }
    }

    @Test
    fun `本文と補助テキストは地に4_5以上`() {
        listOf(lightOshikatsuColorScheme, darkOshikatsuColorScheme).forEach { scheme ->
            listOf(scheme.background, scheme.surface, scheme.surfaceContainer).forEach { ground ->
                assertTrue(contrast(scheme.onSurface, ground) >= 4.5f)
                assertTrue(contrast(scheme.onSurfaceVariant, ground) >= 4.5f)
            }
            assertTrue(contrast(scheme.onBrand, scheme.brand) >= 4.5f)
            assertTrue(contrast(scheme.onBrandContainer, scheme.brandContainer) >= 4.5f)
        }
    }
}
