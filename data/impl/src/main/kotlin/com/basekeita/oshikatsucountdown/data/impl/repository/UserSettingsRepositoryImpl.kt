package com.basekeita.oshikatsucountdown.data.impl.repository

import com.basekeita.oshikatsucountdown.data.definition.model.ReminderSettings
import com.basekeita.oshikatsucountdown.data.definition.model.ReminderTiming
import com.basekeita.oshikatsucountdown.data.definition.model.UserSettings
import com.basekeita.oshikatsucountdown.data.definition.repository.UserSettingsRepository
import com.basekeita.oshikatsucountdown.data.impl.mapping.reminderSettingsOf
import com.basekeita.oshikatsucountdown.data.impl.mapping.storageKey
import com.basekeita.oshikatsucountdown.data.impl.mapping.toMinutesOfDay
import com.basekeita.oshikatsucountdown.local.UserSettingsLocalDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalTime
import javax.inject.Inject

internal class UserSettingsRepositoryImpl @Inject constructor(
    private val localDataSource: UserSettingsLocalDataSource,
) : UserSettingsRepository {
    private val reminders =
        combine(
            localDataSource.remindersEnabled,
            localDataSource.reminderTimings,
            localDataSource.reminderMinutesOfDay,
        ) { enabled, timings, minutes -> reminderSettingsOf(enabled, timings, minutes) }

    override val settings: Flow<UserSettings> =
        combine(
            localDataSource.showPastEvents,
            localDataSource.onboardingCompleted,
            reminders,
            localDataSource.notificationPermissionRequested,
        ) { showPastEvents, onboardingCompleted, reminderSettings, permissionRequested ->
            val defaults = UserSettings()
            UserSettings(
                showPastEvents = showPastEvents ?: defaults.showPastEvents,
                onboardingCompleted = onboardingCompleted ?: defaults.onboardingCompleted,
                reminders = reminderSettings,
                notificationPermissionRequested = permissionRequested ?: defaults.notificationPermissionRequested,
            )
        }

    override suspend fun readReminderSettings(): ReminderSettings {
        val stored = localDataSource.readReminders()
        return reminderSettingsOf(stored.enabled, stored.timings, stored.minutesOfDay)
    }

    override suspend fun setShowPastEvents(show: Boolean) {
        localDataSource.setShowPastEvents(show)
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        localDataSource.setOnboardingCompleted(completed)
    }

    override suspend fun setRemindersEnabled(enabled: Boolean) {
        localDataSource.setRemindersEnabled(enabled)
    }

    override suspend fun setReminderTimingEnabled(
        timing: ReminderTiming,
        enabled: Boolean,
    ) {
        localDataSource.setReminderTimingEnabled(timing.storageKey, enabled)
    }

    override suspend fun setReminderTimeOfDay(time: LocalTime) {
        localDataSource.setReminderMinutesOfDay(time.toMinutesOfDay())
    }

    override suspend fun setNotificationPermissionRequested(requested: Boolean) {
        localDataSource.setNotificationPermissionRequested(requested)
    }
}
