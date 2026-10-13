package com.basekeita.oshikatsucountdown.domain.countdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class CalculateCountdownUseCaseTest {
    private val calculate = CalculateCountdownUseCase()
    private val zone = ZoneId.of("Asia/Tokyo")

    private fun at(dateTime: String) = LocalDateTime.parse(dateTime).atZone(zone).toInstant()

    @Test
    fun `開始の時刻までの残りを秒まで数える`() {
        val live = event(LocalDate.of(2026, 10, 18), startTime = LocalTime.of(17, 0))

        val countdown = calculate(live, at("2026-10-07T10:17:42"), zone)

        assertEquals(CountdownStatus.UPCOMING, countdown.status)
        assertEquals(Duration.ofDays(11).plusHours(6).plusMinutes(42).plusSeconds(18), countdown.remaining)
        assertEquals(11L, countdown.daysUntil)
    }

    @Test
    fun `時刻が無ければ開催日の0時までを数える`() {
        val release = event(LocalDate.of(2026, 10, 10))

        val countdown = calculate(release, at("2026-10-09T23:00:00"), zone)

        assertEquals(Duration.ofHours(1), countdown.remaining)
        assertEquals(1L, countdown.daysUntil)
    }

    @Test
    fun `当日でも開始の前は残りを数え始まったら当日にする`() {
        val live = event(LocalDate.of(2026, 10, 18), startTime = LocalTime.of(17, 0))

        val before = calculate(live, at("2026-10-18T14:45:55"), zone)
        assertEquals(CountdownStatus.UPCOMING, before.status)
        assertEquals(Duration.ofHours(2).plusMinutes(14).plusSeconds(5), before.remaining)
        assertEquals(0L, before.daysUntil)

        val started = calculate(live, at("2026-10-18T17:00:00"), zone)
        assertEquals(CountdownStatus.TODAY, started.status)
        assertEquals(Duration.ZERO, started.remaining)
        assertEquals(1.0, started.progress!!, 0.0)
    }

    @Test
    fun `開催日を過ぎたくり返さない予定は終わった扱い`() {
        val live = event(LocalDate.of(2026, 10, 18), startTime = LocalTime.of(17, 0))

        val countdown = calculate(live, at("2026-10-20T09:00:00"), zone)

        assertEquals(CountdownStatus.ENDED, countdown.status)
        assertEquals(Duration.ZERO, countdown.remaining)
        assertEquals(-2L, countdown.daysUntil)
    }

    @Test
    fun `経過は登録した時刻から開始までの割合`() {
        val live = event(
            LocalDate.of(2026, 10, 11),
            startTime = LocalTime.of(0, 0),
            createdAt = at("2026-10-01T00:00:00"),
        )

        val countdown = calculate(live, at("2026-10-08T12:00:00"), zone)

        assertEquals(0.75, countdown.progress!!, 1e-9)
        assertEquals(LocalDate.of(2026, 10, 1), countdown.progressStartDate)
    }

    @Test
    fun `毎年くり返す予定の経過は前の回の開始から数える`() {
        val birthday = event(
            LocalDate.of(2000, 11, 10),
            repeatsYearly = true,
            createdAt = at("2020-01-01T00:00:00"),
        )

        val countdown = calculate(birthday, at("2026-05-12T00:00:00"), zone)

        assertEquals(LocalDate.of(2026, 11, 10), countdown.occurrenceDate)
        assertEquals(LocalDate.of(2025, 11, 10), countdown.progressStartDate)
        assertEquals(183.0 / 365.0, countdown.progress!!, 1e-9)
    }

    @Test
    fun `登録した時刻が開始より後なら経過は求めない`() {
        val live = event(LocalDate.of(2026, 10, 11), createdAt = at("2026-10-12T00:00:00"))

        val countdown = calculate(live, at("2026-10-10T00:00:00"), zone)

        assertNull(countdown.progress)
    }
}
