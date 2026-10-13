package com.basekeita.oshikatsucountdown.domain.countdown

import com.basekeita.oshikatsucountdown.data.definition.model.EventType
import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** テスト用の予定。指定しない値は固定の既定値。 */
internal fun event(
    date: LocalDate,
    startTime: LocalTime? = null,
    repeatsYearly: Boolean = false,
    pinned: Boolean = false,
    title: String = "予定",
    createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
): OshiEvent = OshiEvent(
    oshiId = null,
    type = EventType.LIVE,
    title = title,
    date = date,
    startTime = startTime,
    repeatsYearly = repeatsYearly,
    pinned = pinned,
    createdAt = createdAt,
)
