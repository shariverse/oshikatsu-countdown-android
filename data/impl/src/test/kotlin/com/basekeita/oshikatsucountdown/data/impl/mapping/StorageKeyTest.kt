package com.basekeita.oshikatsucountdown.data.impl.mapping

import com.basekeita.oshikatsucountdown.data.definition.model.EventType
import com.basekeita.oshikatsucountdown.data.definition.model.OshiColor
import org.junit.Assert.assertEquals
import org.junit.Test

/** 保存のキーは値ごとに 1 つで、重ならない（重なると読むときにどちらかに寄ってしまう）。 */
class StorageKeyTest {
    @Test
    fun `推し色のキーは重ならない`() {
        val keys = OshiColor.entries.map { it.storageKey }

        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `種類のキーは重ならない`() {
        val keys = EventType.entries.map { it.storageKey }

        assertEquals(keys.size, keys.toSet().size)
    }
}
