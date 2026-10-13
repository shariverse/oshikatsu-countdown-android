package com.basekeita.oshikatsucountdown.domain.oshi

import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.model.OshiColor
import com.basekeita.oshikatsucountdown.data.definition.repository.OshiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.UUID

class SaveOshiUseCaseTest {
    @Test
    fun `新しい推しを名前を整えて保存する`() = runTest {
        val oshiRepository = FakeOshiRepository()
        val saveOshi = SaveOshiUseCase(oshiRepository, RecordingPhotoRepository())

        val saved = saveOshi(current = null, name = "  ミナト ", color = OshiColor.BLUE, photoChange = PhotoChange.Keep)

        assertEquals(listOf(saved), oshiRepository.oshis.value)
        assertEquals("ミナト", saved?.name)
        assertEquals(OshiColor.BLUE, saved?.color)
        assertNull(saved?.photoPath)
    }

    @Test
    fun `名前が空なら保存しない`() = runTest {
        val oshiRepository = FakeOshiRepository()
        val saveOshi = SaveOshiUseCase(oshiRepository, RecordingPhotoRepository())

        assertNull(saveOshi(current = null, name = " 　", color = OshiColor.PINK, photoChange = PhotoChange.Keep))
        assertEquals(emptyList<Oshi>(), oshiRepository.oshis.value)
    }

    @Test
    fun `写真を差し替えたら取り込んで前の写真を消す`() = runTest {
        val current = Oshi(name = "ひより", color = OshiColor.ORANGE, photoPath = "/photos/old.jpg")
        val oshiRepository = FakeOshiRepository(listOf(current))
        val photoRepository = RecordingPhotoRepository(importedPath = "/photos/new.jpg")
        val saveOshi = SaveOshiUseCase(oshiRepository, photoRepository)

        val saved = saveOshi(current, "ひより", OshiColor.ORANGE, PhotoChange.Replace("content://picked/1"))

        assertEquals(current.id, saved?.id)
        assertEquals("/photos/new.jpg", oshiRepository.oshis.value.single().photoPath)
        assertEquals(listOf("content://picked/1"), photoRepository.importedUris)
        assertEquals(listOf("/photos/old.jpg"), photoRepository.deletedPaths)
    }

    @Test
    fun `取り込めなければ今の写真のまま保存する`() = runTest {
        val current = Oshi(name = "ひより", color = OshiColor.ORANGE, photoPath = "/photos/old.jpg")
        val oshiRepository = FakeOshiRepository(listOf(current))
        val photoRepository = RecordingPhotoRepository(importedPath = null)
        val saveOshi = SaveOshiUseCase(oshiRepository, photoRepository)

        saveOshi(current, "ひより", OshiColor.RED, PhotoChange.Replace("content://picked/1"))

        assertEquals("/photos/old.jpg", oshiRepository.oshis.value.single().photoPath)
        assertEquals(OshiColor.RED, oshiRepository.oshis.value.single().color)
        assertEquals(emptyList<String>(), photoRepository.deletedPaths)
    }

    @Test
    fun `写真を外したら消す`() = runTest {
        val current = Oshi(name = "ひより", color = OshiColor.ORANGE, photoPath = "/photos/old.jpg")
        val oshiRepository = FakeOshiRepository(listOf(current))
        val photoRepository = RecordingPhotoRepository()
        val saveOshi = SaveOshiUseCase(oshiRepository, photoRepository)

        saveOshi(current, "ひより", OshiColor.ORANGE, PhotoChange.Remove)

        assertNull(oshiRepository.oshis.value.single().photoPath)
        assertEquals(listOf("/photos/old.jpg"), photoRepository.deletedPaths)
    }

    @Test
    fun `保存に失敗したら取り込んだ写真を消して前の写真は残す`() = runTest {
        val current = Oshi(name = "ひより", color = OshiColor.ORANGE, photoPath = "/photos/old.jpg")
        val photoRepository = RecordingPhotoRepository(importedPath = "/photos/new.jpg")
        val saveOshi = SaveOshiUseCase(FailingOshiRepository(listOf(current)), photoRepository)

        val thrown = runCatching { saveOshi(current, "ひより", OshiColor.ORANGE, PhotoChange.Replace("content://picked/1")) }

        assertTrue(thrown.exceptionOrNull() is IOException)
        assertEquals(listOf("/photos/new.jpg"), photoRepository.deletedPaths)
    }

    /** 書き込みが必ず失敗する `OshiRepository`。 */
    private class FailingOshiRepository(
        initial: List<Oshi>,
    ) : OshiRepository {
        override val oshis = MutableStateFlow(initial)

        override suspend fun save(oshi: Oshi): Unit = throw IOException("disk full")

        override suspend fun delete(id: UUID): Unit = throw IOException("disk full")
    }
}
