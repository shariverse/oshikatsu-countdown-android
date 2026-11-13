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

    private val lumina = oshi("LUMINA", "pink", registeredSecond = 1)
    private val minato = oshi("ミナト", "blue", registeredSecond = 2)

    private fun oshi(
        name: String,
        color: String,
        registeredSecond: Long,
    ) = OshiEntity(
        id = UUID.randomUUID(),
        name = name,
        color = color,
        photoPath = null,
        createdAt = Instant.ofEpochSecond(registeredSecond),
    )

    private fun event(
        title: String,
        oshiId: UUID?,
        registeredSecond: Long,
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
        createdAt = Instant.ofEpochSecond(registeredSecond),
    )

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `推しは登録した順に並び保存し直しても結び付いた予定は消えない`() = testScope.runTest {
        oshiDataSource.upsertOshi(minato)
        oshiDataSource.upsertOshi(lumina)
        val live = event("5th LIVE", lumina.id, registeredSecond = 1)
        eventDataSource.upsertEvent(live)

        oshiDataSource.upsertOshi(lumina.copy(color = "purple"))

        assertEquals(listOf(lumina.copy(color = "purple"), minato), oshiDataSource.oshis.first())
        assertEquals(listOf(live), eventDataSource.events.first())
    }

    @Test
    fun `推しを消すと結び付いた予定も消える`() = testScope.runTest {
        oshiDataSource.upsertOshi(lumina)
        oshiDataSource.upsertOshi(minato)
        val luminaLive = event("LUMINA LIVE", lumina.id, registeredSecond = 1)
        val minatoLive = event("ミナト LIVE", minato.id, registeredSecond = 2)
        val unlinked = event("推しなし", null, registeredSecond = 3)
        listOf(luminaLive, minatoLive, unlinked).forEach { eventDataSource.upsertEvent(it) }

        oshiDataSource.deleteOshi(lumina.id)

        assertEquals(listOf(minato), oshiDataSource.oshis.first())
        assertEquals(listOf(minatoLive, unlinked), eventDataSource.events.first())
    }

    @Test
    fun `結び付く推しが無い予定の保存はIOException`() = testScope.runTest {
        val failure = runCatching { eventDataSource.upsertEvent(event("A", UUID.randomUUID(), registeredSecond = 1)) }.exceptionOrNull()

        assertTrue(failure is IOException)
        assertEquals(emptyList<EventEntity>(), eventDataSource.events.first())
    }
}
