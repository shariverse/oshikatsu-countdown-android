package com.basekeita.oshikatsucountdown.data.definition.model

import java.text.BreakIterator

private val whitespaceRun = Regex("""[\s\p{Z}]+""")

/** 連続する空白（改行・全角空白を含む）を半角空白 1 つに潰し、前後を落とす。 */
internal fun String.collapseWhitespace(): String = split(whitespaceRun).filter { it.isNotEmpty() }.joinToString(" ")

/** 先頭から `maxCount` 文字を取る。文字は見た目の 1 文字（書記素クラスタ）で数え、絵文字やサロゲートペアを途中で切らない。 */
fun String.takeCharacters(maxCount: Int): String {
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(this)
    var end = 0
    repeat(maxCount) {
        val next = iterator.next()
        if (next == BreakIterator.DONE) return this
        end = next
    }
    return substring(0, end)
}

/** 先頭の見た目の 1 文字。アバターの頭文字に使う。空なら空文字。 */
fun String.firstCharacter(): String = takeCharacters(1)
