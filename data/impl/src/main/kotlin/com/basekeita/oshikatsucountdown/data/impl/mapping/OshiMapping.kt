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

/**
 * 対応表に無いキーは既定の色にする。どのキーがあり得るかは読む版の対応表で決まり、
 * キーの変更で移行を書き忘れたときや、色を足した版から前の版に戻したときにも入りうるため。
 */
internal fun OshiEntity.toModel(): Oshi = Oshi(
    id = id,
    name = name,
    color = OshiColor.entries.firstOrNull { it.storageKey == color } ?: OshiColor.DEFAULT,
    photoPath = photoPath,
    createdAt = createdAt,
)

internal fun Oshi.toEntity(): OshiEntity = OshiEntity(
    id = id,
    name = name,
    color = color.storageKey,
    photoPath = photoPath,
    createdAt = createdAt,
)
