package com.basekeita.oshikatsucountdown.data.impl.mapping

import com.basekeita.oshikatsucountdown.data.definition.model.EventType
import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import com.basekeita.oshikatsucountdown.local.database.model.EventEntity

/**
 * データベースに保存するキー。保存の形はデータ層が決め、種類の名前を変えても保存済みの値が変わらないようにする。
 * キーを変えるときは、データベースの版を上げて保存済みの値を移す。
 */
internal val EventType.storageKey: String
    get() = when (this) {
        EventType.LIVE -> "live"
        EventType.BIRTHDAY -> "bday"
        EventType.RELEASE -> "release"
        EventType.STREAM -> "stream"
        EventType.OTHER -> "other"
    }

internal fun EventEntity.toModel(): OshiEvent = OshiEvent(
    id = id,
    oshiId = oshiId,
    type = EventType.entries.first { it.storageKey == type },
    title = title,
    date = date,
    startTime = startTime,
    venue = venue,
    repeatsYearly = repeatsYearly,
    pinned = pinned,
    createdAt = createdAt,
)

internal fun OshiEvent.toEntity(): EventEntity = EventEntity(
    id = id,
    oshiId = oshiId,
    type = type.storageKey,
    title = title,
    date = date,
    startTime = startTime,
    venue = venue,
    repeatsYearly = repeatsYearly,
    pinned = pinned,
    createdAt = createdAt,
)
