package com.basekeita.oshikatsucountdown.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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

    val remindersEnabled: Flow<Boolean?> = preferences.map { it[REMINDERS_ENABLED] }.distinctUntilChanged()

    suspend fun setRemindersEnabled(enabled: Boolean) {
        dataStore.edit { it[REMINDERS_ENABLED] = enabled }
    }

    /** タイミングごとの切り替え。キーはタイミングの保存の名前（`storageKey`。data:impl が決める）。 */
    val reminderTimings: Flow<Map<String, Boolean>> = preferences.map { reminderTimingsOf(it) }.distinctUntilChanged()

    /**
     * 通知の設定を 1 回読む。読めなければ [IOException]（読めないことと値が無いことを分けたいとき。
     * 流れのほうは読めなければ値が無い扱いにするので、既定の ON として読める）。
     */
    @Throws(IOException::class)
    suspend fun readReminders(): StoredReminders {
        val stored = dataStore.data.first()
        return StoredReminders(
            enabled = stored[REMINDERS_ENABLED],
            timings = reminderTimingsOf(stored),
            minutesOfDay = stored[REMINDER_MINUTES_OF_DAY],
        )
    }

    private fun reminderTimingsOf(stored: Preferences): Map<String, Boolean> = stored.asMap()
        .filterKeys { it.name.startsWith(REMINDER_TIMING_PREFIX) }
        .mapNotNull { (key, value) -> (value as? Boolean)?.let { key.name.removePrefix(REMINDER_TIMING_PREFIX) to it } }
        .toMap()

    suspend fun setReminderTimingEnabled(
        storageKey: String,
        enabled: Boolean,
    ) {
        dataStore.edit { it[booleanPreferencesKey(REMINDER_TIMING_PREFIX + storageKey)] = enabled }
    }

    /** 日付で決まる通知の時刻。0:00 からの分。 */
    val reminderMinutesOfDay: Flow<Int?> = preferences.map { it[REMINDER_MINUTES_OF_DAY] }.distinctUntilChanged()

    suspend fun setReminderMinutesOfDay(minutes: Int) {
        dataStore.edit { it[REMINDER_MINUTES_OF_DAY] = minutes }
    }

    val notificationPermissionRequested: Flow<Boolean?> =
        preferences.map { it[NOTIFICATION_PERMISSION_REQUESTED] }.distinctUntilChanged()

    suspend fun setNotificationPermissionRequested(requested: Boolean) {
        dataStore.edit { it[NOTIFICATION_PERMISSION_REQUESTED] = requested }
    }

    /** 保存してある通知の設定。値が無ければ `null`（既定値は data:impl が決める）。 */
    data class StoredReminders(
        val enabled: Boolean?,
        val timings: Map<String, Boolean>,
        val minutesOfDay: Int?,
    )

    private companion object {
        val SHOW_PAST_EVENTS = booleanPreferencesKey("showPastEvents")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboardingCompleted")
        val REMINDERS_ENABLED = booleanPreferencesKey("remindersEnabled")
        val REMINDER_MINUTES_OF_DAY = intPreferencesKey("reminderMinutesOfDay")
        val NOTIFICATION_PERMISSION_REQUESTED = booleanPreferencesKey("notificationPermissionRequested")
        const val REMINDER_TIMING_PREFIX = "reminderTiming."
    }
}
