package com.basekeita.oshikatsucountdown.domain.oshi

import com.basekeita.oshikatsucountdown.data.definition.model.EventType
import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.model.OshiColor
import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class DeleteOshiUseCaseTest {
    @Test
    fun `推しと結び付いた予定と写真を消す`() = runTest {
        val target = Oshi(name = "LUMINA", color = OshiColor.PINK, photoPath = "/photos/lumina.jpg")
        val other = Oshi(name = "ミナト", color = OshiColor.BLUE)
        val targetEvent = OshiEvent(
            oshiId = target.id,
            type = EventType.LIVE,
            title = "5th LIVE",
            date = LocalDate.of(2026, 10, 18),
            createdAt = Instant.EPOCH,
        )
        val otherEvent = targetEvent.copy(id = UUID.randomUUID(), oshiId = other.id, title = "写真集")
        val oshiRepository = FakeOshiRepository(listOf(target, other))
        val eventRepository = FakeEventRepository(listOf(targetEvent, otherEvent))
        val photoRepository = RecordingPhotoRepository()

        DeleteOshiUseCase(oshiRepository, eventRepository, photoRepository)(target)

        assertEquals(listOf(other), oshiRepository.oshis.value)
        assertEquals(listOf(otherEvent), eventRepository.events.value)
        assertEquals(listOf("/photos/lumina.jpg"), photoRepository.deletedPaths)
    }
}
