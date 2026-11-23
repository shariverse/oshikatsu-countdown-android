package com.basekeita.oshikatsucountdown.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * 通知を出せるか。Android 13 以降は `POST_NOTIFICATIONS` の許可、どの版でも端末の設定でこのアプリの通知が ON。
 * 予定の通知・当日のカウントダウンを出すか（reminder）と、設定の画面の案内（settings）が同じ判定を使うので、ここに置く。
 */
fun canPostNotifications(context: Context): Boolean {
    val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    return permissionGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
}

/**
 * 正確なアラームを使えるか。Android 12 は `SCHEDULE_EXACT_ALARM` を利用者が端末の設定で切り替える。
 * Android 13 以降は `USE_EXACT_ALARM` でインストールのときに許可される。Android 11 は権限が要らない。
 */
fun canScheduleExactAlarms(context: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
