package com.basekeita.oshikatsucountdown.data.impl.mapping

import com.basekeita.oshikatsucountdown.data.definition.model.ReminderSettings
import com.basekeita.oshikatsucountdown.data.definition.model.ReminderTiming
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class ReminderMappingTest {
    @Test
    fun `タイミングのキーは重ならない`() {
        val keys = ReminderTiming.entries.map { it.storageKey }

        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `設定を書いていなければ既定（ON・5つすべて・9時）`() {
        assertEquals(ReminderSettings(), reminderSettingsOf(enabled = null, timingsByStorageKey = emptyMap(), minutesOfDay = null))
    }

    @Test
    fun `保存した値を読む`() {
        val settings = reminderSettingsOf(
            enabled = false,
            timingsByStorageKey = mapOf("thirtyDaysBefore" to false, "hourBeforeStart" to false, "onTheDay" to true),
            minutesOfDay = 20 * 60 + 30,
        )

        assertEquals(
            ReminderSettings(
                enabled = false,
                timings = setOf(ReminderTiming.SEVEN_DAYS_BEFORE, ReminderTiming.DAY_BEFORE, ReminderTiming.ON_THE_DAY),
                timeOfDay = LocalTime.of(20, 30),
            ),
            settings,
        )
    }

    @Test
    fun `1日の外の分は既定の9時にする`() {
        assertEquals(LocalTime.of(9, 0), reminderSettingsOf(null, emptyMap(), minutesOfDay = 24 * 60).timeOfDay)
        assertEquals(LocalTime.of(9, 0), reminderSettingsOf(null, emptyMap(), minutesOfDay = -1).timeOfDay)
    }

    @Test
    fun `時刻を0時からの分にする`() {
        assertEquals(0, LocalTime.MIDNIGHT.toMinutesOfDay())
        assertEquals(23 * 60 + 59, LocalTime.of(23, 59).toMinutesOfDay())
    }
}
