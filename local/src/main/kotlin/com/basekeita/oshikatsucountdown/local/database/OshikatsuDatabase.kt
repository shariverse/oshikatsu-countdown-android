package com.basekeita.oshikatsucountdown.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.basekeita.oshikatsucountdown.local.database.model.EventEntity
import com.basekeita.oshikatsucountdown.local.database.model.OshiEntity
import com.basekeita.oshikatsucountdown.local.database.util.InstantConverter
import com.basekeita.oshikatsucountdown.local.database.util.LocalDateConverter
import com.basekeita.oshikatsucountdown.local.database.util.LocalTimeConverter
import com.basekeita.oshikatsucountdown.local.database.util.UuidConverter

/** 推しと予定のデータベース。予定は推しの id で推しに結び付き、推しを消すと結び付いた予定も消える。 */
@Database(
    entities = [OshiEntity::class, EventEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(
    UuidConverter::class,
    LocalDateConverter::class,
    LocalTimeConverter::class,
    InstantConverter::class,
)
abstract class OshikatsuDatabase : RoomDatabase() {
    abstract fun oshiDao(): OshiDao

    abstract fun eventDao(): EventDao

    companion object {
        const val DATABASE_NAME = "oshikatsu.db"
    }
}
