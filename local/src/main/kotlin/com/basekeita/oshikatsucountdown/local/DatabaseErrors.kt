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
 * データベースから 1 回読む。失敗は [IOException] にする（空を流す [emptyOnDatabaseError] と違い、読めないことと空を分けたいとき）。
 */
internal suspend fun <T> readDatabase(read: suspend () -> T): T = try {
    read()
} catch (exception: SQLException) {
    throw IOException(exception)
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
