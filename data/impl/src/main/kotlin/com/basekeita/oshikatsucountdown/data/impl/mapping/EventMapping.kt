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

/** 知らない種類は「その他」にする。 */
private fun eventTypeOf(storageKey: String): EventType = EventType.entries.firstOrNull { it.storageKey == storageKey } ?: EventType.OTHER

/** タイトルが空になる予定は読み飛ばす。 */
internal fun EventEntity.toModelOrNull(): OshiEvent? {
    val normalizedTitle = OshiEvent.normalizeTitle(title)
    if (normalizedTitle.isEmpty()) return null

    return OshiEvent(
        id = id,
        oshiId = oshiId,
        type = eventTypeOf(type),
        title = normalizedTitle,
        date = date,
        startTime = startTime,
        venue = OshiEvent.normalizeVenue(venue),
        repeatsYearly = repeatsYearly,
        pinned = pinned,
        createdAt = createdAt,
    )
}

internal fun OshiEvent.toEntity(sortOrder: Long): EventEntity = EventEntity(
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
    sortOrder = sortOrder,
)
