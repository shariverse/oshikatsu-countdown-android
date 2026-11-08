package com.basekeita.oshikatsucountdown.data.impl.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.basekeita.oshikatsucountdown.local.EventLocalDataSource
import com.basekeita.oshikatsucountdown.local.OshiLocalDataSource
import com.basekeita.oshikatsucountdown.local.database.OshikatsuDatabase

/** メモリの上の Room と、それを読み書きする推し・予定の DataSource。Robolectric の上で作る。 */
internal class TestStorages {
    val database: OshikatsuDatabase = Room
        .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), OshikatsuDatabase::class.java)
        .build()
    val oshiDataSource = OshiLocalDataSource(database.oshiDao())
    val eventDataSource = EventLocalDataSource(database.eventDao())
}
