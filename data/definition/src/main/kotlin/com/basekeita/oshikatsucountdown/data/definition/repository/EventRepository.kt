package com.basekeita.oshikatsucountdown.data.definition.repository

import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** 予定の一覧。並びは登録した順（表示の順は UseCase が決める）。 */
interface EventRepository {
    /** 保存済みの予定。無い・読めないときは空。 */
    val events: Flow<List<OshiEvent>>

    /** 同じ id があれば置き換え、無ければ末尾に足す。ピン留めした予定を保存したときは、ほかの予定のピン留めを外す。 */
    suspend fun save(event: OshiEvent)

    /** 削除する。該当が無いときは何もしない。 */
    suspend fun delete(id: UUID)

    /** 推しに結び付いた予定をまとめて削除する。 */
    suspend fun deleteByOshi(oshiId: UUID)

    /** ピン留めを切り替える。ピン留めするときは、ほかの予定のピン留めを外す。 */
    suspend fun setPinned(
        id: UUID,
        pinned: Boolean,
    )
}
