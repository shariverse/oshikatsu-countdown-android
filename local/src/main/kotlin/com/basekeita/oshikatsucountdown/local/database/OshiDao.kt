package com.basekeita.oshikatsucountdown.local.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.basekeita.oshikatsucountdown.local.database.model.OshiEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
abstract class OshiDao {
    /** 登録した順。 */
    @Query("SELECT * FROM oshis ORDER BY sortOrder")
    abstract fun getOshiEntities(): Flow<List<OshiEntity>>

    /**
     * 同じ [id] があれば置き換え、無ければ末尾に足す。行は [createEntity] に登録した順を渡して作る
     * （既にある推しは今の順、新しい推しは末尾の次）。
     * 行を消して入れ直す置き換え（REPLACE）は、並びが変わるうえ結び付いた予定まで消すため使わない。
     */
    @Transaction
    open suspend fun upsertOshi(
        id: UUID,
        createEntity: (sortOrder: Long) -> OshiEntity,
    ) {
        upsertOshiEntity(createEntity(getSortOrder(id) ?: getNextSortOrder()))
    }

    /** 削除する。結び付いた予定も消える。該当が無いときは何もしない。 */
    @Query("DELETE FROM oshis WHERE id = :id")
    abstract suspend fun deleteOshi(id: UUID)

    @Query("SELECT sortOrder FROM oshis WHERE id = :id")
    protected abstract suspend fun getSortOrder(id: UUID): Long?

    @Query("SELECT IFNULL(MAX(sortOrder), 0) + 1 FROM oshis")
    protected abstract suspend fun getNextSortOrder(): Long

    @Upsert
    protected abstract suspend fun upsertOshiEntity(entity: OshiEntity)
}
