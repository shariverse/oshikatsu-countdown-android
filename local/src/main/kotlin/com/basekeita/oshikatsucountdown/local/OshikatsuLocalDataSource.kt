package com.basekeita.oshikatsucountdown.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject

/**
 * DataStore（Preferences、`oshikatsu_prefs`）の読み書き。推しと予定は JSON の配列で 1 つのキーに入れる。
 * 値が無い・読めないときは `null` を流し、既定値は呼ぶ側（data:impl）で決める。
 */
class OshikatsuLocalDataSource
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val preferences: Flow<Preferences> =
        dataStore.data.catch { exception ->
            if (exception !is IOException) throw exception
            emit(emptyPreferences())
        }

    /** 保存済みの推し。保存データが無い、または JSON として読めないときは `null`。 */
    val oshis: Flow<List<StoredOshi>?> = decodedList(OSHIS)

    /** 推しを読んで書き換える。読み書きを 1 回の `edit` で行い、途中で別の書き込みが割り込まないようにする。 */
    suspend fun updateOshis(transform: (List<StoredOshi>) -> List<StoredOshi>) {
        updateList(OSHIS, transform)
    }

    /** 保存済みの予定。保存データが無い、または JSON として読めないときは `null`。 */
    val events: Flow<List<StoredEvent>?> = decodedList(EVENTS)

    /** 予定を読んで書き換える。読み書きを 1 回の `edit` で行い、途中で別の書き込みが割り込まないようにする。 */
    suspend fun updateEvents(transform: (List<StoredEvent>) -> List<StoredEvent>) {
        updateList(EVENTS, transform)
    }

    val showPastEvents: Flow<Boolean?> = preferences.map { it[SHOW_PAST_EVENTS] }.distinctUntilChanged()

    suspend fun setShowPastEvents(show: Boolean) {
        dataStore.edit { it[SHOW_PAST_EVENTS] = show }
    }

    val onboardingCompleted: Flow<Boolean?> = preferences.map { it[ONBOARDING_COMPLETED] }.distinctUntilChanged()

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[ONBOARDING_COMPLETED] = completed }
    }

    private inline fun <reified T> decodedList(key: Preferences.Key<String>): Flow<List<T>?> = preferences
        .map { it[key] }
        .distinctUntilChanged()
        .map { encoded -> encoded?.let { decodeOrNull<List<T>>(it) } }

    /** 読めない保存データは空として書き換える（壊れたデータを残しても読めるようにはならないため）。 */
    private suspend inline fun <reified T> updateList(
        key: Preferences.Key<String>,
        crossinline transform: (List<T>) -> List<T>,
    ) {
        dataStore.edit { mutablePreferences ->
            val current = mutablePreferences[key]?.let { decodeOrNull<List<T>>(it) }.orEmpty()
            mutablePreferences[key] = json.encodeToString(transform(current))
        }
    }

    /** 形の違う JSON、UUID として読めない id を含む JSON は `null`。 */
    private inline fun <reified T> decodeOrNull(encoded: String): T? = try {
        json.decodeFromString<T>(encoded)
    } catch (exception: IllegalArgumentException) {
        null
    }

    private companion object {
        val OSHIS = stringPreferencesKey("oshis")
        val EVENTS = stringPreferencesKey("events")
        val SHOW_PAST_EVENTS = booleanPreferencesKey("showPastEvents")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboardingCompleted")

        /** 後の版で足したキーを前の版でも読めるよう、知らないキーは読み飛ばす。既定値と同じ値も省かずに書く。 */
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
