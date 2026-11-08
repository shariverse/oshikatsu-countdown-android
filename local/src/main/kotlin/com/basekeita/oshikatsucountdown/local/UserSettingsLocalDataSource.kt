package com.basekeita.oshikatsucountdown.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

/**
 * 設定の読み書き（DataStore（Preferences）、`oshikatsu_prefs`）。
 * 値が無い・読めないときは `null` を流し、既定値は呼ぶ側（data:impl）で決める。
 */
class UserSettingsLocalDataSource
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val preferences: Flow<Preferences> =
        dataStore.data.catch { exception ->
            if (exception !is IOException) throw exception
            emit(emptyPreferences())
        }

    val showPastEvents: Flow<Boolean?> = preferences.map { it[SHOW_PAST_EVENTS] }.distinctUntilChanged()

    suspend fun setShowPastEvents(show: Boolean) {
        dataStore.edit { it[SHOW_PAST_EVENTS] = show }
    }

    val onboardingCompleted: Flow<Boolean?> = preferences.map { it[ONBOARDING_COMPLETED] }.distinctUntilChanged()

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[ONBOARDING_COMPLETED] = completed }
    }

    private companion object {
        val SHOW_PAST_EVENTS = booleanPreferencesKey("showPastEvents")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboardingCompleted")
    }
}
