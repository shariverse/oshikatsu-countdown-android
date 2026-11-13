package com.basekeita.oshikatsucountdown.local.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.basekeita.oshikatsucountdown.local.database.model.EventEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
abstract class EventDao {
    /** 登録した順。 */
    @Query("SELECT * FROM events ORDER BY createdAt")
    abstract fun getEventEntities(): Flow<List<EventEntity>>

    /** 同じ id があれば書き換え、無ければ足す。ピン留めした予定なら、ほかの予定のピン留めを外す。 */
    @Transaction
    open suspend fun upsertEvent(entity: EventEntity) {
        upsertEventEntity(entity)
        if (entity.pinned) {
            pinOnly(entity.id)
        }
    }

    /** `id` の予定だけをピン留めにする。`id` の予定が無いときは何もしない。 */
    @Query("UPDATE events SET pinned = (id = :id) WHERE EXISTS (SELECT 1 FROM events WHERE id = :id)")
    abstract suspend fun pinOnly(id: UUID)

    @Query("UPDATE events SET pinned = 0 WHERE id = :id")
    abstract suspend fun unpin(id: UUID)

    @Query("DELETE FROM events WHERE id = :id")
    abstract suspend fun deleteEvent(id: UUID)

    @Upsert
    protected abstract suspend fun upsertEventEntity(entity: EventEntity)
}
