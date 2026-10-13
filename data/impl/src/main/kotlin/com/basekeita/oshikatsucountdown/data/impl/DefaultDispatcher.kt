package com.basekeita.oshikatsucountdown.data.impl

import javax.inject.Qualifier

/** 保存データの JSON のデコードと整形を走らせる Dispatcher。テストでは仮想時間の Dispatcher に差し替える。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class DefaultDispatcher
