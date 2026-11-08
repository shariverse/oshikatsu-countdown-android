package com.basekeita.oshikatsucountdown.local.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * 予定の行。[oshiId] の推しを消すと、この予定も消える。
 *
 * @property type 予定の種類の保存のキー（`bday` など。対応は data:impl）。
 * @property sortOrder 登録した順。同じ id を保存し直しても変えない。
 */
@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = OshiEntity::class,
            parentColumns = ["id"],
            childColumns = ["oshiId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("oshiId")],
)
data class EventEntity(
    @PrimaryKey val id: UUID,
    val oshiId: UUID?,
    val type: String,
    val title: String,
    val date: LocalDate,
    val startTime: LocalTime?,
    val venue: String,
    val repeatsYearly: Boolean,
    val pinned: Boolean,
    val createdAt: Instant,
    val sortOrder: Long,
)
