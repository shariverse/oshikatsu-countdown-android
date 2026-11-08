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
    @Query("SELECT * FROM events ORDER BY sortOrder")
    abstract fun getEventEntities(): Flow<List<EventEntity>>

    /**
     * 同じ [id] があれば置き換え、無ければ末尾に足す。行は [createEntity] に登録した順を渡して作る
     * （既にある予定は今の順、新しい予定は末尾の次）。ピン留めした予定なら、ほかの予定のピン留めを外す。
     */
    @Transaction
    open suspend fun upsertEvent(
        id: UUID,
        createEntity: (sortOrder: Long) -> EventEntity,
    ) {
        val entity = createEntity(getSortOrder(id) ?: getNextSortOrder())
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

    @Query("DELETE FROM events WHERE oshiId = :oshiId")
    abstract suspend fun deleteEventsByOshi(oshiId: UUID)

    @Query("SELECT sortOrder FROM events WHERE id = :id")
    protected abstract suspend fun getSortOrder(id: UUID): Long?

    @Query("SELECT IFNULL(MAX(sortOrder), 0) + 1 FROM events")
    protected abstract suspend fun getNextSortOrder(): Long

    @Upsert
    protected abstract suspend fun upsertEventEntity(entity: EventEntity)
}
