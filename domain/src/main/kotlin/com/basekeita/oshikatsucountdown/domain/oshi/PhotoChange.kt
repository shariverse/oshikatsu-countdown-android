package com.basekeita.oshikatsucountdown.domain.oshi

/** 推しを保存するときの写真の扱い。 */
sealed interface PhotoChange {
    /** 今の写真のまま。 */
    data object Keep : PhotoChange

    /** 写真を外す。 */
    data object Remove : PhotoChange

    /** 選んだ写真（Photo Picker の content URI）に差し替える。 */
    data class Replace(
        val sourceUri: String,
    ) : PhotoChange
}
