package com.basekeita.oshikatsucountdown.feature.reminder

import org.junit.Assert.assertEquals
import org.junit.Test

/** 予約の記録の置き換え方（足す前に前の記録と今回のものを書き、終えてから今回のものだけにする）。 */
class RecordReplacementTest {
    @Test
    fun `計画から外れたものを取り消し、足す前に前と今回の両方を記録し、終えたら今回のものだけにする`() {
        val records = recordReplacement(previous = setOf("old", "kept"), planned = setOf("kept", "new"))

        assertEquals(setOf("old", "kept", "new"), records.writeAhead)
        assertEquals(setOf("old"), records.toCancel)
        assertEquals(setOf("kept", "new"), records.final)
    }

    @Test
    fun `途中で止まっても、次の置き換えで前の回が足したものを取り消せる`() {
        val first = recordReplacement(previous = setOf("a"), planned = setOf("b"))
        // 足す前の記録（writeAhead）を書いたところで止まった。次の置き換えは、それを前の記録として読む
        val next = recordReplacement(previous = first.writeAhead, planned = setOf("c"))

        assertEquals(setOf("a", "b"), next.toCancel)
        assertEquals(setOf("c"), next.final)
    }

    @Test
    fun `通知を出せなくなったら、記録にあるものを全部取り消して記録を空にする`() {
        val records = recordReplacement(previous = setOf("a", "b"), planned = emptySet())

        assertEquals(setOf("a", "b"), records.toCancel)
        assertEquals(emptySet<String>(), records.final)
    }
}
