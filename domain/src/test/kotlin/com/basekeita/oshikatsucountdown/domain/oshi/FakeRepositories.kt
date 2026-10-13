package com.basekeita.oshikatsucountdown.domain.oshi

import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import com.basekeita.oshikatsucountdown.data.definition.repository.EventRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.OshiRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

/** メモリに持つ `OshiRepository`。 */
internal class FakeOshiRepository(
    initial: List<Oshi> = emptyList(),
) : OshiRepository {
    override val oshis = MutableStateFlow(initial)

    override suspend fun save(oshi: Oshi) {
        val current = oshis.value
        oshis.value = if (current.any { it.id == oshi.id }) {
            current.map { existing ->
                if (existing.id == oshi.id) {
                    oshi
                } else {
                    existing
                }
            }
        } else {
            current + oshi
        }
    }

    override suspend fun delete(id: UUID) {
        oshis.value = oshis.value.filter { it.id != id }
    }
}

/** メモリに持つ `EventRepository`。ピン留めの規則は持たない。 */
internal class FakeEventRepository(
    initial: List<OshiEvent> = emptyList(),
) : EventRepository {
    override val events = MutableStateFlow(initial)

    override suspend fun save(event: OshiEvent) {
        events.value = events.value.filter { it.id != event.id } + event
    }

    override suspend fun delete(id: UUID) {
        events.value = events.value.filter { it.id != id }
    }

    override suspend fun deleteByOshi(oshiId: UUID) {
        events.value = events.value.filter { it.oshiId != oshiId }
    }

    override suspend fun setPinned(
        id: UUID,
        pinned: Boolean,
    ) {
        events.value = events.value.map { event ->
            if (event.id == id) {
                event.copy(pinned = pinned)
            } else {
                event
            }
        }
    }
}

/** 取り込みと削除を記録する `PhotoRepository`。`importedPath` が null なら取り込みに失敗する。 */
internal class RecordingPhotoRepository(
    private val importedPath: String? = "/photos/new.jpg",
) : PhotoRepository {
    val importedUris = mutableListOf<String>()
    val deletedPaths = mutableListOf<String>()

    override suspend fun import(sourceUri: String): String? {
        importedUris += sourceUri
        return importedPath
    }

    override suspend fun delete(path: String) {
        deletedPaths += path
    }
}
