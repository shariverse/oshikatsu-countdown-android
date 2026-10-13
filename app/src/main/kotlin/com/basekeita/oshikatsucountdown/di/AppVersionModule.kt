package com.basekeita.oshikatsucountdown.di

import com.basekeita.oshikatsucountdown.BuildConfig
import com.basekeita.oshikatsucountdown.feature.settings.AppVersionName
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** 設定に出すアプリのバージョン。feature は BuildConfig を持たないので app から渡す。 */
@Module
@InstallIn(SingletonComponent::class)
object AppVersionModule {
    @Provides
    @AppVersionName
    fun providesAppVersionName(): String = BuildConfig.VERSION_NAME
}
