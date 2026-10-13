package com.basekeita.oshikatsucountdown.domain.countdown

import java.time.Duration
import java.time.LocalDate

/**
 * 詳細画面の秒単位のカウントダウン。
 *
 * @property remaining 開始の時刻（時刻が無ければ開催日の 0:00）までの残り。始まった後と終わった予定は 0。
 * @property daysUntil 開催日までの暦の日数。当日は 0、終わった予定は負。
 * @property progress 登録した時刻（毎年くり返す予定は前の回の開始のほうが後ならそちら）から開始までの経過の割合（0〜1）。
 *   起点が開始より後で求められないときは null。
 * @property progressStartDate `progress` の起点の日付。
 */
data class EventCountdown(
    val occurrenceDate: LocalDate,
    val status: CountdownStatus,
    val remaining: Duration,
    val daysUntil: Long,
    val progress: Double?,
    val progressStartDate: LocalDate,
)
