package com.basekeita.oshikatsucountdown.data.impl.repository

import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.repository.OshiRepository
import com.basekeita.oshikatsucountdown.data.impl.mapping.toEntity
import com.basekeita.oshikatsucountdown.data.impl.mapping.toModel
import com.basekeita.oshikatsucountdown.local.OshiLocalDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

internal class OshiRepositoryImpl
@Inject
constructor(
    private val localDataSource: OshiLocalDataSource,
) : OshiRepository {
    override val oshis: Flow<List<Oshi>> =
        localDataSource.oshis.map { entities -> entities.map { oshi -> oshi.toModel() } }

    override suspend fun save(oshi: Oshi) {
        localDataSource.upsertOshi(oshi.toEntity())
    }

    override suspend fun delete(id: UUID) {
        localDataSource.deleteOshi(id)
    }
}
