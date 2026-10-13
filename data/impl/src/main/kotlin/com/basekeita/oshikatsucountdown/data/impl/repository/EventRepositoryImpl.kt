package com.basekeita.oshikatsucountdown.data.impl.repository

import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import com.basekeita.oshikatsucountdown.data.definition.repository.EventRepository
import com.basekeita.oshikatsucountdown.data.impl.DefaultDispatcher
import com.basekeita.oshikatsucountdown.data.impl.toEvents
import com.basekeita.oshikatsucountdown.data.impl.toStored
import com.basekeita.oshikatsucountdown.local.OshikatsuLocalDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

internal class EventRepositoryImpl
@Inject
constructor(
    private val localDataSource: OshikatsuLocalDataSource,
    @param:DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
) : EventRepository {
    override val events: Flow<List<OshiEvent>> =
        localDataSource.events
            .map { stored -> stored?.toEvents().orEmpty() }
            // 全件の JSON のデコードと整形を、購読する ViewModel の Main で走らせない
            .flowOn(defaultDispatcher)

    override suspend fun save(event: OshiEvent) {
        updateEvents { current ->
            val replaced = if (current.any { it.id == event.id }) {
                current.map { existing ->
                    if (existing.id == event.id) {
                        event
                    } else {
                        existing
                    }
                }
            } else {
                current + event
            }
            if (event.pinned) {
                replaced.pinnedOnly(event.id)
            } else {
                replaced
            }
        }
    }

    override suspend fun delete(id: UUID) {
        updateEvents { current -> current.filter { it.id != id } }
    }

    override suspend fun deleteByOshi(oshiId: UUID) {
        updateEvents { current -> current.filter { it.oshiId != oshiId } }
    }

    override suspend fun setPinned(
        id: UUID,
        pinned: Boolean,
    ) {
        updateEvents { current ->
            if (current.none { it.id == id }) {
                current
            } else if (pinned) {
                current.pinnedOnly(id)
            } else {
                current.map { existing ->
                    if (existing.id == id) {
                        existing.copy(pinned = false)
                    } else {
                        existing
                    }
                }
            }
        }
    }

    private suspend fun updateEvents(transform: (List<OshiEvent>) -> List<OshiEvent>) {
        localDataSource.updateEvents { stored -> transform(stored.toEvents()).map { it.toStored() } }
    }
}

/** `id` の予定だけをピン留めにする。 */
private fun List<OshiEvent>.pinnedOnly(id: UUID): List<OshiEvent> = map { existing -> existing.copy(pinned = existing.id == id) }
