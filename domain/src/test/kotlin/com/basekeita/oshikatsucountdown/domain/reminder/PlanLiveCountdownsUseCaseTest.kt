package com.basekeita.oshikatsucountdown.domain.reminder

import com.basekeita.oshikatsucountdown.data.definition.model.EventType
import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** iOS 版の `LiveActivityPlanTests` と同じ場面を確かめる。 */
class PlanLiveCountdownsUseCaseTest {
    private val tokyo = ZoneId.of("Asia/Tokyo")
    private val planLiveCountdowns = PlanLiveCountdownsUseCase()

    private fun live(
        date: LocalDate,
        hour: Int,
        minute: Int = 0,
        title: String = "ライブ",
        repeatsYearly: Boolean = false,
    ): OshiEvent = OshiEvent(
        oshiId = null,
        type = EventType.LIVE,
        title = title,
        date = date,
        startTime = LocalTime.of(hour, minute),
        repeatsYearly = repeatsYearly,
        createdAt = Instant.EPOCH,
    )

    private fun at(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 0,
        minute: Int = 0,
        second: Int = 0,
        zone: ZoneId = tokyo,
    ): Instant = LocalDateTime.of(year, month, day, hour, minute, second).atZone(zone).toInstant()

    private fun plan(
        events: List<OshiEvent>,
        now: Instant,
        zone: ZoneId = tokyo,
    ): LiveCountdownPlan = planLiveCountdowns(events, now, zone)

    @Test
    fun `開演の3時間前から開演の1時間後まで出す`() {
        val event = live(LocalDate.of(2026, 10, 18), 17)

        assertEquals(emptyList<LiveCountdownWindow>(), plan(listOf(event), at(2026, 10, 18, 13, 59, 59)).current)
        assertEquals(listOf(event.id), plan(listOf(event), at(2026, 10, 18, 14)).current.map { it.eventId })
        assertEquals(listOf(event.id), plan(listOf(event), at(2026, 10, 18, 17, 30)).current.map { it.eventId })
        assertEquals(emptyList<LiveCountdownWindow>(), plan(listOf(event), at(2026, 10, 18, 18)).current)

        val window = plan(listOf(event), at(2026, 10, 18, 15)).current.first()
        assertEquals(at(2026, 10, 18, 17), window.start)
        assertEquals(at(2026, 10, 18, 14), window.begin)
        assertEquals(at(2026, 10, 18, 18), window.end)
        assertEquals(LocalDate.of(2026, 10, 18), window.occurrenceDate)
    }

    @Test
    fun `開演の時刻が無い予定には出さない`() {
        val birthday = live(LocalDate.of(2026, 10, 18), 0).copy(startTime = null)

        val result = plan(listOf(birthday), at(2026, 10, 18, 12))

        assertEquals(LiveCountdownPlan(current = emptyList(), scheduled = emptyList()), result)
    }

    @Test
    fun `時間帯が始まる前の予定は始まる時刻に予約する`() {
        val event = live(LocalDate.of(2026, 10, 18), 17)

        val result = plan(listOf(event), at(2026, 10, 15, 12))

        assertEquals(emptyList<LiveCountdownWindow>(), result.current)
        assertEquals(listOf(event.id), result.scheduled.map { it.eventId })
        assertEquals(at(2026, 10, 18, 14), result.scheduled.first().begin)
    }

    @Test
    fun `予約するのは8日先までの近い順に3件まで`() {
        val now = at(2026, 10, 1, 12)
        val events = (2..6).map { day -> live(LocalDate.of(2026, 10, day), 18, title = "予定 $day") }
        val far = live(LocalDate.of(2026, 10, 20), 18, title = "遠い")

        assertEquals(events.take(3).map { it.id }, plan(events.reversed() + far, now).scheduled.map { it.eventId })

        // 8 日先（10/9 12:00）までに始まるものだけ
        val edge = live(LocalDate.of(2026, 10, 9), 15, title = "ちょうど")
        val beyond = live(LocalDate.of(2026, 10, 9), 15, 1, title = "過ぎる")
        assertEquals(listOf(edge.id), plan(listOf(edge), now).scheduled.map { it.eventId })
        assertEquals(emptyList<LiveCountdownWindow>(), plan(listOf(beyond), now).scheduled)
    }

