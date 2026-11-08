package com.basekeita.oshikatsucountdown.data.impl.repository

import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.repository.OshiRepository
import com.basekeita.oshikatsucountdown.data.impl.DefaultDispatcher
import com.basekeita.oshikatsucountdown.data.impl.mapping.toEntity
import com.basekeita.oshikatsucountdown.data.impl.mapping.toModel
import com.basekeita.oshikatsucountdown.local.OshiLocalDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

internal class OshiRepositoryImpl
@Inject
constructor(
    private val localDataSource: OshiLocalDataSource,
    @param:DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
) : OshiRepository {
    override val oshis: Flow<List<Oshi>> =
        localDataSource.oshis
            .map { entities -> entities.map { it.toModel() } }
            // 全件の整形を、購読する ViewModel の Main で走らせない
            .flowOn(defaultDispatcher)

    override suspend fun save(oshi: Oshi) {
        localDataSource.upsertOshi(oshi.id) { sortOrder -> oshi.toEntity(sortOrder) }
    }

    override suspend fun delete(id: UUID) {
        localDataSource.deleteOshi(id)
    }
}
