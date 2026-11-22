package com.basekeita.oshikatsucountdown.feature.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.util.Log

/**
 * 決めた時刻ちょうどに、端末の待機中（Doze）でも動くアラームを入れる。
 * 呼ぶ側は先に `canScheduleExactAlarms` を確かめる。確かめた直後に許可を取り消されたときは入れずに終える
 * （取り消されると、入れてあった正確なアラームも消える）。
 */
internal fun setExactAlarm(
    alarmManager: AlarmManager,
    triggerAtMillis: Long,
    operation: PendingIntent,
) {
    try {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
    } catch (exception: SecurityException) {
        Log.w("ExactAlarms", "正確なアラームを入れられませんでした", exception)
    }
}
