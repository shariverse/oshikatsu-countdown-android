package com.basekeita.oshikatsucountdown.data.impl.repository

import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import com.basekeita.oshikatsucountdown.data.definition.repository.EventRepository
import com.basekeita.oshikatsucountdown.data.impl.DefaultDispatcher
import com.basekeita.oshikatsucountdown.data.impl.mapping.toEntity
import com.basekeita.oshikatsucountdown.data.impl.mapping.toModel
import com.basekeita.oshikatsucountdown.local.EventLocalDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

internal class EventRepositoryImpl
@Inject
constructor(
    private val localDataSource: EventLocalDataSource,
    @param:DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
) : EventRepository {
    override val events: Flow<List<OshiEvent>> =
        localDataSource.events
            .map { entities -> entities.map { it.toModel() } }
            // 全件の整形を、購読する ViewModel の Main で走らせない
            .flowOn(defaultDispatcher)

    override suspend fun save(event: OshiEvent) {
        localDataSource.upsertEvent(event.id) { sortOrder -> event.toEntity(sortOrder) }
    }

    override suspend fun delete(id: UUID) {
        localDataSource.deleteEvent(id)
    }

    override suspend fun deleteByOshi(oshiId: UUID) {
        localDataSource.deleteEventsByOshi(oshiId)
    }

    override suspend fun setPinned(
        id: UUID,
        pinned: Boolean,
    ) {
        localDataSource.setPinned(id, pinned)
    }
}
