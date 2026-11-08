package com.basekeita.oshikatsucountdown.local

import com.basekeita.oshikatsucountdown.local.database.model.EventEntity
import com.basekeita.oshikatsucountdown.local.database.model.OshiEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** 推しと予定の結び付き（推しの保存・削除が予定に与えるもの）と、登録した順の並び。 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class OshiLocalDataSourceTest {
    private val testScope = TestScope(UnconfinedTestDispatcher())
    private val database = testDatabase()
    private val oshiDataSource = OshiLocalDataSource(database.oshiDao())
    private val eventDataSource = EventLocalDataSource(database.eventDao())

    private val luminaId = UUID.randomUUID()
    private val minatoId = UUID.randomUUID()

    private fun oshi(
        id: UUID,
        color: String,
        sortOrder: Long,
    ) = OshiEntity(id = id, name = "LUMINA", color = color, photoPath = null, sortOrder = sortOrder)

    private fun event(
        title: String,
        oshiId: UUID?,
    ) = EventEntity(
        id = UUID.randomUUID(),
        oshiId = oshiId,
        type = "live",
        title = title,
        date = LocalDate.of(2026, 10, 18),
        startTime = null,
        venue = "",
        repeatsYearly = false,
        pinned = false,
        createdAt = Instant.EPOCH,
        sortOrder = 0,
    )

    private suspend fun saveOshi(
        id: UUID,
        color: String,
    ) {
        oshiDataSource.upsertOshi(id) { sortOrder -> oshi(id, color, sortOrder) }
    }

    /** 保存し、登録した順を付けた行を返す。 */
    private suspend fun saveEvent(entity: EventEntity): EventEntity {
        var saved = entity
        eventDataSource.upsertEvent(entity.id) { sortOrder -> entity.copy(sortOrder = sortOrder).also { saved = it } }
        return saved
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `推しは登録した順に並び保存し直しても並びと結び付いた予定は変わらない`() = testScope.runTest {
        saveOshi(luminaId, "pink")
        saveOshi(minatoId, "blue")
        val live = saveEvent(event("5th LIVE", luminaId))

        saveOshi(luminaId, "purple")

        assertEquals(
            listOf(oshi(luminaId, "purple", sortOrder = 1), oshi(minatoId, "blue", sortOrder = 2)),
            oshiDataSource.oshis.first(),
        )
        assertEquals(listOf(live), eventDataSource.events.first())
    }

    @Test
    fun `推しを消すと結び付いた予定も消える`() = testScope.runTest {
        saveOshi(luminaId, "pink")
        saveOshi(minatoId, "blue")
        saveEvent(event("LUMINA LIVE", luminaId))
        val minatoLive = saveEvent(event("ミナト LIVE", minatoId))
        val unlinked = saveEvent(event("推しなし", null))

        oshiDataSource.deleteOshi(luminaId)

        assertEquals(listOf(minatoId), oshiDataSource.oshis.first().map { it.id })
        assertEquals(listOf(minatoLive, unlinked), eventDataSource.events.first())
    }

    @Test
    fun `結び付く推しが無い予定の保存はIOException`() = testScope.runTest {
        val failure = runCatching { saveEvent(event("A", UUID.randomUUID())) }.exceptionOrNull()

        assertTrue(failure is IOException)
        assertEquals(emptyList<EventEntity>(), eventDataSource.events.first())
    }
}
