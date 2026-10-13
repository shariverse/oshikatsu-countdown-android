package com.basekeita.oshikatsucountdown.data.impl.repository

import com.basekeita.oshikatsucountdown.data.definition.model.UserSettings
import com.basekeita.oshikatsucountdown.data.definition.repository.UserSettingsRepository
import com.basekeita.oshikatsucountdown.local.OshikatsuLocalDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

internal class UserSettingsRepositoryImpl
@Inject
constructor(
    private val localDataSource: OshikatsuLocalDataSource,
) : UserSettingsRepository {
    override val settings: Flow<UserSettings> =
        combine(
            localDataSource.showPastEvents,
            localDataSource.onboardingCompleted,
        ) { showPastEvents, onboardingCompleted ->
            val defaults = UserSettings()
            UserSettings(
                showPastEvents = showPastEvents ?: defaults.showPastEvents,
                onboardingCompleted = onboardingCompleted ?: defaults.onboardingCompleted,
            )
        }

    override suspend fun setShowPastEvents(show: Boolean) {
        localDataSource.setShowPastEvents(show)
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        localDataSource.setOnboardingCompleted(completed)
    }
}
