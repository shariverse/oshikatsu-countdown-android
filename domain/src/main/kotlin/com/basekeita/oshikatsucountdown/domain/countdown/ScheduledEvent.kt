package com.basekeita.oshikatsucountdown.domain.countdown

import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import java.time.LocalDate

/**
 * 今日から見た予定。
 *
 * @property occurrenceDate 数える対象の開催日（毎年くり返す予定は次の回）。
 * @property daysUntil 開催日までの日数（暦の日数）。当日は 0、終わった予定は負。
 */
data class ScheduledEvent(
    val event: OshiEvent,
    val occurrenceDate: LocalDate,
    val daysUntil: Long,
    val status: CountdownStatus,
)

/**
 * ホームに出す予定の並び。
 *
 * @property featured 先頭に大きく出す予定。ピン留めした予定がまだ終わっていなければそれ、無ければいちばん近い予定。
 * @property upcoming 当日とこれからの予定。近い順。
 * @property past 終わった予定。新しい順。
 */
data class EventSchedule(
    val featured: ScheduledEvent?,
    val upcoming: List<ScheduledEvent>,
    val past: List<ScheduledEvent>,
)
