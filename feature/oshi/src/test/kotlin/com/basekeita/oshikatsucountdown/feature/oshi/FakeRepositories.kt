package com.basekeita.oshikatsucountdown.feature.oshi

import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import com.basekeita.oshikatsucountdown.data.definition.repository.EventRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.OshiRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException
import java.util.UUID

/**
 * メモリに持つ `OshiRepository`。`failsToWrite` を立てると保存・削除で `IOException` を投げる。
 * [linkedEvents] を渡すと、推しを消したときに結び付いた予定も消す（データベースの外部キーと同じ）。
 */
internal class FakeOshiRepository(
    initial: List<Oshi> = emptyList(),
    private val linkedEvents: FakeEventRepository? = null,
) : OshiRepository {
    override val oshis = MutableStateFlow(initial)
    var failsToWrite = false

    override suspend fun save(oshi: Oshi) {
        if (failsToWrite) throw IOException("disk full")

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
        if (failsToWrite) throw IOException("disk full")

        oshis.value = oshis.value.filter { it.id != id }
        linkedEvents?.let { events -> events.events.value = events.events.value.filter { it.oshiId != id } }
    }
}

/** メモリに持つ `EventRepository`。 */
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

    override suspend fun setPinned(
        id: UUID,
        pinned: Boolean,
    ) = Unit
}

/** 取り込みと削除を記録する `PhotoRepository`。取り込んだ写真は `/photos/<URI の末尾>.jpg` に置いたことにする。 */
internal class RecordingPhotoRepository : PhotoRepository {
    val deletedPaths = mutableListOf<String>()

    override suspend fun import(sourceUri: String): String = "/photos/${sourceUri.substringAfterLast('/')}.jpg"

    override suspend fun delete(path: String) {
        deletedPaths += path
    }
}
