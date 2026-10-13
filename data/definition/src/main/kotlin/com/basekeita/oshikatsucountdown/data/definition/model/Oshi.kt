package com.basekeita.oshikatsucountdown.data.definition.model

import java.util.UUID

/**
 * 推し。予定はこの id で推しに結び付く。
 *
 * @property photoPath アプリの内部ストレージに取り込んだ写真の絶対パス。写真が無ければ null。
 */
data class Oshi(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val color: OshiColor,
    val photoPath: String? = null,
) {
    companion object {
        /** 推しの名前の最大文字数（正規化後）。 */
        const val MAX_NAME_LENGTH = 20

        /** 空白を潰し、上限で切る。 */
        fun normalizeName(raw: String): String = raw.collapseWhitespace().takeCharacters(MAX_NAME_LENGTH)
    }
}
