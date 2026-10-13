package com.basekeita.oshikatsucountdown.data.impl

import com.basekeita.oshikatsucountdown.data.definition.model.EventType
import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.model.OshiColor
import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import com.basekeita.oshikatsucountdown.local.StoredEvent
import com.basekeita.oshikatsucountdown.local.StoredOshi
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** 時刻の保存形式（`HH:mm`）。秒は持たない。 */
private val storedTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * 保存値。enum の名前から作らず書き出しておき、名前を変えても保存済みの値が読めるようにする。
 */
internal val OshiColor.storedValue: String
    get() = when (this) {
        OshiColor.PINK -> "pink"
        OshiColor.RED -> "red"
        OshiColor.ORANGE -> "orange"
        OshiColor.YELLOW -> "yellow"
        OshiColor.GREEN -> "green"
        OshiColor.MINT -> "mint"
        OshiColor.BLUE -> "blue"
        OshiColor.NAVY -> "navy"
        OshiColor.PURPLE -> "purple"
        OshiColor.LAVENDER -> "lavender"
        OshiColor.BROWN -> "brown"
        OshiColor.BLACK -> "black"
    }

internal val EventType.storedValue: String
    get() = when (this) {
        EventType.LIVE -> "live"
        EventType.BIRTHDAY -> "bday"
        EventType.RELEASE -> "release"
        EventType.STREAM -> "stream"
        EventType.OTHER -> "other"
    }

/** 知らない色は既定の色に戻す。 */
private fun oshiColorOf(storedValue: String): OshiColor = OshiColor.entries.firstOrNull { it.storedValue == storedValue } ?: OshiColor.DEFAULT

/** 知らない種類は「その他」にする。 */
private fun eventTypeOf(storedValue: String): EventType = EventType.entries.firstOrNull { it.storedValue == storedValue } ?: EventType.OTHER

/** 名前が空になる推しは読み飛ばす。 */
internal fun StoredOshi.toModelOrNull(): Oshi? {
    val normalizedName = Oshi.normalizeName(name)
    if (normalizedName.isEmpty()) return null

    return Oshi(id = id, name = normalizedName, color = oshiColorOf(color), photoPath = photoPath)
}

internal fun Oshi.toStored(): StoredOshi = StoredOshi(id = id, name = name, color = color.storedValue, photoPath = photoPath)

/** 日付が読めない、またはタイトルが空になる予定は読み飛ばす。時刻が読めないときは時刻なしとして読む。 */
internal fun StoredEvent.toModelOrNull(): OshiEvent? {
    val normalizedTitle = OshiEvent.normalizeTitle(title)
    if (normalizedTitle.isEmpty()) return null

    val parsedDate = try {
        LocalDate.parse(date)
    } catch (exception: DateTimeParseException) {
        return null
    }
    val parsedTime = startTime?.let { stored ->
        try {
            LocalTime.parse(stored, storedTimeFormatter)
        } catch (exception: DateTimeParseException) {
            null
        }
    }
    return OshiEvent(
        id = id,
        oshiId = oshiId,
        type = eventTypeOf(type),
        title = normalizedTitle,
        date = parsedDate,
        startTime = parsedTime,
        venue = OshiEvent.normalizeVenue(venue),
        repeatsYearly = repeatsYearly,
        pinned = pinned,
        createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    )
}

internal fun OshiEvent.toStored(): StoredEvent = StoredEvent(
    id = id,
    oshiId = oshiId,
    type = type.storedValue,
    title = title,
    date = date.toString(),
    startTime = startTime?.format(storedTimeFormatter),
    venue = venue,
    repeatsYearly = repeatsYearly,
    pinned = pinned,
    createdAtEpochMillis = createdAt.toEpochMilli(),
)

/** 同じ id が重なっていたら先のものだけ残す。 */
internal fun List<StoredOshi>.toOshis(): List<Oshi> = mapNotNull { it.toModelOrNull() }.distinctBy { it.id }

/** 同じ id が重なっていたら先のものだけ残す。 */
internal fun List<StoredEvent>.toEvents(): List<OshiEvent> = mapNotNull { it.toModelOrNull() }.distinctBy { it.id }
