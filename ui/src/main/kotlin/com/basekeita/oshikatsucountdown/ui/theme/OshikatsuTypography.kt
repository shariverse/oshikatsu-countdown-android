package com.basekeita.oshikatsucountdown.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * カウントダウンの数字の文字。書体はシステムの既定で、桁の幅をそろえる（`tnum`）ため秒が進んでも横に揺れない。
 * 本文の文字は M3 の `MaterialTheme.typography` を使う。
 */
object OshikatsuTypography {
    private val numeral = TextStyle(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum")

    /** 詳細の残り日数。 */
    val countdownHero = numeral.copy(fontSize = 88.sp, lineHeight = 88.sp, letterSpacing = (-2).sp)

    /** ホームの先頭の残り日数。 */
    val countdownLarge = numeral.copy(fontSize = 68.sp, lineHeight = 68.sp, letterSpacing = (-1).sp)

    /** 詳細の時・分・秒。 */
    val countdownClock = numeral.copy(fontSize = 32.sp, lineHeight = 36.sp)

    /** 一覧の行の残り日数。 */
    val countdownSmall = numeral.copy(fontSize = 24.sp, lineHeight = 28.sp)

    /** 先頭のカードの日付（`10.18 SUN`）。 */
    val dateBadge = numeral.copy(fontSize = 18.sp, lineHeight = 22.sp)
}
