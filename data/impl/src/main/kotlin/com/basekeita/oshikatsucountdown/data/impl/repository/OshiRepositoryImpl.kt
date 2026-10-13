package com.basekeita.oshikatsucountdown.data.impl.repository

import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.repository.OshiRepository
import com.basekeita.oshikatsucountdown.data.impl.DefaultDispatcher
import com.basekeita.oshikatsucountdown.data.impl.toOshis
import com.basekeita.oshikatsucountdown.data.impl.toStored
import com.basekeita.oshikatsucountdown.local.OshikatsuLocalDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

internal class OshiRepositoryImpl
@Inject
constructor(
    private val localDataSource: OshikatsuLocalDataSource,
    @param:DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
) : OshiRepository {
    override val oshis: Flow<List<Oshi>> =
        localDataSource.oshis
            .map { stored -> stored?.toOshis().orEmpty() }
            // 全件の JSON のデコードと整形を、購読する ViewModel の Main で走らせない
            .flowOn(defaultDispatcher)

    override suspend fun save(oshi: Oshi) {
        localDataSource.updateOshis { stored ->
            val current = stored.toOshis()
            val updated = if (current.any { it.id == oshi.id }) {
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
            updated.map { it.toStored() }
        }
    }

    override suspend fun delete(id: UUID) {
        localDataSource.updateOshis { stored ->
            stored.toOshis().filter { it.id != id }.map { it.toStored() }
        }
    }
}
