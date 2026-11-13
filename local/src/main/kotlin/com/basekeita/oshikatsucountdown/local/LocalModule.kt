package com.basekeita.oshikatsucountdown.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.basekeita.oshikatsucountdown.local.database.EventDao
import com.basekeita.oshikatsucountdown.local.database.OshiDao
import com.basekeita.oshikatsucountdown.local.database.OshikatsuDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalModule {
    private const val PREFERENCES_NAME = "oshikatsu_prefs"

    /** 1 つのファイルに DataStore を 2 つ作ると壊れるため、アプリ全体で 1 つにする。ファイルが壊れていたら空から始める。 */
    @Provides
    @Singleton
    fun providesPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        produceFile = { context.preferencesDataStoreFile(PREFERENCES_NAME) },
    )

    /** 同じファイルを開く RoomDatabase を複数作ると変更の知らせが届かないため、アプリ全体で 1 つにする。 */
    @Provides
    @Singleton
    fun providesOshikatsuDatabase(
        @ApplicationContext context: Context,
    ): OshikatsuDatabase = Room.databaseBuilder(context, OshikatsuDatabase::class.java, OshikatsuDatabase.DATABASE_NAME).build()

    @Provides
    fun providesOshiDao(database: OshikatsuDatabase): OshiDao = database.oshiDao()

    @Provides
    fun providesEventDao(database: OshikatsuDatabase): EventDao = database.eventDao()
}
