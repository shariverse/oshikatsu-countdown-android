package com.basekeita.oshikatsucountdown.data.definition.repository

import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** 推しの一覧。並びは登録した順。 */
interface OshiRepository {
    /** 保存済みの推し。無い・読めないときは空。 */
    val oshis: Flow<List<Oshi>>

    /** 同じ id があれば置き換え、無ければ末尾に足す。 */
    suspend fun save(oshi: Oshi)

    /** 削除する。該当が無いときは何もしない。 */
    suspend fun delete(id: UUID)
}
