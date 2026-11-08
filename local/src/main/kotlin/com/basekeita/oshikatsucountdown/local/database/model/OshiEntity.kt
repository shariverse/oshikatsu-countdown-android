package com.basekeita.oshikatsucountdown.local.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 推しの行。
 *
 * @property color 推し色の保存のキー（`pink` など。対応は data:impl）。
 * @property photoPath 取り込んだ写真の絶対パス。
 * @property sortOrder 登録した順。同じ id を保存し直しても変えない。
 */
@Entity(tableName = "oshis")
data class OshiEntity(
    @PrimaryKey val id: UUID,
    val name: String,
    val color: String,
    val photoPath: String?,
    val sortOrder: Long,
)
