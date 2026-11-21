package com.basekeita.oshikatsucountdown.data.impl.repository

import com.basekeita.oshikatsucountdown.data.definition.model.EventType
import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import com.basekeita.oshikatsucountdown.local.database.model.OshiEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class EventRepositoryImplTest {
    private val testScope = TestScope(UnconfinedTestDispatcher())
    private val storages = TestStorages()
    private val repository by lazy { EventRepositoryImpl(storages.eventDataSource) }

    @After
    fun closeDatabase() {
        storages.database.close()
    }

    /** 登録した順に並ぶよう、作るたびに 1 ミリ秒ずつ後の時刻にする。 */
    private var nextCreatedAt = Instant.ofEpochMilli(1_790_000_000_000)

    private fun event(
        title: String,
        pinned: Boolean = false,
        oshiId: UUID? = null,
    ) = OshiEvent(
        oshiId = oshiId,
        type = EventType.LIVE,
        title = title,
        date = LocalDate.of(2026, 10, 18),
        startTime = LocalTime.of(17, 0),
        venue = "東京ドーム",
        pinned = pinned,
        createdAt = nextCreatedAt.also { nextCreatedAt = it.plusMillis(1) },
    )

    @Test
    fun `保存した予定はそのまま戻り同じidは置き換える`() = testScope.runTest {
        val live = event("5th LIVE")
        repository.save(live)
        repository.save(event("写真集"))
        repository.save(live.copy(title = "5th LIVE 東京公演"))

        val events = repository.events.first()

        assertEquals(listOf("5th LIVE 東京公演", "写真集"), events.map { it.title })
        assertEquals(live.copy(title = "5th LIVE 東京公演"), events.first())
    }

    @Test
    fun `ピン留めした予定を保存するとほかのピン留めを外す`() = testScope.runTest {
        val first = event("A", pinned = true)
        val second = event("B")
        repository.save(first)
        repository.save(second)

        repository.save(second.copy(pinned = true))

        assertEquals(listOf(false, true), repository.events.first().map { it.pinned })
    }

    @Test
    fun `ピン留めの切り替え`() = testScope.runTest {
        val first = event("A", pinned = true)
        val second = event("B")
        repository.save(first)
        repository.save(second)

        repository.setPinned(second.id, true)
        assertEquals(listOf(false, true), repository.events.first().map { it.pinned })

        repository.setPinned(second.id, false)
        assertEquals(listOf(false, false), repository.events.first().map { it.pinned })

        repository.setPinned(UUID.randomUUID(), true)
        assertEquals(listOf(false, false), repository.events.first().map { it.pinned })
    }

    @Test
    fun `削除できる`() = testScope.runTest {
        val oshiId = UUID.randomUUID()
        storages.oshiDataSource.upsertOshi(OshiEntity(oshiId, "LUMINA", "pink", null, Instant.EPOCH))
        val a = event("A", oshiId = oshiId)
        val b = event("B", oshiId = oshiId)
        val c = event("C")
        listOf(a, b, c).forEach { repository.save(it) }

        repository.delete(a.id)
        assertEquals(listOf("B", "C"), repository.events.first().map { it.title })
    }
}
