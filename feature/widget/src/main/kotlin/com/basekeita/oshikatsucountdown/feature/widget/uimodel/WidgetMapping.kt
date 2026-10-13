package com.basekeita.oshikatsucountdown.feature.widget.uimodel

import com.basekeita.oshikatsucountdown.data.definition.model.EventType
import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.model.OshiColor
import com.basekeita.oshikatsucountdown.data.definition.model.firstCharacter
import com.basekeita.oshikatsucountdown.domain.countdown.CountdownStatus
import com.basekeita.oshikatsucountdown.domain.countdown.EventSchedule
import com.basekeita.oshikatsucountdown.domain.countdown.ScheduledEvent
import com.basekeita.oshikatsucountdown.ui.uimodel.EventTypeUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel

/** 当日とこれからの予定を近い順に。 */
internal fun EventSchedule.upcomingWidgetEvents(oshis: List<Oshi>): List<WidgetEventUiModel> {
    val oshiById = oshis.associateBy { it.id }
    return upcoming.map { scheduled -> scheduled.toWidgetUiModel(oshiById[scheduled.event.oshiId]) }
}

/** `eventId` の予定。終わった予定も探す。無い（削除された、選んでいない）ときは null。 */
internal fun EventSchedule.findWidgetEvent(
    eventId: String?,
    oshis: List<Oshi>,
): WidgetEventUiModel? {
    if (eventId == null) return null

    val scheduled = (upcoming + past).firstOrNull { it.event.id.toString() == eventId } ?: return null
    return scheduled.toWidgetUiModel(oshis.firstOrNull { it.id == scheduled.event.oshiId })
}

private fun ScheduledEvent.toWidgetUiModel(oshi: Oshi?): WidgetEventUiModel = WidgetEventUiModel(
    id = event.id.toString(),
    title = event.title,
    type = event.type.toUiModel(),
    oshiName = oshi?.name,
    initial = (oshi?.name ?: event.title).firstCharacter(),
    color = oshi?.color?.toUiModel() ?: NO_OSHI_COLOR,
    photoPath = oshi?.photoPath,
    occurrenceDate = occurrenceDate,
    daysUntil = daysUntil,
    status = when (status) {
        CountdownStatus.UPCOMING -> WidgetEventStatusUiModel.UPCOMING
        CountdownStatus.TODAY -> WidgetEventStatusUiModel.TODAY
        CountdownStatus.ENDED -> WidgetEventStatusUiModel.ENDED
    },
)

/** 推しなしの予定の色。ホームと同じく、どの推し色とも紛れない無彩色。 */
private val NO_OSHI_COLOR = OshiColorUiModel.BLACK

private fun OshiColor.toUiModel(): OshiColorUiModel = when (this) {
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

private fun EventType.toUiModel(): EventTypeUiModel = when (this) {
    EventType.LIVE -> EventTypeUiModel.LIVE
    EventType.BIRTHDAY -> EventTypeUiModel.BIRTHDAY
    EventType.RELEASE -> EventTypeUiModel.RELEASE
    EventType.STREAM -> EventTypeUiModel.STREAM
    EventType.OTHER -> EventTypeUiModel.OTHER
}
