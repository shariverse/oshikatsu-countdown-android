package com.basekeita.oshikatsucountdown.ui.component.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

/** 縞の濃いほうの色。推し色を 80% の明るさに落とす。 */
private const val DEEP_STRIPE_FACTOR = 0.2f

/**
 * 写真が無いときに写真の場所を埋める、推し色の斜めの縞（135°）。
 *
 * @param stripeWidth 縞 1 本の幅。
 */
@Composable
fun StripedBackground(
    color: Color,
    modifier: Modifier = Modifier,
    stripeWidth: Dp = 22.dp,
) {
    val period = with(LocalDensity.current) { (stripeWidth * 2).toPx() }
    val brush = remember(color, period) {
        val deep = lerp(color, Color.Black, DEEP_STRIPE_FACTOR)
        // 45° の方向に start から end までが 1 周期になるよう、縦横を周期 / √2 にする
        val step = period / sqrt(2f)
        Brush.linearGradient(
            colorStops = arrayOf(0f to color, 0.5f to color, 0.5f to deep, 1f to deep),
            start = Offset.Zero,
            end = Offset(step, step),
            tileMode = TileMode.Repeated,
        )
    }
    Box(modifier = modifier.background(brush))
}

@Preview
@Composable
private fun StripedBackgroundPreview() {
    StripedBackground(color = Color(0xFFC8326A), modifier = Modifier.size(200.dp))
}
