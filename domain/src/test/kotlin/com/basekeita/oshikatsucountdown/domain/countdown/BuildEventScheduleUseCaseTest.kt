package com.basekeita.oshikatsucountdown.domain.countdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class BuildEventScheduleUseCaseTest {
    private val buildSchedule = BuildEventScheduleUseCase()
    private val today = LocalDate.of(2026, 10, 13)

    @Test
    fun `当日とこれからの予定を近い順に並べ終わった予定は新しい順`() {
        val live = event(LocalDate.of(2026, 10, 18), title = "LIVE")
        val release = event(LocalDate.of(2026, 10, 16), title = "発売日")
        val todayEvent = event(today, title = "今日")
        val ended = event(LocalDate.of(2026, 9, 1), title = "終わった")
        val endedRecently = event(LocalDate.of(2026, 10, 1), title = "最近終わった")

        val schedule = buildSchedule(listOf(live, ended, release, todayEvent, endedRecently), today)

        assertEquals(listOf("今日", "発売日", "LIVE"), schedule.upcoming.map { it.event.title })
        assertEquals(listOf(0L, 3L, 5L), schedule.upcoming.map { it.daysUntil })
        assertEquals(
            listOf(CountdownStatus.TODAY, CountdownStatus.UPCOMING, CountdownStatus.UPCOMING),
            schedule.upcoming.map { it.status },
        )
        assertEquals(listOf("最近終わった", "終わった"), schedule.past.map { it.event.title })
        assertEquals(listOf(-12L, -42L), schedule.past.map { it.daysUntil })
    }

    @Test
    fun `同じ日は終日の予定から時刻の早い順`() {
        val evening = event(LocalDate.of(2026, 10, 18), startTime = LocalTime.of(17, 0), title = "夜")
        val allDay = event(LocalDate.of(2026, 10, 18), title = "終日")
        val noon = event(LocalDate.of(2026, 10, 18), startTime = LocalTime.of(12, 0), title = "昼")

        val schedule = buildSchedule(listOf(evening, allDay, noon), today)

        assertEquals(listOf("終日", "昼", "夜"), schedule.upcoming.map { it.event.title })
    }

    @Test
    fun `毎年くり返す予定は次の回で数え終わった予定にならない`() {
        val birthday = event(LocalDate.of(2000, 10, 1), repeatsYearly = true)

        val schedule = buildSchedule(listOf(birthday), today)

        assertEquals(LocalDate.of(2027, 10, 1), schedule.upcoming.single().occurrenceDate)
        assertEquals(353L, schedule.upcoming.single().daysUntil)
        assertEquals(emptyList<ScheduledEvent>(), schedule.past)
    }

    @Test
    fun `先頭はピン留めした予定でなければいちばん近い予定`() {
        val near = event(LocalDate.of(2026, 10, 14), title = "近い")
        val pinned = event(LocalDate.of(2026, 12, 1), pinned = true, title = "ピン留め")

        assertEquals("ピン留め", buildSchedule(listOf(near, pinned), today).featured?.event?.title)
        assertEquals("近い", buildSchedule(listOf(near, pinned.copy(pinned = false)), today).featured?.event?.title)
    }

    @Test
    fun `ピン留めした予定が終わっていたらいちばん近い予定を先頭にする`() {
        val near = event(LocalDate.of(2026, 10, 14), title = "近い")
        val endedPinned = event(LocalDate.of(2026, 10, 1), pinned = true, title = "終わった")

        assertEquals("近い", buildSchedule(listOf(near, endedPinned), today).featured?.event?.title)
    }

    @Test
    fun `予定が無ければ先頭も無い`() {
        val schedule = buildSchedule(listOf(event(LocalDate.of(2026, 1, 1))), today)

        assertNull(schedule.featured)
        assertEquals(emptyList<ScheduledEvent>(), schedule.upcoming)
    }

    @Test
    fun `同じ日時なら登録した順`() {
        val first = event(LocalDate.of(2026, 10, 20), title = "先", createdAt = Instant.parse("2026-01-01T00:00:00Z"))
        val second = event(LocalDate.of(2026, 10, 20), title = "後", createdAt = Instant.parse("2026-02-01T00:00:00Z"))

        assertEquals(listOf("先", "後"), buildSchedule(listOf(second, first), today).upcoming.map { it.event.title })
    }
}
