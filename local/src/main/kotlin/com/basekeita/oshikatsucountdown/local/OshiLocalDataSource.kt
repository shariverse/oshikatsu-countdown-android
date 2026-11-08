package com.basekeita.oshikatsucountdown.local

import com.basekeita.oshikatsucountdown.local.database.OshiDao
import com.basekeita.oshikatsucountdown.local.database.model.OshiEntity
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

/** 推しの読み書き（Room の `oshis`）。 */
class OshiLocalDataSource
@Inject
constructor(
    private val oshiDao: OshiDao,
) {
    /** 登録した順。読めないときは空。 */
    val oshis: Flow<List<OshiEntity>> = oshiDao.getOshiEntities().emptyOnDatabaseError()

    /** 同じ [id] があれば置き換え、無ければ末尾に足す。行は [createEntity] に登録した順を渡して作る。 */
    @Throws(IOException::class)
    suspend fun upsertOshi(
        id: UUID,
        createEntity: (sortOrder: Long) -> OshiEntity,
    ) {
        writeDatabase { oshiDao.upsertOshi(id, createEntity) }
    }

    /** 削除する。結び付いた予定も同じ書き込みで消える。 */
    @Throws(IOException::class)
    suspend fun deleteOshi(id: UUID) {
        writeDatabase { oshiDao.deleteOshi(id) }
    }
}
