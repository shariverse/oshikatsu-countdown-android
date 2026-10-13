package com.basekeita.oshikatsucountdown.feature.widget.glance

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import com.basekeita.oshikatsucountdown.feature.widget.WidgetConfigureActivity
import com.basekeita.oshikatsucountdown.navigation.ExternalLaunchExtras

/** アプリを開く。 */
internal fun openAppIntent(context: Context): Intent {
    val launchIntent = checkNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName))
    // 開いているアプリがあれば作り直さずに Intent を渡す
    return launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
}

/** アプリで予定の詳細を開く。 */
internal fun openEventIntent(
    context: Context,
    eventId: String,
): Intent = openAppIntent(context).putExtra(ExternalLaunchExtras.EVENT_ID, eventId)

/** アプリで予定の追加を開く。 */
internal fun addEventIntent(context: Context): Intent = openAppIntent(context).putExtra(ExternalLaunchExtras.ADD_EVENT, true)

/** ウィジェットに出す予定を選び直す。 */
internal fun configureIntent(
    context: Context,
    appWidgetId: Int,
): Intent = Intent(context, WidgetConfigureActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
