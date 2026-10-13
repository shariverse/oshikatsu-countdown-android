package com.basekeita.oshikatsucountdown.data.impl.repository

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.model.OshiColor
import com.basekeita.oshikatsucountdown.local.OshikatsuLocalDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class OshiRepositoryImplTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val dataStore by lazy { testDataStore(temporaryFolder.root, testScope.backgroundScope) }
    private val repository by lazy { OshiRepositoryImpl(OshikatsuLocalDataSource(dataStore), testDispatcher) }

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
        dataStore.edit {
            it[stringPreferencesKey("oshis")] =
                """[{"id":"$id","name":"ひより","color":"gold"},{"id":"${UUID.randomUUID()}","name":" ","color":"red"}]"""
        }

        assertEquals(listOf(Oshi(id = id, name = "ひより", color = OshiColor.DEFAULT)), repository.oshis.first())
    }
}
