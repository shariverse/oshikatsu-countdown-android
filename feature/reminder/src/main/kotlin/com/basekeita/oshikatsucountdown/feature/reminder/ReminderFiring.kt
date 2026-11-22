package com.basekeita.oshikatsucountdown.feature.reminder

import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import com.basekeita.oshikatsucountdown.data.definition.model.ReminderSettings
import com.basekeita.oshikatsucountdown.domain.reminder.PlanRemindersUseCase
import java.time.Instant
import java.time.ZoneId

/**
 * 予約した時刻に届いた通知を、まだ出すか。出す直前の時点の計画に、同じ識別子・同じ時刻の通知があるときだけ出す。
 *
 * 予約し直しが間に合わなかったもの（設定を OFF にした・タイミングを外した・予定を消した・日付や時刻を変えた、
 * 予約の記録が壊れて取り消せなかった）を、古い文面で出さないため。
 *
 * @param event 通知の予定。消されていれば null。
 * @param fireAt 予約した時刻。
 */
internal fun isReminderStillPlanned(
    planReminders: PlanRemindersUseCase,
    event: OshiEvent?,
    settings: ReminderSettings,
    identifier: String,
    fireAt: Instant,
    zone: ZoneId,
): Boolean {
    if (event == null) return false

    // 計画は「今より後」の通知だけなので、出す時刻の直前の時点で計画する
    return planReminders(listOf(event), settings, fireAt.minusMillis(1), zone)
        .any { it.identifier == identifier && it.fireAt == fireAt }
}
