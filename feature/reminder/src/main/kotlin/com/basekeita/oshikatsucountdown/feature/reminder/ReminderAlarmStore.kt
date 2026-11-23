package com.basekeita.oshikatsucountdown.feature.reminder

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 中身は予約の都合の値で、作り直せる。壊れたら空にして読む（読めないままだと、予約し直すたびに失敗し、予約が二度と入らなくなる）。
 * 空にした後に残る、記録から外れたアラームは、出るときに Receiver が計画に無いことを確かめて出さない。
 */
private val Context.reminderAlarmsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "reminder_alarms",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/**
 * 予約したアラームと、出している当日のカウントダウンの識別子。AlarmManager は予約したアラームを読み出せないので、
 * 計画から外れたものを取り消すために持つ。予約の都合の値で設定ではないため、設定（`oshikatsu_prefs`）とは別のファイルに置く。
 */
internal class ReminderAlarmStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    /** 何の識別子か。 */
    enum class Kind(
        val key: Preferences.Key<Set<String>>,
    ) {
        /** 予定の通知のアラーム。 */
        REMINDER_ALARMS(stringSetPreferencesKey("scheduledIdentifiers")),

        /** 当日のカウントダウンを始める・変える・消すアラーム。 */
        COUNTDOWN_ALARMS(stringSetPreferencesKey("countdownAlarmIdentifiers")),

        /** 出している当日のカウントダウンの通知。 */
        COUNTDOWN_NOTIFICATIONS(stringSetPreferencesKey("countdownNotificationIdentifiers")),
    }

    suspend fun identifiers(kind: Kind): Set<String> = context.reminderAlarmsDataStore.data.first()[kind.key].orEmpty()

    suspend fun setIdentifiers(
        kind: Kind,
        identifiers: Set<String>,
    ) {
        context.reminderAlarmsDataStore.edit { it[kind.key] = identifiers }
    }
}
