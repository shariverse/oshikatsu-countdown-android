package com.basekeita.oshikatsucountdown.data.definition.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NormalizationTest {
    @Test
    fun `名前は空白を潰して上限で切る`() {
        assertEquals("ミナト くん", Oshi.normalizeName("　ミナト\n\n くん  "))
        assertEquals("あ".repeat(Oshi.MAX_NAME_LENGTH), Oshi.normalizeName("あ".repeat(Oshi.MAX_NAME_LENGTH + 5)))
    }

    @Test
    fun `タイトルと会場は絵文字を途中で切らない`() {
        val family = "👨‍👩‍👧"
        val title = "a".repeat(OshiEvent.MAX_TITLE_LENGTH - 1) + family + "b"

        assertEquals("a".repeat(OshiEvent.MAX_TITLE_LENGTH - 1) + family, OshiEvent.normalizeTitle(title))
        assertEquals("東京ドーム", OshiEvent.normalizeVenue(" 東京ドーム "))
    }

    @Test
    fun `頭文字は見た目の1文字`() {
        assertEquals("👨‍👩‍👧", "👨‍👩‍👧と一緒".firstCharacter())
        assertEquals("L", "LUMINA".firstCharacter())
        assertEquals("", "".firstCharacter())
    }
}
