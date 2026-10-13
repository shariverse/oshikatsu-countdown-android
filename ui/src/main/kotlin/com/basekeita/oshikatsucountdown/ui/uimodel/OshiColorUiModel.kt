package com.basekeita.oshikatsucountdown.ui.uimodel

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.basekeita.oshikatsucountdown.ui.R

/**
 * 推し色。塗りの上に白い文字を置くので、どの色も白に対して 4.5:1 以上になる濃さにしてある。
 *
 * @property color 塗りの色。地の上の文字に使うときは `onSurfaceColor()` を通す。
 */
enum class OshiColorUiModel(
    val color: Color,
    @param:StringRes val labelRes: Int,
) {
    PINK(Color(0xFFC8326A), R.string.oshi_color_pink),
    RED(Color(0xFFD0352B), R.string.oshi_color_red),
    ORANGE(Color(0xFFB45A0E), R.string.oshi_color_orange),
    YELLOW(Color(0xFF8A6D00), R.string.oshi_color_yellow),
    GREEN(Color(0xFF1A7A50), R.string.oshi_color_green),
    MINT(Color(0xFF0E7C86), R.string.oshi_color_mint),
    BLUE(Color(0xFF2F6FD6), R.string.oshi_color_blue),
    NAVY(Color(0xFF3B4BB8), R.string.oshi_color_navy),
    PURPLE(Color(0xFF7A4BD0), R.string.oshi_color_purple),
    LAVENDER(Color(0xFFA23FB0), R.string.oshi_color_lavender),
    BROWN(Color(0xFF6B5B4B), R.string.oshi_color_brown),
    BLACK(Color(0xFF3A3842), R.string.oshi_color_black),
    ;

    /** 塗りの上の文字。 */
    val onColor: Color get() = Color.White
}
