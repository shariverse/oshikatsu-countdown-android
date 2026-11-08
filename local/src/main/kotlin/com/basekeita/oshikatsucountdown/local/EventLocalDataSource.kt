package com.basekeita.oshikatsucountdown.local

import com.basekeita.oshikatsucountdown.local.database.EventDao
import com.basekeita.oshikatsucountdown.local.database.model.EventEntity
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

/** 予定の読み書き（Room の `events`）。 */
class EventLocalDataSource
@Inject
constructor(
    private val eventDao: EventDao,
) {
    /** 登録した順。読めないときは空。 */
    val events: Flow<List<EventEntity>> = eventDao.getEventEntities().emptyOnDatabaseError()

    /**
     * 同じ id があれば書き換え、無ければ足す。ピン留めした予定なら、ほかの予定のピン留めを外す。
     * 結び付く推しが無い予定は保存できない（[IOException]）。
     */
    @Throws(IOException::class)
    suspend fun upsertEvent(entity: EventEntity) {
        writeDatabase { eventDao.upsertEvent(entity) }
    }

    @Throws(IOException::class)
    suspend fun deleteEvent(id: UUID) {
        writeDatabase { eventDao.deleteEvent(id) }
    }

    @Throws(IOException::class)
    suspend fun deleteEventsByOshi(oshiId: UUID) {
        writeDatabase { eventDao.deleteEventsByOshi(oshiId) }
    }

    /** ピン留めするときは、ほかの予定のピン留めを外す。該当が無いときは何もしない。 */
    @Throws(IOException::class)
    suspend fun setPinned(
        id: UUID,
        pinned: Boolean,
    ) {
        writeDatabase {
            if (pinned) {
                eventDao.pinOnly(id)
            } else {
                eventDao.unpin(id)
            }
        }
    }
}
