@file:Suppress("ktlint:standard:comment-wrapping", "ktlint:standard:discouraged-comment-location")

package com.basekeita.oshikatsucountdown.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// 配色。地・文字・線は画面デザイン（Android ホーム）の M3 の値、アクセントは推し色。
// アプリ全体のアクセント（brand）は推し色のピンクと同じ。
// ─────────────────────────────────────────────────────────────────────────────

/** 端末の外観設定（ライト / ダーク）で値が変わる色。 */
@Immutable
data class OshikatsuColorScheme(
    val isDark: Boolean,
    val background: Color, // 画面の地
    val surface: Color, // カード・シート
    val surfaceContainer: Color, // ナビゲーションバー・入力欄の地
    val surfaceContainerHigh: Color, // 進み具合のバーの溝・区切り
    val onSurface: Color, // 本文
    val onSurfaceVariant: Color, // 補助テキスト
    val outline: Color, // チップの枠
    val outlineVariant: Color, // 区切り線
    val brand: Color, // アプリのアクセント（推しが決まっていないところ）
    val onBrand: Color, // brand の塗りに乗る文字
    val brandContainer: Color, // 選択中のチップ・ナビゲーションの印
    val onBrandContainer: Color, // brandContainer に乗る文字
    val destructive: Color, // 削除の文字
)

/** ライト。 */
val lightOshikatsuColorScheme = OshikatsuColorScheme(
    isDark = false,
    background = Color(0xFFFCF8FA),
    surface = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF3EDF1),
    surfaceContainerHigh = Color(0xFFE2DCE3),
    onSurface = Color(0xFF1D1B20),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    brand = Color(0xFFC8326A),
    onBrand = Color(0xFFFFFFFF),
    brandContainer = Color(0xFFF4D6E1), // brand を白に 18% 混ぜた色
    onBrandContainer = Color(0xFF1D1B20),
    destructive = Color(0xFFB3261E),
)

/** ダーク。 */
val darkOshikatsuColorScheme = OshikatsuColorScheme(
    isDark = true,
    background = Color(0xFF141218),
    surface = Color(0xFF211F26),
    surfaceContainer = Color(0xFF1D1B20),
    surfaceContainerHigh = Color(0xFF36343B),
    onSurface = Color(0xFFE6E0E9),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    brand = Color(0xFFFFB1C8),
    onBrand = Color(0xFF5E1133),
    brandContainer = Color(0xFF7E2A4A),
    onBrandContainer = Color(0xFFFFD9E2),
    destructive = Color(0xFFF2B8B5),
)
