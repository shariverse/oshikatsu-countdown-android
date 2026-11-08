package com.basekeita.oshikatsucountdown.data.impl.repository

import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.model.OshiColor
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class OshiRepositoryImplTest {
    private val testScope = TestScope(UnconfinedTestDispatcher())
    private val storages = TestStorages()
    private val repository by lazy { OshiRepositoryImpl(storages.oshiDataSource) }

    @After
    fun closeDatabase() {
        storages.database.close()
    }

    @Test
    fun `登録した順に戻り同じidは置き換え削除できる`() = testScope.runTest {
        val lumina = Oshi(name = "LUMINA", color = OshiColor.PINK, photoPath = "/photos/a.jpg", createdAt = Instant.EPOCH)
        val minato = Oshi(name = "ミナト", color = OshiColor.BLUE, createdAt = Instant.EPOCH.plusSeconds(1))
        repository.save(lumina)
        repository.save(minato)
        repository.save(lumina.copy(color = OshiColor.PURPLE))

        assertEquals(listOf(lumina.copy(color = OshiColor.PURPLE), minato), repository.oshis.first())

        repository.delete(lumina.id)
        assertEquals(listOf(minato), repository.oshis.first())
    }
}
