package com.basekeita.oshikatsucountdown.data.impl.mapping

import com.basekeita.oshikatsucountdown.data.definition.model.EventType
import com.basekeita.oshikatsucountdown.data.definition.model.OshiColor
import com.basekeita.oshikatsucountdown.local.database.model.EventEntity
import com.basekeita.oshikatsucountdown.local.database.model.OshiEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * 保存のキーは値ごとに 1 つで、重ならない（重なると読むときにどちらかに寄ってしまう）。
 * 対応表に無いキーは既定（推し色はピンク、種類はその他）で読む。
 */
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

    @Test
    fun `対応表に無い推し色のキーは既定の色で読む`() {
        val entity = OshiEntity(
            id = UUID.randomUUID(),
            name = "ひより",
            color = "gold",
            photoPath = null,
            createdAt = Instant.EPOCH,
        )

        assertEquals(OshiColor.DEFAULT, entity.toModel().color)
    }

    @Test
    fun `対応表に無い種類のキーはその他で読む`() {
        val entity = EventEntity(
            id = UUID.randomUUID(),
            oshiId = null,
            type = "unknown",
            title = "読める",
            date = LocalDate.of(2026, 10, 18),
            startTime = null,
            venue = "",
            repeatsYearly = false,
            pinned = false,
            createdAt = Instant.EPOCH,
        )

        assertEquals(EventType.OTHER, entity.toModel().type)
    }
}
