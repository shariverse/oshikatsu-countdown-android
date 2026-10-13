package com.basekeita.oshikatsucountdown.domain.countdown

import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** 予定の開始までの残りと経過の割合を、`now` の時点で求める。日付と時刻は `zone` での値として読む。 */
class CalculateCountdownUseCase
@Inject
constructor() {
    operator fun invoke(
        event: OshiEvent,
        now: Instant,
        zone: ZoneId,
    ): EventCountdown {
        val today = now.atZone(zone).toLocalDate()
        val occurrence = event.occurrenceOn(today)
        val startTime = event.startTime ?: LocalTime.MIDNIGHT
        val target = occurrence.atTime(startTime).atZone(zone).toInstant()
        val daysUntil = ChronoUnit.DAYS.between(today, occurrence)

        val status = if (daysUntil < 0) {
            CountdownStatus.ENDED
        } else if (now >= target) {
            CountdownStatus.TODAY
        } else {
            CountdownStatus.UPCOMING
        }

        val previousStart = event.previousOccurrence(occurrence)?.atTime(startTime)?.atZone(zone)?.toInstant()
        val progressStart = if (previousStart != null && previousStart > event.createdAt) {
            previousStart
        } else {
            event.createdAt
        }

        val progress = if (status != CountdownStatus.UPCOMING) {
            1.0
        } else {
            val total = Duration.between(progressStart, target)
            if (total.isNegative || total.isZero) {
                null
            } else {
                val elapsed = Duration.between(progressStart, now).toMillis().toDouble()
                (elapsed / total.toMillis()).coerceIn(0.0, 1.0)
            }
        }

        val remaining = if (status == CountdownStatus.UPCOMING) {
            Duration.between(now, target)
        } else {
            Duration.ZERO
        }

        return EventCountdown(
            occurrenceDate = occurrence,
            status = status,
            remaining = remaining,
            daysUntil = daysUntil,
            progress = progress,
            progressStartDate = progressStart.atZone(zone).toLocalDate(),
        )
    }
}
