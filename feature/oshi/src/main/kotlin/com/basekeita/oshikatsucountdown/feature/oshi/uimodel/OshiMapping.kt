package com.basekeita.oshikatsucountdown.feature.oshi.uimodel

import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.model.OshiColor
import com.basekeita.oshikatsucountdown.data.definition.model.firstCharacter
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiUiModel

internal fun Oshi.toUiModel(): OshiUiModel = OshiUiModel(
    id = id.toString(),
    name = name,
    initial = name.firstCharacter(),
    color = color.toUiModel(),
    photoPath = photoPath,
)

internal fun OshiColor.toUiModel(): OshiColorUiModel = when (this) {
    OshiColor.PINK -> OshiColorUiModel.PINK
    OshiColor.RED -> OshiColorUiModel.RED
    OshiColor.ORANGE -> OshiColorUiModel.ORANGE
    OshiColor.YELLOW -> OshiColorUiModel.YELLOW
    OshiColor.GREEN -> OshiColorUiModel.GREEN
    OshiColor.MINT -> OshiColorUiModel.MINT
    OshiColor.BLUE -> OshiColorUiModel.BLUE
    OshiColor.NAVY -> OshiColorUiModel.NAVY
    OshiColor.PURPLE -> OshiColorUiModel.PURPLE
    OshiColor.LAVENDER -> OshiColorUiModel.LAVENDER
    OshiColor.BROWN -> OshiColorUiModel.BROWN
    OshiColor.BLACK -> OshiColorUiModel.BLACK
}

internal fun OshiColorUiModel.toModel(): OshiColor = when (this) {
    OshiColorUiModel.PINK -> OshiColor.PINK
    OshiColorUiModel.RED -> OshiColor.RED
    OshiColorUiModel.ORANGE -> OshiColor.ORANGE
    OshiColorUiModel.YELLOW -> OshiColor.YELLOW
    OshiColorUiModel.GREEN -> OshiColor.GREEN
    OshiColorUiModel.MINT -> OshiColor.MINT
    OshiColorUiModel.BLUE -> OshiColor.BLUE
    OshiColorUiModel.NAVY -> OshiColor.NAVY
    OshiColorUiModel.PURPLE -> OshiColor.PURPLE
    OshiColorUiModel.LAVENDER -> OshiColor.LAVENDER
    OshiColorUiModel.BROWN -> OshiColor.BROWN
    OshiColorUiModel.BLACK -> OshiColor.BLACK
}
