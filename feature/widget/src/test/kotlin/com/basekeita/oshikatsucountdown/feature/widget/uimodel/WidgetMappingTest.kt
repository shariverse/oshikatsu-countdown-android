package com.basekeita.oshikatsucountdown.feature.widget.uimodel

import com.basekeita.oshikatsucountdown.data.definition.model.EventType
import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.model.OshiColor
import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import com.basekeita.oshikatsucountdown.domain.countdown.BuildEventScheduleUseCase
import com.basekeita.oshikatsucountdown.ui.uimodel.EventTypeUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class WidgetMappingTest {
    private val today = LocalDate.of(2026, 10, 7)

    private val lumina = Oshi(name = "LUMINA", color = OshiColor.PINK)
    private val hiyori = Oshi(name = "ひより", color = OshiColor.ORANGE)

    private fun event(
        title: String,
        date: LocalDate,
        oshi: Oshi? = null,
        type: EventType = EventType.LIVE,
        repeatsYearly: Boolean = false,
    ) = OshiEvent(
        oshiId = oshi?.id,
        type = type,
        title = title,
        date = date,
        repeatsYearly = repeatsYearly,
        createdAt = Instant.EPOCH,
    )

    private val live = event("LUMINA 5th LIVE", LocalDate.of(2026, 10, 18), lumina)
    private val birthday = event("ひよりの誕生日", LocalDate.of(2020, 11, 10), hiyori, EventType.BIRTHDAY, repeatsYearly = true)
    private val todayRelease = event("写真集", today, type = EventType.RELEASE)
    private val ended = event("FCイベント", LocalDate.of(2026, 9, 1), lumina)

    private val schedule = BuildEventScheduleUseCase()(listOf(live, birthday, todayRelease, ended), today)

    @Test
    fun `一覧は当日とこれからの予定を近い順に並べ終わった予定は出さない`() {
        val events = schedule.upcomingWidgetEvents(listOf(lumina, hiyori))

        assertEquals(listOf("写真集", "LUMINA 5th LIVE", "ひよりの誕生日"), events.map { it.title })
        assertEquals(listOf(0L, 11L, 34L), events.map { it.daysUntil })
        assertEquals(
            listOf(WidgetEventStatusUiModel.TODAY, WidgetEventStatusUiModel.UPCOMING, WidgetEventStatusUiModel.UPCOMING),
            events.map { it.status },
        )
    }

    @Test
    fun `推しの名前と推し色を引き推しなしの予定は無彩色にする`() {
        val events = schedule.upcomingWidgetEvents(listOf(lumina, hiyori)).associateBy { it.title }

        assertEquals("LUMINA", events.getValue("LUMINA 5th LIVE").oshiName)
        assertEquals(OshiColorUiModel.PINK, events.getValue("LUMINA 5th LIVE").color)
        assertEquals(OshiColorUiModel.ORANGE, events.getValue("ひよりの誕生日").color)
        assertNull(events.getValue("写真集").oshiName)
        assertEquals(OshiColorUiModel.BLACK, events.getValue("写真集").color)
        assertEquals(EventTypeUiModel.RELEASE, events.getValue("写真集").type)
        assertEquals("写", events.getValue("写真集").initial)
    }

    @Test
    fun `1件のウィジェットでは毎年くり返す予定の次の回を数える`() {
        val event = schedule.findWidgetEvent(birthday.id.toString(), listOf(lumina, hiyori))

        assertEquals(LocalDate.of(2026, 11, 10), event?.occurrenceDate)
        assertEquals(34L, event?.daysUntil)
    }

    @Test
    fun `1件のウィジェットでは終わった予定も探して何日前かを出せる`() {
        val event = schedule.findWidgetEvent(ended.id.toString(), listOf(lumina))

        assertEquals(WidgetEventStatusUiModel.ENDED, event?.status)
        assertEquals(-36L, event?.daysUntil)
    }

    @Test
    fun `選んでいない予定と削除された予定は見つからない`() {
        assertNull(schedule.findWidgetEvent(null, listOf(lumina)))
        assertNull(schedule.findWidgetEvent(UUID.randomUUID().toString(), listOf(lumina)))
    }

    @Test
    fun `推しが消えた予定は推しなしとして出す`() {
        val event = schedule.findWidgetEvent(live.id.toString(), emptyList())

        assertNull(event?.oshiName)
        assertEquals(OshiColorUiModel.BLACK, event?.color)
    }
}
