package com.basekeita.oshikatsucountdown.local

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class OshikatsuLocalDataSourceTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val testScope = TestScope(UnconfinedTestDispatcher())

    // ファイルを渡す create は、JVM のテストでは Build.VERSION.SDK_INT が 0 になり File.renameTo で書き換えるため、
    // Windows では既存のファイルを置き換えられず 2 回目の書き込みが失敗する。OkioStorage は置き換えて書くため、テストではこちらを使う。
    private val dataStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            storage =
            OkioStorage(FileSystem.SYSTEM, PreferencesSerializer) {
                temporaryFolder.root.resolve("test.preferences_pb").toOkioPath()
            },
            corruptionHandler = null,
            migrations = emptyList(),
            scope = testScope.backgroundScope,
        )

    private val dataSource = OshikatsuLocalDataSource(dataStore)

    private val storedEvent = StoredEvent(
        id = UUID.randomUUID(),
        oshiId = UUID.randomUUID(),
        type = "live",
        title = "5th LIVE",
        date = "2026-10-18",
        startTime = "17:00",
        venue = "東京ドーム",
        repeatsYearly = false,
        pinned = true,
        createdAtEpochMillis = 1_790_000_000_000,
    )

    private suspend fun writeRaw(
        name: String,
        value: String,
    ) {
        dataStore.edit { it[stringPreferencesKey(name)] = value }
    }

    private suspend fun readRaw(name: String): String? = dataStore.data.first()[stringPreferencesKey(name)]

    @Test
    fun `保存した推しと予定はそのまま戻る`() = testScope.runTest {
        val oshi = StoredOshi(UUID.randomUUID(), "LUMINA", "pink", "/data/photo.jpg")
        dataSource.updateOshis { listOf(oshi) }
        dataSource.updateEvents { listOf(storedEvent) }

        assertEquals(listOf(oshi), dataSource.oshis.first())
        assertEquals(listOf(storedEvent), dataSource.events.first())
    }

    @Test
    fun `保存データが無ければnullで壊れていてもnull`() = testScope.runTest {
        assertNull(dataSource.oshis.first())
        assertNull(dataSource.events.first())

        writeRaw("oshis", "broken")
        writeRaw("events", """[{"id":"not-a-uuid","type":"live","title":"A","date":"2026-01-01","createdAtEpochMillis":0}]""")

        assertNull(dataSource.oshis.first())
        assertNull(dataSource.events.first())
    }

    @Test
    fun `更新には今の値を渡し無ければ空を渡す`() = testScope.runTest {
        val first = StoredOshi(UUID.randomUUID(), "A", "pink")
        dataSource.updateOshis { current ->
            assertEquals(emptyList<StoredOshi>(), current)
            listOf(first)
        }

        var received: List<StoredOshi>? = null
        dataSource.updateOshis { current ->
            received = current
            current
        }

        assertEquals(listOf(first), received)
    }

    @Test
    fun `後から足した項目が無い古い形の予定も既定値で読む`() = testScope.runTest {
        val id = UUID.randomUUID()
        writeRaw("events", """[{"id":"$id","type":"bday","title":"誕生日","date":"2000-11-10","createdAtEpochMillis":0,"future":1}]""")

        val event = dataSource.events.first()!!.single()

        assertEquals(id, event.id)
        assertNull(event.oshiId)
        assertNull(event.startTime)
        assertEquals("", event.venue)
        assertEquals(false, event.pinned)
    }

    @Test
    fun `予定の保存形式`() = testScope.runTest {
        dataSource.updateEvents { listOf(storedEvent) }

        val entry = Json.parseToJsonElement(readRaw("events")!!).jsonArray.first().jsonObject

        assertEquals(
            setOf("id", "oshiId", "type", "title", "date", "startTime", "venue", "repeatsYearly", "pinned", "createdAtEpochMillis"),
            entry.keys,
        )
    }

    @Test
    fun `設定は未設定ならnullで保存した値が戻る`() = testScope.runTest {
        assertNull(dataSource.showPastEvents.first())
        assertNull(dataSource.onboardingCompleted.first())

        dataSource.setShowPastEvents(true)
        dataSource.setOnboardingCompleted(true)

        assertEquals(true, dataSource.showPastEvents.first())
        assertEquals(true, dataSource.onboardingCompleted.first())
    }
}
