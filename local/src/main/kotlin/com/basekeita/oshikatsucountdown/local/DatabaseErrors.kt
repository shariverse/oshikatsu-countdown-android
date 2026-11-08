package com.basekeita.oshikatsucountdown.local

import android.database.SQLException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.IOException

/** データベースを読めなければ空を流す。 */
internal fun <T> Flow<List<T>>.emptyOnDatabaseError(): Flow<List<T>> = catch { exception ->
    if (exception !is SQLException) throw exception
    emit(emptyList())
}

/**
 * データベースに書き込む。失敗は、書き込みの失敗を受ける側（ViewModel）にそろえて [IOException] にする。
 */
internal suspend fun writeDatabase(write: suspend () -> Unit) {
    try {
        write()
    } catch (exception: SQLException) {
        throw IOException(exception)
    }
}
