package com.basekeita.oshikatsucountdown.data.definition.repository

import com.basekeita.oshikatsucountdown.data.definition.model.ReminderSettings
import com.basekeita.oshikatsucountdown.data.definition.model.ReminderTiming
import com.basekeita.oshikatsucountdown.data.definition.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.time.LocalTime

interface UserSettingsRepository {
    /** 未設定の値は `UserSettings` の既定値。 */
    val settings: Flow<UserSettings>

    /**
     * 通知の設定を 1 回読む。読めなければ [IOException]（[settings] は読めなければ既定値になり、OFF にした通知が ON として読める）。
     * 通知の予約で使う。
     */
    @Throws(IOException::class)
    suspend fun readReminderSettings(): ReminderSettings = settings.first().reminders

    suspend fun setShowPastEvents(show: Boolean)

    suspend fun setOnboardingCompleted(completed: Boolean)

    suspend fun setRemindersEnabled(enabled: Boolean)

    suspend fun setReminderTimingEnabled(
        timing: ReminderTiming,
        enabled: Boolean,
    )

    suspend fun setReminderTimeOfDay(time: LocalTime)

    suspend fun setNotificationPermissionRequested(requested: Boolean)
}
