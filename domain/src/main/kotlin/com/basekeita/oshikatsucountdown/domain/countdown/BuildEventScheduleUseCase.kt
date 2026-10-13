package com.basekeita.oshikatsucountdown.domain.countdown

import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** 予定を今日から見た並びにする。同じ日の予定は時刻の決まっていないもの（終日）、早い時刻の順。 */
class BuildEventScheduleUseCase
@Inject
constructor() {
    operator fun invoke(
        events: List<OshiEvent>,
        today: LocalDate,
    ): EventSchedule {
        val scheduled = events.map { event -> schedule(event, today) }
        val upcoming = scheduled
            .filter { it.status != CountdownStatus.ENDED }
            .sortedWith(
                compareBy<ScheduledEvent> { it.occurrenceDate }
                    .thenBy { it.event.startTime ?: LocalTime.MIN }
                    .thenBy { it.event.createdAt },
            )
        val past = scheduled
            .filter { it.status == CountdownStatus.ENDED }
            .sortedWith(
                compareByDescending<ScheduledEvent> { it.occurrenceDate }
                    .thenByDescending { it.event.startTime ?: LocalTime.MIN },
            )
        val featured = upcoming.firstOrNull { it.event.pinned } ?: upcoming.firstOrNull()
        return EventSchedule(featured = featured, upcoming = upcoming, past = past)
    }

    private fun schedule(
        event: OshiEvent,
        today: LocalDate,
    ): ScheduledEvent {
        val occurrence = event.occurrenceOn(today)
        val daysUntil = ChronoUnit.DAYS.between(today, occurrence)
        val status = if (daysUntil > 0) {
            CountdownStatus.UPCOMING
        } else if (daysUntil == 0L) {
            CountdownStatus.TODAY
        } else {
            CountdownStatus.ENDED
        }
        return ScheduledEvent(event = event, occurrenceDate = occurrence, daysUntil = daysUntil, status = status)
    }
}
