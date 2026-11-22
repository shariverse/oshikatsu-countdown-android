package com.basekeita.oshikatsucountdown.domain.reminder

import com.basekeita.oshikatsucountdown.data.definition.model.EventType
import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import com.basekeita.oshikatsucountdown.data.definition.model.ReminderSettings
import com.basekeita.oshikatsucountdown.data.definition.model.ReminderTiming
import com.basekeita.oshikatsucountdown.data.definition.model.ReminderTiming.DAY_BEFORE
import com.basekeita.oshikatsucountdown.data.definition.model.ReminderTiming.HOUR_BEFORE_START
import com.basekeita.oshikatsucountdown.data.definition.model.ReminderTiming.ON_THE_DAY
import com.basekeita.oshikatsucountdown.data.definition.model.ReminderTiming.SEVEN_DAYS_BEFORE
import com.basekeita.oshikatsucountdown.data.definition.model.ReminderTiming.THIRTY_DAYS_BEFORE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** iOS 版の `ReminderPlanTests` と同じ場面を確かめる。 */
class PlanRemindersUseCaseTest {
    private val tokyo = ZoneId.of("Asia/Tokyo")
    private val newYork = ZoneId.of("America/New_York")
    private val planReminders = PlanRemindersUseCase()

    private fun event(
        date: LocalDate,
        startTime: LocalTime? = null,
        repeatsYearly: Boolean = false,
        title: String = "予定",
    ): OshiEvent = OshiEvent(
        oshiId = null,
        type = EventType.LIVE,
        title = title,
        date = date,
        startTime = startTime,
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

    private fun settings(
        timings: Set<ReminderTiming> = ReminderTiming.entries.toSet(),
        time: LocalTime = LocalTime.of(9, 0),
    ) = ReminderSettings(enabled = true, timings = timings, timeOfDay = time)

    private fun plan(
        events: List<OshiEvent>,
        now: Instant,
        settings: ReminderSettings = ReminderSettings(),
        zone: ZoneId = tokyo,
        limit: Int = PlanRemindersUseCase.MAXIMUM_COUNT,
    ): List<PlannedReminder> = planReminders(events, settings, now, zone, limit)

    @Test
    fun `開演の時刻がある予定は5つのタイミングを近い順に出し、日付で決まる通知は設定の時刻に出す`() {
        val live = event(LocalDate.of(2026, 10, 18), startTime = LocalTime.of(17, 0))

        val reminders = plan(listOf(live), now = at(2026, 9, 1, 12))

        assertEquals(listOf(THIRTY_DAYS_BEFORE, SEVEN_DAYS_BEFORE, DAY_BEFORE, ON_THE_DAY, HOUR_BEFORE_START), reminders.map { it.timing })
        assertEquals(
            listOf(
                LocalDateTime.of(2026, 9, 18, 9, 0),
                LocalDateTime.of(2026, 10, 11, 9, 0),
                LocalDateTime.of(2026, 10, 17, 9, 0),
                LocalDateTime.of(2026, 10, 18, 9, 0),
                LocalDateTime.of(2026, 10, 18, 16, 0),
            ),
            reminders.map { it.fireDateTime },
        )
        assertEquals(at(2026, 10, 18, 16), reminders.last().fireAt)
        assertTrue(reminders.all { it.eventId == live.id && it.occurrenceDate == LocalDate.of(2026, 10, 18) })
    }

    @Test
    fun `開演の時刻が無い予定は開演1時間前を出さない`() {
        val reminders = plan(listOf(event(LocalDate.of(2026, 10, 18))), now = at(2026, 9, 1))

        assertEquals(listOf(THIRTY_DAYS_BEFORE, SEVEN_DAYS_BEFORE, DAY_BEFORE, ON_THE_DAY), reminders.map { it.timing })
        assertEquals(at(2026, 10, 18, 9), reminders.last().fireAt)
    }

    @Test
    fun `設定で選んだタイミングと時刻だけ出す`() {
        val live = event(LocalDate.of(2026, 10, 18), startTime = LocalTime.of(17, 0))

        val reminders = plan(listOf(live), now = at(2026, 9, 1), settings = settings(setOf(DAY_BEFORE, ON_THE_DAY), LocalTime.of(20, 30)))

        // 当日の 20:30 は開演より後なので出さない
        assertEquals(listOf(DAY_BEFORE), reminders.map { it.timing })
        assertEquals(at(2026, 10, 17, 20, 30), reminders.first().fireAt)
    }

    @Test
    fun `通知がOFFなら何も予約しない`() {
        val reminders = plan(listOf(event(LocalDate.of(2026, 10, 18))), now = at(2026, 9, 1), settings = ReminderSettings(enabled = false))

        assertEquals(emptyList<PlannedReminder>(), reminders)
    }

    @Test
    fun `今より前の時刻は出さず、ちょうど今の時刻も出さない`() {
        val events = listOf(event(LocalDate.of(2026, 10, 18)))

        // 予定の 10 日前に登録した: 30 日前は過ぎている
        assertEquals(listOf(SEVEN_DAYS_BEFORE, DAY_BEFORE, ON_THE_DAY), plan(events, now = at(2026, 10, 8, 12)).map { it.timing })
        assertEquals(listOf(ON_THE_DAY), plan(events, now = at(2026, 10, 17, 9)).map { it.timing })
        assertEquals(listOf(DAY_BEFORE, ON_THE_DAY), plan(events, now = at(2026, 10, 17, 8, 59, 59)).map { it.timing })
    }

    @Test
    fun `終わった予定は出さない`() {
        assertEquals(emptyList<PlannedReminder>(), plan(listOf(event(LocalDate.of(2026, 10, 15))), now = at(2026, 10, 18)))
    }

    @Test
    fun `当日の通知は開演1時間前より前に出るときだけ出す`() {
        fun timings(start: LocalTime): List<ReminderTiming> = plan(
            listOf(event(LocalDate.of(2026, 10, 18), startTime = start)),
            now = at(2026, 10, 1),
            settings = settings(setOf(ON_THE_DAY, HOUR_BEFORE_START)),
        ).map { it.timing }

        // 開演 11:00: 当日 9:00 は 1 時間前（10:00）より前
        assertEquals(listOf(ON_THE_DAY, HOUR_BEFORE_START), timings(LocalTime.of(11, 0)))
        // 開演 10:00: 当日 9:00 と 1 時間前が重なるので、1 時間前だけ
        assertEquals(listOf(HOUR_BEFORE_START), timings(LocalTime.of(10, 0)))
        // 開演 9:30: 当日 9:00 が 1 時間前（8:30）より後になるので、1 時間前だけ
        assertEquals(listOf(HOUR_BEFORE_START), timings(LocalTime.of(9, 30)))
        // 開演 8:00: 当日 9:00 は開演に間に合わない
        assertEquals(listOf(HOUR_BEFORE_START), timings(LocalTime.of(8, 0)))
    }

    @Test
    fun `開演1時間前を出さない設定なら当日の通知は開演より前に出るときだけ出す`() {
        fun timings(start: LocalTime): List<ReminderTiming> = plan(
            listOf(event(LocalDate.of(2026, 10, 18), startTime = start)),
            now = at(2026, 10, 1),
            settings = settings(setOf(ON_THE_DAY)),
        ).map { it.timing }

        assertEquals(listOf(ON_THE_DAY), timings(LocalTime.of(9, 30)))
        assertEquals(emptyList<ReminderTiming>(), timings(LocalTime.of(9, 0)))
        assertEquals(emptyList<ReminderTiming>(), timings(LocalTime.of(8, 0)))
    }

    @Test
    fun `開演が0時台なら1時間前は前日で、前日の通知もその前に出るときだけ出す`() {
        val live = event(LocalDate.of(2026, 10, 18), startTime = LocalTime.of(0, 30))

        val reminders = plan(listOf(live), now = at(2026, 10, 1), settings = settings(setOf(DAY_BEFORE, ON_THE_DAY, HOUR_BEFORE_START)))

        assertEquals(listOf(DAY_BEFORE, HOUR_BEFORE_START), reminders.map { it.timing })
        assertEquals(LocalDateTime.of(2026, 10, 17, 23, 30), reminders.last().fireDateTime)

        val late = plan(listOf(live), now = at(2026, 10, 1), settings = settings(setOf(DAY_BEFORE, HOUR_BEFORE_START), LocalTime.of(23, 45)))

        assertEquals(listOf(HOUR_BEFORE_START), late.map { it.timing })
    }

    @Test
    fun `開演1時間前は実際の経過時間で数える（夏時間が始まる日）`() {
        // 2026-03-08 2:00 に夏時間が始まり、時計が 3:00 に進む。3:30 の 1 時間前は 1:30
        val live = event(LocalDate.of(2026, 3, 8), startTime = LocalTime.of(3, 30))

        val reminders = plan(listOf(live), now = at(2026, 3, 1, zone = newYork), settings = settings(setOf(HOUR_BEFORE_START)), zone = newYork)

        assertEquals(LocalDateTime.of(2026, 3, 8, 1, 30), reminders.first().fireDateTime)
        assertEquals(at(2026, 3, 8, 3, 30, zone = newYork).minus(Duration.ofHours(1)), reminders.first().fireAt)
    }

    @Test
    fun `日付と時刻は書いたままで、タイムゾーンが違っても同じ日付と時刻に出す`() {
        val live = listOf(event(LocalDate.of(2026, 10, 18), startTime = LocalTime.of(17, 0)))

        val inTokyo = plan(live, now = at(2026, 9, 1))
        val inNewYork = plan(live, now = at(2026, 9, 1, zone = newYork), zone = newYork)

        assertEquals(inTokyo.map { it.fireDateTime }, inNewYork.map { it.fireDateTime })
        assertEquals(at(2026, 9, 18, 9, zone = newYork), inNewYork.first().fireAt)
    }

    @Test
    fun `毎年くり返す予定は次の回とその次の回まで出す`() {
        val birthday = event(LocalDate.of(2000, 10, 18), repeatsYearly = true)

        val reminders = plan(listOf(birthday), now = at(2026, 10, 1))

        // 今年の 30 日前（9/18）は過ぎている
        val thisYear = LocalDate.of(2026, 10, 18)
        val nextYear = LocalDate.of(2027, 10, 18)
        assertEquals(listOf(thisYear, thisYear, thisYear, nextYear, nextYear, nextYear, nextYear), reminders.map { it.occurrenceDate })
        assertEquals(
            listOf(SEVEN_DAYS_BEFORE, DAY_BEFORE, ON_THE_DAY, THIRTY_DAYS_BEFORE, SEVEN_DAYS_BEFORE, DAY_BEFORE, ON_THE_DAY),
            reminders.map { it.timing },
        )
    }

    @Test
    fun `毎年くり返す予定の当日を過ぎたら翌年の回を出す`() {
        val birthday = event(LocalDate.of(2000, 10, 18), repeatsYearly = true)

        val reminders = plan(listOf(birthday), now = at(2026, 10, 18, 10))

        // 今年の回は当日の 9:00 を過ぎて出すものが残っていないので、翌年の回だけ
        assertEquals(listOf(THIRTY_DAYS_BEFORE, SEVEN_DAYS_BEFORE, DAY_BEFORE, ON_THE_DAY), reminders.map { it.timing })
        assertTrue(reminders.all { it.occurrenceDate == LocalDate.of(2027, 10, 18) })
    }

    @Test
    fun `2月29日の予定は平年は2月28日、閏年は2月29日に数える`() {
        val birthday = event(LocalDate.of(2000, 2, 29), repeatsYearly = true)

        val reminders = plan(listOf(birthday), now = at(2026, 3, 1), settings = settings(setOf(ON_THE_DAY)))

        assertEquals(listOf(LocalDate.of(2027, 2, 28), LocalDate.of(2028, 2, 29)), reminders.map { it.fireDateTime.toLocalDate() })
    }

    @Test
    fun `数えきれないときは近い順に上限まで出す`() {
        val far = event(LocalDate.of(2026, 12, 1), title = "遠い")
        val near = event(LocalDate.of(2026, 10, 18), title = "近い")

        val reminders = plan(listOf(far, near), now = at(2026, 10, 1), settings = settings(setOf(DAY_BEFORE, ON_THE_DAY)), limit = 3)

        assertEquals(listOf(near.id, near.id, far.id), reminders.map { it.eventId })
        assertEquals(listOf(DAY_BEFORE, ON_THE_DAY, DAY_BEFORE), reminders.map { it.timing })
    }

    @Test
    fun `既定の上限は64件`() {
        val events = (1..20).map { day -> event(LocalDate.of(2026, 12, day), title = "予定 $day") }

        val reminders = plan(events, now = at(2026, 10, 1))

        assertEquals(64, reminders.size)
        assertEquals(reminders.map { it.fireAt }.sorted(), reminders.map { it.fireAt })
    }

    @Test
    fun `識別子は予定と回とタイミングで決まり、計画し直しても変わらない`() {
        val live = event(LocalDate.of(2026, 10, 18), startTime = LocalTime.of(17, 0))

        val earlier = plan(listOf(live), now = at(2026, 9, 1))
        val later = plan(listOf(live), now = at(2026, 10, 12))

        assertTrue(earlier.map { it.identifier }.containsAll(later.map { it.identifier }))
        assertEquals(earlier.size, earlier.map { it.identifier }.toSet().size)
        assertEquals("reminder.${live.id}.2026-10-18.THIRTY_DAYS_BEFORE", earlier.first().identifier)
    }

    @Test
    fun `毎年くり返す1月1日の予定の30日前は、前の年の12月2日に出す`() {
        val newYear = event(LocalDate.of(2020, 1, 1), repeatsYearly = true)

        val reminders = plan(listOf(newYear), now = at(2026, 11, 1), settings = settings(setOf(THIRTY_DAYS_BEFORE)))

        assertEquals(LocalDateTime.of(2026, 12, 2, 9, 0), reminders.first().fireDateTime)
        assertEquals(LocalDate.of(2027, 1, 1), reminders.first().occurrenceDate)
    }

    @Test
    fun `開演がちょうど0時なら当日の通知は出さず、前日と前日23時の1時間前を出す`() {
        val live = event(LocalDate.of(2026, 10, 18), startTime = LocalTime.MIDNIGHT)

        val reminders = plan(listOf(live), now = at(2026, 10, 1), settings = settings(setOf(DAY_BEFORE, ON_THE_DAY, HOUR_BEFORE_START)))

        assertEquals(listOf(DAY_BEFORE, HOUR_BEFORE_START), reminders.map { it.timing })
        assertEquals(listOf(LocalDateTime.of(2026, 10, 17, 9, 0), LocalDateTime.of(2026, 10, 17, 23, 0)), reminders.map { it.fireDateTime })
    }
}
