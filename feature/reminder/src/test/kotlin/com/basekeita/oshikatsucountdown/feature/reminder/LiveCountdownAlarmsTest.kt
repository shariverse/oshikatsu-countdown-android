package com.basekeita.oshikatsucountdown.feature.reminder

import com.basekeita.oshikatsucountdown.domain.reminder.LiveCountdownPlan
import com.basekeita.oshikatsucountdown.domain.reminder.LiveCountdownWindow
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** 当日のカウントダウンのアラームと、知らせるかどうかの判断。 */
class LiveCountdownAlarmsTest {
    private val start = Instant.parse("2026-10-18T08:00:00Z")
    private val current = LiveCountdownWindow(UUID.randomUUID(), LocalDate.of(2026, 10, 18), start)
    private val upcoming = LiveCountdownWindow(UUID.randomUUID(), LocalDate.of(2026, 10, 20), start.plus(Duration.ofDays(2)))

    private fun beginKey(window: LiveCountdownWindow) = countdownAlarmKey(window.identifier, CountdownMoment.BEGIN)

    @Test
    fun `これから始まるものは出し始める時刻、出しているものは開演と1時間後にアラームを入れる`() {
        val now = start.minus(Duration.ofHours(1))
        val plan = LiveCountdownPlan(current = listOf(current), scheduled = listOf(upcoming))

        assertEquals(
            mapOf(
                beginKey(upcoming) to upcoming.begin,
                countdownAlarmKey(current.identifier, CountdownMoment.START) to start,
                countdownAlarmKey(current.identifier, CountdownMoment.END) to current.end,
            ),
            countdownAlarmsOf(plan, now),
        )
    }

    @Test
    fun `開演を過ぎたら開演のアラームは入れない（ちょうど開演でも入れない）`() {
        val plan = LiveCountdownPlan(current = listOf(current), scheduled = emptyList())

        assertEquals(
            setOf(countdownAlarmKey(current.identifier, CountdownMoment.END)),
            countdownAlarmsOf(plan, start).keys,
        )
        assertEquals(
            setOf(countdownAlarmKey(current.identifier, CountdownMoment.START), countdownAlarmKey(current.identifier, CountdownMoment.END)),
            countdownAlarmsOf(plan, start.minusMillis(1)).keys,
        )
    }

    @Test
    fun `出し始める時刻を予約してあり、まだ出していないものだけ知らせる`() {
        assertEquals(setOf(current.identifier), countdownsToAlert(listOf(current), previousAlarmKeys = setOf(beginKey(current)), shown = emptySet()))
    }

    @Test
    fun `予約していなかったもの（時間帯に入ってから作った予定・アプリを開いて初めて出すもの）は知らせない`() {
        assertEquals(emptySet<String>(), countdownsToAlert(listOf(current), previousAlarmKeys = emptySet(), shown = emptySet()))
    }

    @Test
    fun `どのきっかけが先に置き直しても、知らせるのは1回だけ`() {
        val previousAlarmKeys = setOf(beginKey(current))

        // 1 回目の置き直し（アプリのプロセスの開始が、出し始める時刻のアラームより先に通っても）で知らせる
        val first = countdownsToAlert(listOf(current), previousAlarmKeys, shown = emptySet())
        // 1 回目で記録は「出している」に変わり、出し始める時刻のアラームは外れる
        val afterFirst = countdownAlarmsOf(LiveCountdownPlan(current = listOf(current), scheduled = emptyList()), start.minus(Duration.ofHours(2))).keys
        val second = countdownsToAlert(listOf(current), afterFirst, shown = setOf(current.identifier))

        assertEquals(setOf(current.identifier), first)
        assertEquals(emptySet<String>(), second)
    }

    @Test
    fun `途中で止まって記録に前のものが残っていても、出しているものは知らせ直さない`() {
        assertEquals(
            emptySet<String>(),
            countdownsToAlert(listOf(current), previousAlarmKeys = setOf(beginKey(current)), shown = setOf(current.identifier)),
        )
    }
}
