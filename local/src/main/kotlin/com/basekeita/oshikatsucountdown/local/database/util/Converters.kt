package com.basekeita.oshikatsucountdown.local.database.util

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/** id は UUID の文字列で持つ。 */
internal class UuidConverter {
    @TypeConverter
    fun stringToUuid(value: String?): UUID? = value?.let(UUID::fromString)

    @TypeConverter
    fun uuidToString(uuid: UUID?): String? = uuid?.toString()
}

/** 日付は ISO 8601 の `yyyy-MM-dd` で持つ。 */
internal class LocalDateConverter {
    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun localDateToString(date: LocalDate?): String? = date?.toString()
}

/** 時刻は ISO 8601 の `HH:mm`（秒があれば `HH:mm:ss`）で持つ。 */
internal class LocalTimeConverter {
    @TypeConverter
    fun stringToLocalTime(value: String?): LocalTime? = value?.let(LocalTime::parse)

    @TypeConverter
    fun localTimeToString(time: LocalTime?): String? = time?.toString()
}

/** 時刻は UNIX 時間のミリ秒で持つ。 */
internal class InstantConverter {
    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun instantToLong(instant: Instant?): Long? = instant?.toEpochMilli()
}
