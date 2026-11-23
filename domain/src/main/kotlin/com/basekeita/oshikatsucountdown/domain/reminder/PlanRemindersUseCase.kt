package com.basekeita.oshikatsucountdown.domain.reminder

import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import com.basekeita.oshikatsucountdown.data.definition.model.ReminderSettings
import com.basekeita.oshikatsucountdown.data.definition.model.ReminderTiming
import com.basekeita.oshikatsucountdown.domain.countdown.occurrenceOn
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

/**
 * 予約する通知 1 件。
 *
 * @property occurrenceDate 通知が知らせる開催日（毎年くり返す予定はその回）。
 * @property fireDateTime 出す日付と時刻（計画を作ったタイムゾーンでの値）。
 * @property fireAt 出す時点。予約・過ぎたかどうか・並びに使う。タイムゾーンが変わったら計画し直し、
 *   その土地の同じ日付・時刻に出す。
 */
data class PlannedReminder(
    val eventId: UUID,
    val timing: ReminderTiming,
    val occurrenceDate: LocalDate,
    val fireDateTime: LocalDateTime,
    val fireAt: Instant,
) {
    /** 予約の識別子。同じ予定・回・タイミングなら同じ値になり、予約し直しても重ならない。 */
    val identifier: String
        get() = "reminder.$eventId.$occurrenceDate.${timing.name}"
}

/**
 * どの予定に、いつ通知を予約するか。`now` の時点の計画を、`zone` のタイムゾーンで求める。
 *
 * - 日付で決まる通知（N 日前・前日・当日）は、設定で選んだ時刻に出す
 * - 開演 1 時間前は、開始の時刻がある予定だけ。1 時間は実際の経過時間で数える（夏時間をまたいでも 1 時間前）
 * - 開始の時刻がある予定では、日付で決まる通知は、開演 1 時間前（その通知を出さない設定なら開始の時刻）より前に出るものだけ出す。
 *   開演に間に合わない「当日」や、開演 1 時間前と重なる・後になる「当日」を出さないため
 * - 今より後の通知だけ。終わった予定と、過ぎたタイミングは出さない
 * - 毎年くり返す予定は、次の回とその次の回まで計画する（当日を過ぎてアプリを開かなくても、翌年の分が予約に残るように）
 * - 近い順に `limit` 件まで
 */
class PlanRemindersUseCase @Inject constructor() {
    operator fun invoke(
        events: List<OshiEvent>,
        settings: ReminderSettings,
        now: Instant,
        zone: ZoneId,
        limit: Int = MAXIMUM_COUNT,
    ): List<PlannedReminder> {
        if (!settings.enabled) return emptyList()

        // LocalDate.ofInstant は Java 9 の API で、Android 13 以前の端末に無いので使わない
        val today = now.atZone(zone).toLocalDate()
        return events
            .flatMap { event ->
                occurrences(event, today).flatMap { occurrence -> reminders(event, occurrence, settings, zone) }
            }
            .filter { it.fireAt > now }
            .sortedWith(
                compareBy<PlannedReminder> { it.fireAt }
                    .thenBy { it.eventId.toString() }
                    .thenBy { it.timing.ordinal },
            )
            .take(limit.coerceAtLeast(0))
    }

    /** 計画する開催日。毎年くり返す予定は次の回とその次の回（2/29 は平年 2/28）。 */
    private fun occurrences(
        event: OshiEvent,
        today: LocalDate,
    ): List<LocalDate> {
        val occurrence = event.occurrenceOn(today)
        if (!event.repeatsYearly) return listOf(occurrence)

        // その次の回は、登録した日付から求める（2/28 に寄せた回から求めると、閏年の 2/29 に戻れない）
        return listOf(occurrence, event.date.withYear(occurrence.year + 1))
    }

    /** 1 回の開催日に出す通知。過ぎたかどうかは呼ぶ側で確かめる。 */
    private fun reminders(
        event: OshiEvent,
        occurrence: LocalDate,
        settings: ReminderSettings,
        zone: ZoneId,
    ): List<PlannedReminder> {
        val start = event.startTime?.let { occurrence.atTime(it).atZone(zone).toInstant() }
        val hourBeforeStart = start?.minus(HOUR_BEFORE_START)
        // 日付で決まる通知は、この時点より前に出るものだけ（開始の時刻が無ければ制限しない）
        val deadline = if (ReminderTiming.HOUR_BEFORE_START in settings.timings) hourBeforeStart else start

        return ReminderTiming.entries.filter { it in settings.timings }.mapNotNull { timing ->
            val daysBefore = timing.daysBefore
            if (daysBefore != null) {
                val fireDateTime = occurrence.minusDays(daysBefore.toLong()).atTime(settings.timeOfDay)
                val fireAt = fireDateTime.atZone(zone).toInstant()
                if (deadline != null && fireAt >= deadline) {
                    null
                } else {
                    PlannedReminder(event.id, timing, occurrence, fireDateTime, fireAt)
                }
            } else if (hourBeforeStart != null) {
                PlannedReminder(event.id, timing, occurrence, LocalDateTime.ofInstant(hourBeforeStart, zone), hourBeforeStart)
            } else {
                null
            }
        }
    }

    companion object {
        /**
         * 予約する通知の上限。iOS 版（アプリごとに 64 件まで）とそろえ、AlarmManager に積むアラームの数も抑える。
         * 空いた枠は、通知を出したときやアプリが前面に来たときの予約し直しで埋まる。
         */
        const val MAXIMUM_COUNT = 64

        private val HOUR_BEFORE_START: Duration = Duration.ofHours(1)
    }
}
