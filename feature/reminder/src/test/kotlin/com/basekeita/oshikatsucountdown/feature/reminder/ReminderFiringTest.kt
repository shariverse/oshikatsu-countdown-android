package com.basekeita.oshikatsucountdown.feature.reminder

import com.basekeita.oshikatsucountdown.data.definition.model.EventType
import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import com.basekeita.oshikatsucountdown.data.definition.model.ReminderSettings
import com.basekeita.oshikatsucountdown.data.definition.model.ReminderTiming
import com.basekeita.oshikatsucountdown.domain.reminder.PlanRemindersUseCase
import com.basekeita.oshikatsucountdown.domain.reminder.PlannedReminder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** 予約した時刻に届いた通知を、まだ出すかの判断。 */
class ReminderFiringTest {
    private val tokyo = ZoneId.of("Asia/Tokyo")
    private val planReminders = PlanRemindersUseCase()
    private val live = OshiEvent(
        oshiId = null,
        type = EventType.LIVE,
        title = "ライブ",
        date = LocalDate.of(2026, 10, 18),
        startTime = LocalTime.of(17, 0),
        createdAt = Instant.EPOCH,
    )

    /** 予約したときの計画にある、前日の通知。 */
    private val dayBefore: PlannedReminder = planReminders(listOf(live), ReminderSettings(), Instant.parse("2026-10-01T00:00:00Z"), tokyo)
        .first { it.timing == ReminderTiming.DAY_BEFORE }

    private fun isStillPlanned(
        event: OshiEvent? = live,
        settings: ReminderSettings = ReminderSettings(),
        reminder: PlannedReminder = dayBefore,
    ): Boolean = isReminderStillPlanned(planReminders, event, settings, reminder.identifier, reminder.fireAt, tokyo)

    @Test
    fun `予約したときと計画が変わっていなければ出す`() {
        assertTrue(isStillPlanned())
    }

    @Test
    fun `予約し直しが間に合わず、設定を変えた・予定を消したときは出さない`() {
        assertFalse(isStillPlanned(settings = ReminderSettings(enabled = false)))
        assertFalse(isStillPlanned(settings = ReminderSettings(timings = ReminderTiming.entries.toSet() - ReminderTiming.DAY_BEFORE)))
        assertFalse(isStillPlanned(settings = ReminderSettings(timeOfDay = LocalTime.of(20, 0))))
        assertFalse(isStillPlanned(event = null))
    }

    @Test
    fun `予定の日付を変えたあとに古い回の通知が届いても出さない`() {
        assertFalse(isStillPlanned(event = live.copy(date = LocalDate.of(2026, 10, 25))))
    }

    @Test
    fun `開演1時間前の通知は、開演の時刻を変えたら出さない`() {
        val hourBefore = planReminders(listOf(live), ReminderSettings(), Instant.parse("2026-10-01T00:00:00Z"), tokyo)
            .first { it.timing == ReminderTiming.HOUR_BEFORE_START }

        assertTrue(isStillPlanned(reminder = hourBefore))
        assertFalse(isStillPlanned(event = live.copy(startTime = LocalTime.of(18, 0)), reminder = hourBefore))
    }
}
