package com.basekeita.oshikatsucountdown.data.impl.repository

import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.model.OshiColor
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
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class OshiRepositoryImplTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val storages = TestStorages()
    private val repository by lazy { OshiRepositoryImpl(storages.oshiDataSource, testDispatcher) }

    @After
    fun closeDatabase() {
        storages.database.close()
    }

    @Test
    fun `登録した順に戻り同じidは置き換え削除できる`() = testScope.runTest {
        val lumina = Oshi(name = "LUMINA", color = OshiColor.PINK, photoPath = "/photos/a.jpg")
        val minato = Oshi(name = "ミナト", color = OshiColor.BLUE)
        repository.save(lumina)
        repository.save(minato)
        repository.save(lumina.copy(color = OshiColor.PURPLE))

        assertEquals(listOf(lumina.copy(color = OshiColor.PURPLE), minato), repository.oshis.first())

        repository.delete(lumina.id)
        assertEquals(listOf(minato), repository.oshis.first())
    }

    @Test
    fun `知らない色は既定の色で読み名前が空の推しは読み飛ばす`() = testScope.runTest {
        val id = UUID.randomUUID()
        val blankId = UUID.randomUUID()
        storages.oshiDataSource.upsertOshi(id) { sortOrder -> OshiEntity(id, "ひより", "gold", null, sortOrder) }
        storages.oshiDataSource.upsertOshi(blankId) { sortOrder -> OshiEntity(blankId, " ", "red", null, sortOrder) }

        assertEquals(listOf(Oshi(id = id, name = "ひより", color = OshiColor.DEFAULT)), repository.oshis.first())
    }
}
