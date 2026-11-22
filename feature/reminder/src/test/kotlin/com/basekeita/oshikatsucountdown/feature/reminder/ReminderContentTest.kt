package com.basekeita.oshikatsucountdown.feature.reminder

import com.basekeita.oshikatsucountdown.data.definition.model.EventType
import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.model.OshiColor
import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import com.basekeita.oshikatsucountdown.data.definition.model.ReminderTiming
import com.basekeita.oshikatsucountdown.domain.reminder.LiveCountdownWindow
import com.basekeita.oshikatsucountdown.ui.rule.formatMonthDayWeekday
import com.basekeita.oshikatsucountdown.ui.rule.formatTime
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/** iOS 版の `ReminderContentTests` と同じ場面を、strings.xml と同じ文言で確かめる。 */
class ReminderContentTest {
    private val wording = object : ReminderWording {
        override fun daysLeft(days: Int): String = "あと $days 日"

        override val dayBefore = "いよいよ明日！"
        override val today = "今日！"
        override val hourLeft = "あと 1 時間"

        override fun typeLabel(type: EventType): String = when (type) {
            EventType.LIVE -> "ライブ"
            EventType.BIRTHDAY -> "誕生日"
            EventType.RELEASE -> "発売日"
            EventType.STREAM -> "配信"
            EventType.OTHER -> "その他"
        }

        override fun startTime(event: OshiEvent): String {
            val label = when (event.type) {
                EventType.LIVE -> "開演"
                EventType.RELEASE -> "発売"
                EventType.STREAM -> "配信開始"
                EventType.BIRTHDAY, EventType.OTHER -> "開始"
            }
            return "$label ${formatTime(checkNotNull(event.startTime))}"
        }

        override fun monthDayWeekday(date: LocalDate): String = formatMonthDayWeekday(date)

        override fun untilStart(type: EventType): String = when (type) {
            EventType.LIVE -> "開演まで"
            EventType.RELEASE -> "発売まで"
            EventType.STREAM -> "配信開始まで"
            EventType.BIRTHDAY, EventType.OTHER -> "開始まで"
        }

        override val started = "今日は当日！"
    }

    private val day = LocalDate.of(2026, 10, 18)

    private fun event(
        type: EventType = EventType.LIVE,
        startTime: LocalTime? = LocalTime.of(17, 0),
    ) = OshiEvent(
        oshiId = UUID.randomUUID(),
        type = type,
        title = "LUMINA 全国ツアー",
        date = day,
        startTime = startTime,
        createdAt = Instant.EPOCH,
    )

    private fun content(
        timing: ReminderTiming,
        event: OshiEvent = event(),
        oshiName: String? = "LUMINA",
    ) = reminderContentOf(timing, day, event, oshiName, wording)

    @Test
    fun `タイトルは予定のタイトルでサブタイトルは推しの名前`() {
        val reminder = content(ReminderTiming.SEVEN_DAYS_BEFORE)

        assertEquals("LUMINA 全国ツアー", reminder.title)
        assertEquals("LUMINA", reminder.subtitle)
    }

    @Test
    fun `推しなしの予定はサブタイトルを種類にする`() {
        assertEquals("配信", content(ReminderTiming.SEVEN_DAYS_BEFORE, event(type = EventType.STREAM), oshiName = null).subtitle)
    }

    @Test
    fun `N日前は残りの日数と開催日と開始の時刻`() {
        assertEquals("あと 30 日 · 10月18日(日) · 開演 17:00", content(ReminderTiming.THIRTY_DAYS_BEFORE).body)
        assertEquals("あと 7 日 · 10月18日(日) · 開演 17:00", content(ReminderTiming.SEVEN_DAYS_BEFORE).body)
    }

    @Test
    fun `前日はいよいよ明日と開催日`() {
        assertEquals("いよいよ明日！ · 10月18日(日) · 開演 17:00", content(ReminderTiming.DAY_BEFORE).body)
    }

    @Test
    fun `当日と開演1時間前は今日のことなので日付を出さない`() {
        assertEquals("今日！ · 開演 17:00", content(ReminderTiming.ON_THE_DAY).body)
        assertEquals("あと 1 時間 · 開演 17:00", content(ReminderTiming.HOUR_BEFORE_START).body)
    }

    @Test
    fun `開始の時刻が無ければ時刻を出さない`() {
        assertEquals("いよいよ明日！ · 10月18日(日)", content(ReminderTiming.DAY_BEFORE, event(startTime = null)).body)
        assertEquals("今日！", content(ReminderTiming.ON_THE_DAY, event(startTime = null)).body)
    }

    @Test
    fun `当日のカウントダウンの中身は推しの名前とタイトルと推し色と開演の時刻と残りの見出し`() {
        val oshi = Oshi(name = "ミナト", color = OshiColor.NAVY, createdAt = Instant.EPOCH)
        val live = event().copy(oshiId = oshi.id, title = "5th LIVE")
        val start = Instant.parse("2026-10-18T08:00:00Z")
        val window = LiveCountdownWindow(live.id, day, start)

        val content = liveCountdownContentOf(window, live, oshi, wording)

        assertEquals(live.id, content.eventId)
        assertEquals("ミナト", content.heading)
        assertEquals("5th LIVE", content.title)
        assertEquals(OshiColorUiModel.NAVY, content.color)
        assertEquals(start, content.start)
        assertEquals(start.plusSeconds(3_600), content.end)
        assertEquals("開演まで", content.untilText)
        assertEquals("開演 17:00", content.startText)
        assertEquals("今日は当日！", content.startedText)
        assertEquals(false, content.isStarted(start.minusSeconds(1)))
        assertEquals(true, content.isStarted(start))
    }

    @Test
    fun `推しなしの予定のカウントダウンは見出しを種類にして色をブラックにする`() {
        val stream = event(type = EventType.STREAM).copy(oshiId = null)

        val content = liveCountdownContentOf(LiveCountdownWindow(stream.id, day, Instant.EPOCH), stream, oshi = null, wording = wording)

        assertEquals("配信", content.heading)
        assertEquals(OshiColorUiModel.BLACK, content.color)
        assertEquals("配信開始まで", content.untilText)
    }

    @Test
    fun `開始の時刻の呼び名は種類で変わる`() {
        assertEquals("今日！ · 発売 0:00", content(ReminderTiming.ON_THE_DAY, event(type = EventType.RELEASE, startTime = LocalTime.MIDNIGHT)).body)
        assertEquals("今日！ · 開始 9:30", content(ReminderTiming.ON_THE_DAY, event(type = EventType.BIRTHDAY, startTime = LocalTime.of(9, 30))).body)
    }
}
