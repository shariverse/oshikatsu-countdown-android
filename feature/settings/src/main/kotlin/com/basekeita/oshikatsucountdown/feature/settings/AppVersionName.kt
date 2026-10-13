package com.basekeita.oshikatsucountdown.feature.settings

import javax.inject.Qualifier

/** アプリのバージョン名（`versionName`）。値は app が BuildConfig から渡す。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppVersionName
