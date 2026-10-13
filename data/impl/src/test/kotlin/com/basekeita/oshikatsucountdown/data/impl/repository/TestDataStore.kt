package com.basekeita.oshikatsucountdown.data.impl.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import kotlinx.coroutines.CoroutineScope
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.io.File

/**
 * 一時ファイルの DataStore。ファイルを渡す create は JVM のテストでは `File.renameTo` で書き換え、
 * Windows では 2 回目の書き込みが失敗するため、置き換えて書く OkioStorage を使う。
 */
internal fun testDataStore(
    directory: File,
    scope: CoroutineScope,
): DataStore<Preferences> = PreferenceDataStoreFactory.create(
    storage = OkioStorage(FileSystem.SYSTEM, PreferencesSerializer) {
        directory.resolve("test.preferences_pb").toOkioPath()
    },
    corruptionHandler = null,
    migrations = emptyList(),
    scope = scope,
)
