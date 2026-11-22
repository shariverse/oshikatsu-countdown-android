package com.basekeita.oshikatsucountdown.feature.reminder

import com.basekeita.oshikatsucountdown.domain.reminder.LiveCountdownPlan
import com.basekeita.oshikatsucountdown.domain.reminder.LiveCountdownWindow
import java.time.Instant

/** 当日のカウントダウンのアラームを入れる時点。 */
internal enum class CountdownMoment {
    /** 出し始める（開演の 3 時間前）。 */
    BEGIN,

    /** 開演。残りの代わりに「今日は当日！」を出す。 */
    START,

    /** 終える（開演の 1 時間後）。 */
    END,
}

/** アラームのキー（時間帯の識別子と時点）。記録とアラームの見分けに使う。 */
internal fun countdownAlarmKey(
    identifier: String,
    moment: CountdownMoment,
): String = "$identifier#${moment.name}"

/**
 * 計画から、入れておくアラーム（キーと時点）を決める。
 *
 * - これから時間帯が始まるもの: 出し始める時刻
 * - 出しているもの: 開演（まだ来ていなければ）と、開演の 1 時間後
 */
internal fun countdownAlarmsOf(
    plan: LiveCountdownPlan,
    now: Instant,
): Map<String, Instant> = buildMap {
    plan.scheduled.forEach { window -> put(countdownAlarmKey(window.identifier, CountdownMoment.BEGIN), window.begin) }
    plan.current.forEach { window ->
        if (window.start > now) put(countdownAlarmKey(window.identifier, CountdownMoment.START), window.start)
        put(countdownAlarmKey(window.identifier, CountdownMoment.END), window.end)
    }
}

/**
 * 知らせて出す（音・バイブ・バナー）カウントダウン。出し始める時刻のアラームを入れてあり、まだ出していないまま時間帯に入ったもの。
 *
 * どのきっかけで置き直しても 1 回だけ知らせる（アラームでプロセスが起きたとき、プロセスの開始の置き直しがアラームより先に通っても鳴る）。
 * 予約していなかったもの（時間帯に入ってから作った予定、アプリを開いて初めて出すもの）は知らせずに出す
 * （iOS 版も、知らせるのは予約したライブアクティビティが始まるときだけ）。
 *
 * @param previousAlarmKeys 前の置き直しで入れたアラームのキー。
 * @param shown 前の置き直しで出していたカウントダウンの識別子。
 */
internal fun countdownsToAlert(
    current: List<LiveCountdownWindow>,
    previousAlarmKeys: Set<String>,
    shown: Set<String>,
): Set<String> = current
    .map { it.identifier }
    .filter { identifier -> countdownAlarmKey(identifier, CountdownMoment.BEGIN) in previousAlarmKeys && identifier !in shown }
    .toSet()
