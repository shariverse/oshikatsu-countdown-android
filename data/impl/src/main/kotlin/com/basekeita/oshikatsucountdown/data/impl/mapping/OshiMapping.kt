package com.basekeita.oshikatsucountdown.data.impl.mapping

import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.model.OshiColor
import com.basekeita.oshikatsucountdown.local.database.model.OshiEntity

/**
 * データベースに保存するキー。保存の形はデータ層が決め、推し色の名前を変えても保存済みの値が変わらないようにする。
 * キーを変えるときは、データベースの版を上げて保存済みの値を移す。
 */
internal val OshiColor.storageKey: String
    get() = when (this) {
        OshiColor.PINK -> "pink"
        OshiColor.RED -> "red"
        OshiColor.ORANGE -> "orange"
        OshiColor.YELLOW -> "yellow"
        OshiColor.GREEN -> "green"
        OshiColor.MINT -> "mint"
        OshiColor.BLUE -> "blue"
        OshiColor.NAVY -> "navy"
        OshiColor.PURPLE -> "purple"
        OshiColor.LAVENDER -> "lavender"
        OshiColor.BROWN -> "brown"
        OshiColor.BLACK -> "black"
    }

/** 知らない色は既定の色に戻す。 */
private fun oshiColorOf(storageKey: String): OshiColor = OshiColor.entries.firstOrNull { it.storageKey == storageKey } ?: OshiColor.DEFAULT

/** 名前が空になる推しは読み飛ばす。 */
internal fun OshiEntity.toModelOrNull(): Oshi? {
    val normalizedName = Oshi.normalizeName(name)
    if (normalizedName.isEmpty()) return null

    return Oshi(id = id, name = normalizedName, color = oshiColorOf(color), photoPath = photoPath)
}

internal fun Oshi.toEntity(sortOrder: Long): OshiEntity = OshiEntity(
    id = id,
    name = name,
    color = color.storageKey,
    photoPath = photoPath,
    sortOrder = sortOrder,
)
