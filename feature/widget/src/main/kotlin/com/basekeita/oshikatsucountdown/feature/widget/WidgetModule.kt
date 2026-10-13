package com.basekeita.oshikatsucountdown.feature.widget

import com.basekeita.oshikatsucountdown.data.definition.repository.EventRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.OshiRepository
import com.basekeita.oshikatsucountdown.domain.countdown.BuildEventScheduleUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
abstract class WidgetModule {
    @Binds
    internal abstract fun bindsWidgetEventSelectionStore(store: GlanceWidgetEventSelectionStore): WidgetEventSelectionStore
}

/** Glance のウィジェットは Hilt が作らないので、使うものはここから受け取る。 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun eventRepository(): EventRepository

    fun oshiRepository(): OshiRepository

    fun buildEventSchedule(): BuildEventScheduleUseCase

    fun clock(): Clock
}
