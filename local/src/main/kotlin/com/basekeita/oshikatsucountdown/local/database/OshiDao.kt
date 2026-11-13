package com.basekeita.oshikatsucountdown.local.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.basekeita.oshikatsucountdown.local.database.model.OshiEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface OshiDao {
    /** 登録した順。 */
    @Query("SELECT * FROM oshis ORDER BY createdAt")
    fun getOshiEntities(): Flow<List<OshiEntity>>

    /**
     * 同じ id があれば書き換え、無ければ足す。
     * 行を消して入れ直す置き換え（REPLACE）は、結び付いた予定まで消すため使わない。
     */
    @Upsert
    suspend fun upsertOshi(entity: OshiEntity)

    /** 削除する。結び付いた予定も消える。該当が無いときは何もしない。 */
    @Query("DELETE FROM oshis WHERE id = :id")
    suspend fun deleteOshi(id: UUID)
}
