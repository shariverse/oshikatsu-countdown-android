package com.basekeita.oshikatsucountdown.data.definition.repository

import com.basekeita.oshikatsucountdown.data.definition.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface UserSettingsRepository {
    /** 未設定の値は `UserSettings` の既定値。 */
    val settings: Flow<UserSettings>

    suspend fun setShowPastEvents(show: Boolean)

    suspend fun setOnboardingCompleted(completed: Boolean)
}
