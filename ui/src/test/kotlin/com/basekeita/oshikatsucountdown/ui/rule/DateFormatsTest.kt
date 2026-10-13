package com.basekeita.oshikatsucountdown.ui.rule

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DateFormatsTest {
    private val date = LocalDate.of(2026, 10, 18)

    @Test
    fun `日付の書き方`() {
        assertEquals("10月18日(日)", formatMonthDayWeekday(date))
        assertEquals("2026年10月18日(日)", formatFullDate(date))
        assertEquals("10/18", formatShortDate(date))
        assertEquals("10.18 SUN", formatDateBadge(date))
    }

    @Test
    fun `時刻は時の0を詰める`() {
        assertEquals("17:00", formatTime(LocalTime.of(17, 0)))
        assertEquals("9:05", formatTime(LocalTime.of(9, 5)))
    }
}
