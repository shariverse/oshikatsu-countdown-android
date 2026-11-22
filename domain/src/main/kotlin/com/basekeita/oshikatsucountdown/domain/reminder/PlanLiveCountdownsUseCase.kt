package com.basekeita.oshikatsucountdown.domain.reminder

import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import com.basekeita.oshikatsucountdown.domain.countdown.occurrenceOn
import com.basekeita.oshikatsucountdown.domain.countdown.previousOccurrence
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

/**
 * 1 件の予定の当日のカウントダウンを出す時間帯（開演の 3 時間前から、開演の 1 時間後まで）。
 *
 * @property occurrenceDate 数える開催日（毎年くり返す予定はその回）。
 * @property start 開演（開始）の時点。
 */
data class LiveCountdownWindow(
    val eventId: UUID,
    val occurrenceDate: LocalDate,
    val start: Instant,
) {
    /** 出し始める時点。 */
    val begin: Instant get() = start.minus(PlanLiveCountdownsUseCase.LEAD_TIME)

    /** 終える時点。 */
    val end: Instant get() = start.plus(PlanLiveCountdownsUseCase.LINGER_TIME)

    /** 通知とアラームの識別子。同じ予定・回なら同じ値になり、置き直しても重ならない。 */
    val identifier: String
        get() = "countdown.$eventId.$occurrenceDate"
}

/**
 * @property current 今が時間帯に入っているもの。すぐに出す。始まりの早い順。
 * @property scheduled これから時間帯が始まるもの。始まる時刻に予約する。始まりの早い順。
 */
data class LiveCountdownPlan(
    val current: List<LiveCountdownWindow>,
    val scheduled: List<LiveCountdownWindow>,
)

/**
 * どの予定の当日のカウントダウンを、いつ出すか。`now` の時点の計画を、`zone` のタイムゾーンで求める。iOS 版の `LiveActivityPlan` と同じ規則。
 *
 * - 開始の時刻がある予定だけ。時刻の無い予定（誕生日など）には出さない
 * - 開演の 3 時間前から、開演の 1 時間後まで出す
 * - 予約するのは `SCHEDULING_HORIZON` の先までに始まる、近い順に `MAXIMUM_SCHEDULED_COUNT` 件まで
 */
class PlanLiveCountdownsUseCase
@Inject
constructor() {
    operator fun invoke(
        events: List<OshiEvent>,
        now: Instant,
        zone: ZoneId,
    ): LiveCountdownPlan {
        // LocalDate.ofInstant は Java 9 の API で、Android 13 以前の端末に無いので使わない
        val today = now.atZone(zone).toLocalDate()
        val windows = events
            .flatMap { event ->
                val startTime = event.startTime ?: return@flatMap emptyList()
                occurrences(event, today).map { occurrence ->
                    LiveCountdownWindow(event.id, occurrence, occurrence.atTime(startTime).atZone(zone).toInstant())
                }
            }
            .sortedWith(compareBy<LiveCountdownWindow> { it.start }.thenBy { it.eventId.toString() })
        val horizon = now.plus(SCHEDULING_HORIZON)
        return LiveCountdownPlan(
            current = windows.filter { it.begin <= now && now < it.end },
            scheduled = windows.filter { it.begin > now && it.begin <= horizon }.take(MAXIMUM_SCHEDULED_COUNT),
        )
    }

    /**
     * 見る開催日。毎年くり返す予定は前の回も見る（夜遅くの開演の時間帯は日付をまたぎ、
     * 日付が変わると数える回が翌年に移るため）。
     */
    private fun occurrences(
        event: OshiEvent,
        today: LocalDate,
    ): List<LocalDate> {
        val occurrence = event.occurrenceOn(today)
        val previous = event.previousOccurrence(occurrence) ?: return listOf(occurrence)
        return listOf(previous, occurrence)
    }

    companion object {
        /** 開演のどれだけ前から出すか。 */
        val LEAD_TIME: Duration = Duration.ofHours(3)

        /** 開演のどれだけ後まで出すか。 */
        val LINGER_TIME: Duration = Duration.ofHours(1)

        /** 予約する先の長さ（iOS 版と同じ）。Android は 7 日前・前日の通知を出すときにも置き直すので、アプリを開かなくても先の予約が入る。 */
        val SCHEDULING_HORIZON: Duration = Duration.ofDays(8)

        /** 予約しておく数の上限（iOS 版と同じ）。 */
        const val MAXIMUM_SCHEDULED_COUNT = 3
    }
}
