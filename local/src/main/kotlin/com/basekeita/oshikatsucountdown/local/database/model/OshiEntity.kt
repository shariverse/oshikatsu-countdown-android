package com.basekeita.oshikatsucountdown.local.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/**
 * 推しの行。
 *
 * @property color 推し色の保存のキー（`pink` など。対応は data:impl）。
 * @property photoPath 取り込んだ写真の絶対パス。
 * @property createdAt 登録した時刻。一覧はこの順に並べる。
 */
@Entity(tableName = "oshis")
data class OshiEntity(
    @PrimaryKey val id: UUID,
    val name: String,
    val color: String,
    val photoPath: String?,
    val createdAt: Instant,
)
