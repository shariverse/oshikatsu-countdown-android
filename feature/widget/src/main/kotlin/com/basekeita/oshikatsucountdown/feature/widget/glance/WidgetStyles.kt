package com.basekeita.oshikatsucountdown.feature.widget.glance

import android.os.Build
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import com.basekeita.oshikatsucountdown.ui.theme.darkOshikatsuColorScheme
import com.basekeita.oshikatsucountdown.ui.theme.lightOshikatsuColorScheme
import com.basekeita.oshikatsucountdown.ui.theme.onSurfaceColor
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel

/**
 * ウィジェットの色。端末の外観（ライト / ダーク）で切り替わる色はアプリの配色のトークンから作る。
 * 推し色の文字を置く地は surface（推し色の文字が 4.5:1 を保てる地）にする。
 */
internal object WidgetColors {
    val surface: ColorProvider = ColorProvider(day = lightOshikatsuColorScheme.surface, night = darkOshikatsuColorScheme.surface)
    val onSurface: ColorProvider = ColorProvider(day = lightOshikatsuColorScheme.onSurface, night = darkOshikatsuColorScheme.onSurface)
    val onSurfaceVariant: ColorProvider = ColorProvider(
        day = lightOshikatsuColorScheme.onSurfaceVariant,
        night = darkOshikatsuColorScheme.onSurfaceVariant,
    )
    val brandContainer: ColorProvider = ColorProvider(
        day = lightOshikatsuColorScheme.brandContainer,
        night = darkOshikatsuColorScheme.brandContainer,
    )
    val onBrandContainer: ColorProvider = ColorProvider(
        day = lightOshikatsuColorScheme.onBrandContainer,
        night = darkOshikatsuColorScheme.onBrandContainer,
    )
}

/** 推し色の塗り。外観で変えない。 */
internal fun OshiColorUiModel.fillColor(): ColorProvider = ColorProvider(color)

/** 推し色の塗りの上の文字。 */
internal fun OshiColorUiModel.onFillColor(): ColorProvider = ColorProvider(onColor)

/** 推し色を surface の上の文字に使うときの色。ダークでは白に寄せる。 */
internal fun OshiColorUiModel.onSurfaceTextColor(): ColorProvider = ColorProvider(day = onSurfaceColor(isDark = false), night = onSurfaceColor(isDark = true))

/**
 * ウィジェットの外枠。角の丸みはホーム画面の他のウィジェットとそろえる（Android 12 以降。11 では Glance が角を丸められない）。
 */
internal fun GlanceModifier.widgetBackground(): GlanceModifier {
    val background = appWidgetBackground()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        background.cornerRadius(android.R.dimen.system_app_widget_background_radius)
    } else {
        background
    }
}
