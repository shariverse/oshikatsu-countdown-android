package com.basekeita.oshikatsucountdown.data.definition.model

/** 推し色（メンバーカラー）。実際の色の値は UI 層が持つ。 */
enum class OshiColor {
    PINK,
    RED,
    ORANGE,
    YELLOW,
    GREEN,
    MINT,
    BLUE,
    NAVY,
    PURPLE,
    LAVENDER,
    BROWN,
    BLACK,
    ;

    companion object {
        val DEFAULT = PINK
    }
}
