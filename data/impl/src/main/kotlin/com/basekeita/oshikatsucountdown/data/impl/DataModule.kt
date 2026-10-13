package com.basekeita.oshikatsucountdown.data.impl

import com.basekeita.oshikatsucountdown.data.definition.repository.EventRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.OshiRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.PhotoRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.UserSettingsRepository
import com.basekeita.oshikatsucountdown.data.impl.repository.EventRepositoryImpl
import com.basekeita.oshikatsucountdown.data.impl.repository.OshiRepositoryImpl
import com.basekeita.oshikatsucountdown.data.impl.repository.PhotoRepositoryImpl
import com.basekeita.oshikatsucountdown.data.impl.repository.UserSettingsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    internal abstract fun bindsOshiRepository(oshiRepository: OshiRepositoryImpl): OshiRepository

    @Binds
    internal abstract fun bindsEventRepository(eventRepository: EventRepositoryImpl): EventRepository

    @Binds
    internal abstract fun bindsPhotoRepository(photoRepository: PhotoRepositoryImpl): PhotoRepository

    @Binds
    internal abstract fun bindsUserSettingsRepository(userSettingsRepository: UserSettingsRepositoryImpl): UserSettingsRepository

    companion object {
        @Provides
        @DefaultDispatcher
        internal fun providesDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
    }
}
