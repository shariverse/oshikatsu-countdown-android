package com.basekeita.oshikatsucountdown.data.definition.repository

/** 推しの写真。選んだ写真はアプリの内部ストレージに縮小して取り込み、端末の外には出さない。 */
interface PhotoRepository {
    /** `sourceUri`（Photo Picker が返した content URI）の写真を取り込み、取り込んだファイルの絶対パスを返す。読めなければ null。 */
    suspend fun import(sourceUri: String): String?

    /** 取り込んだ写真を削除する。取り込み先の外のパスは消さない。 */
    suspend fun delete(path: String)
}
