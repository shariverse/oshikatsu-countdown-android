package com.basekeita.oshikatsucountdown.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

/**
 * カウントダウンの「今」。ViewModel を作るたびに端末のタイムゾーンで作り直し、旅先でタイムゾーンを変えても次に開いた画面から追従する
 * （テストでは固定の時計に差し替える）。
 */
@Module
@InstallIn(SingletonComponent::class)
object ClockModule {
    @Provides
    fun providesClock(): Clock = Clock.systemDefaultZone()
}