    @Test
    fun `時間帯を過ぎた予定と終わった予定は出さない`() {
        val today = live(LocalDate.of(2026, 10, 18), 10)
        val ended = live(LocalDate.of(2026, 10, 10), 18)

        val result = plan(listOf(today, ended), at(2026, 10, 18, 12))

        assertEquals(LiveCountdownPlan(current = emptyList(), scheduled = emptyList()), result)
    }

    @Test
    fun `同じ時間帯に複数あれば開演の早い順にすべて出す`() {
        val late = live(LocalDate.of(2026, 10, 18), 18, title = "夜の部")
        val early = live(LocalDate.of(2026, 10, 18), 16, title = "昼の部")

        assertEquals(listOf(early.id, late.id), plan(listOf(late, early), at(2026, 10, 18, 15, 30)).current.map { it.eventId })
    }

    @Test
    fun `開演が0時台なら前の日の夜から出す`() {
        val event = live(LocalDate.of(2026, 10, 18), 0, 30)

        assertEquals(listOf(event.id), plan(listOf(event), at(2026, 10, 17, 21, 30)).current.map { it.eventId })
    }

    @Test
    fun `毎年くり返す予定はその年の回に出し、日付をまたぐ時間帯は日付が変わっても前の回を出し続ける`() {
        val birthday = live(LocalDate.of(2000, 10, 18), 23, 30, title = "誕生日配信", repeatsYearly = true)

        assertEquals(listOf(LocalDate.of(2026, 10, 18)), plan(listOf(birthday), at(2026, 10, 18, 21)).current.map { it.occurrenceDate })
        // 10/19 0:10 は、数える回が翌年に移っても、前の回（開演 23:30 の 1 時間後の 0:30 まで）を出す
        assertEquals(listOf(LocalDate.of(2026, 10, 18)), plan(listOf(birthday), at(2026, 10, 19, 0, 10)).current.map { it.occurrenceDate })
    }

    @Test
    fun `日付と時刻は書いたまま端末のタイムゾーンで数える`() {
        val newYork = ZoneId.of("America/New_York")
        val event = live(LocalDate.of(2026, 10, 18), 17)

        val window = plan(listOf(event), at(2026, 10, 18, 15, zone = newYork), zone = newYork).current.first()

        assertEquals(at(2026, 10, 18, 17, zone = newYork), window.start)
    }

    @Test
    fun `同じ予定と回なら、置き直しても同じ時間帯と識別子になる`() {
        val event = live(LocalDate.of(2026, 10, 18), 17)

        val first = plan(listOf(event), at(2026, 10, 18, 15)).current.first()
        val second = plan(listOf(event), at(2026, 10, 18, 16)).current.first()

        assertEquals(first, second)
        assertTrue(first.identifier.startsWith("countdown.${event.id}.2026-10-18"))
    }

    @Test
    fun `毎年くり返す1月1日0時30分の予定は、前の年の12月31日21時30分から出す`() {
        val newYear = live(LocalDate.of(2020, 1, 1), 0, 30, title = "年越し配信", repeatsYearly = true)

        assertEquals(emptyList<LiveCountdownWindow>(), plan(listOf(newYear), at(2026, 12, 31, 21, 29)).current)
        assertEquals(listOf(LocalDate.of(2027, 1, 1)), plan(listOf(newYear), at(2026, 12, 31, 21, 30)).current.map { it.occurrenceDate })
    }

    @Test
    fun `毎年くり返す2月29日の予定は、平年は2月28日に出す`() {
        val birthday = live(LocalDate.of(2000, 2, 29), 20, title = "誕生日配信", repeatsYearly = true)

        assertEquals(listOf(LocalDate.of(2027, 2, 28)), plan(listOf(birthday), at(2027, 2, 28, 19)).current.map { it.occurrenceDate })
        assertEquals(listOf(LocalDate.of(2028, 2, 29)), plan(listOf(birthday), at(2028, 2, 29, 19)).current.map { it.occurrenceDate })
    }

    @Test
    fun `くり返さない予定でも、日付をまたぐ時間帯は日付が変わっても出し続ける`() {
        val event = live(LocalDate.of(2026, 10, 18), 23, 30)

        assertEquals(listOf(event.id), plan(listOf(event), at(2026, 10, 19, 0, 10)).current.map { it.eventId })
        assertEquals(emptyList<LiveCountdownWindow>(), plan(listOf(event), at(2026, 10, 19, 0, 30)).current)
    }
}
