package com.basekeita.oshikatsucountdown.domain.countdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class EventOccurrenceTest {
    private val today = LocalDate.of(2026, 10, 13)

    @Test
    fun `くり返さない予定は過ぎていても登録した日付のまま`() {
        assertEquals(LocalDate.of(2026, 10, 18), event(LocalDate.of(2026, 10, 18)).occurrenceOn(today))
        assertEquals(LocalDate.of(2026, 9, 1), event(LocalDate.of(2026, 9, 1)).occurrenceOn(today))
    }

    @Test
    fun `毎年くり返す予定は今年の同じ日がまだなら今年`() {
        val birthday = event(LocalDate.of(2000, 11, 10), repeatsYearly = true)

        assertEquals(LocalDate.of(2026, 11, 10), birthday.occurrenceOn(today))
    }

    @Test
    fun `毎年くり返す予定は今日なら今日で過ぎていれば来年`() {
        assertEquals(today, event(LocalDate.of(2001, 10, 13), repeatsYearly = true).occurrenceOn(today))
        assertEquals(LocalDate.of(2027, 10, 12), event(LocalDate.of(2001, 10, 12), repeatsYearly = true).occurrenceOn(today))
    }

    @Test
    fun `毎年くり返す予定でも初回がまだ先なら初回`() {
        val anniversary = event(LocalDate.of(2027, 3, 1), repeatsYearly = true)

        assertEquals(LocalDate.of(2027, 3, 1), anniversary.occurrenceOn(today))
    }

    @Test
    fun `2月29日の予定は平年には2月28日`() {
        val leapDay = event(LocalDate.of(2024, 2, 29), repeatsYearly = true)

        assertEquals(LocalDate.of(2027, 2, 28), leapDay.occurrenceOn(today))
        assertEquals(LocalDate.of(2028, 2, 29), leapDay.occurrenceOn(LocalDate.of(2027, 3, 1)))
    }

    @Test
    fun `前の回は毎年くり返す予定の2回目以降だけ`() {
        val birthday = event(LocalDate.of(2000, 11, 10), repeatsYearly = true)

        assertEquals(LocalDate.of(2025, 11, 10), birthday.previousOccurrence(LocalDate.of(2026, 11, 10)))
        assertNull(birthday.previousOccurrence(LocalDate.of(2000, 11, 10)))
        assertNull(event(LocalDate.of(2026, 11, 10)).previousOccurrence(LocalDate.of(2026, 11, 10)))
    }
}
