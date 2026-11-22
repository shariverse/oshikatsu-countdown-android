package com.basekeita.oshikatsucountdown.feature.reminder

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.basekeita.oshikatsucountdown.navigation.ExternalLaunchExtras
import com.basekeita.oshikatsucountdown.ui.canPostNotifications
import com.basekeita.oshikatsucountdown.ui.canScheduleExactAlarms
import java.util.UUID

private const val REMINDER_CHANNEL_ID = "event_reminders"
internal const val COUNTDOWN_CHANNEL_ID = "live_countdown"

/** 通知はタグ（予約の識別子）で見分けるので、id は 1 つに固定する。 */
private const val NOTIFICATION_ID = 1

/** 同じ予定の通知をまとめるまとめ役（グループの要約）の通知のタグの前置き。 */
private const val REMINDER_GROUP_SUMMARY_TAG_PREFIX = "reminder-group."

/** 押したときに開く Intent の data の scheme（通知ごとに別の PendingIntent にする）。 */
private const val OPEN_EVENT_URI_SCHEME = "oshikatsu-notification"

/**
 * 予定の通知と当日のカウントダウンを出せるか（SPEC の「通知が許可されている」）。次の両方がそろっていること。
 *
 * - 通知を出せる（`canPostNotifications`）
 * - 決めた時刻ちょうどに出せる（`canScheduleExactAlarms`）
 *
 * 出せなければ予約を残さない。
 */
fun areReminderNotificationsAllowed(context: Context): Boolean = canPostNotifications(context) && canScheduleExactAlarms(context)

/**
 * 予定の通知と当日のカウントダウンのチャンネルを作る。アプリのプロセスが始まるたびに呼ぶ（`ReminderSync.start`）。
 * 最初の通知を出す前から端末の設定に並べ、利用者が前もって切り替えられるようにする（当日のカウントダウンは、このチャンネルで切り替える）。
 * 同じ id で何度作っても、利用者が変えた設定は残る（名前と説明だけ、今の言語のものに変わる）。
 */
internal fun createNotificationChannels(context: Context) {
    NotificationManagerCompat.from(context).createNotificationChannelsCompat(
        listOf(reminderChannel(context), countdownChannel(context)),
    )
}

/**
 * 予定の通知のチャンネル。iOS 版と同じく、バナー（ヘッズアップ）と音とバイブで知らせる（音とバイブは端末のマナーモードなどの設定に従う）。
 * そのため重要度は HIGH（DEFAULT はバナーを出さない）にし、バイブを有効にする（チャンネルのバイブは既定で OFF）。音は既定で端末の通知音。
 * 重要度・バイブはチャンネルを作った後に変えられない（重要度は下げることだけできる）。
 * iOS 版は通知でバッジを使わない（許可は alert と sound だけ）ので、アプリのアイコンにドット（バッジ）は付けない（チャンネルの既定は付ける）。
 */
private fun reminderChannel(context: Context): NotificationChannelCompat = NotificationChannelCompat.Builder(REMINDER_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
    .setName(context.getString(R.string.reminder_channel_name))
    .setDescription(context.getString(R.string.reminder_channel_description))
    .setVibrationEnabled(true)
    .setShowBadge(false)
    .build()

/**
 * 当日のカウントダウンのチャンネル。設定の「予定を通知する」とは別で、利用者は端末の設定でこのチャンネルを切り替える
 * （iOS 版は端末の設定でライブアクティビティを切り替える）。始めるときに 1 回、iOS 版の予約したライブアクティビティの知らせと同じく
 * バナー（ヘッズアップ）と音とバイブで知らせるので、重要度は HIGH にしてバイブを有効にする。置き直しでは鳴らさない（`setSilent`）。
 * 出し続ける通知なので、アプリのアイコンにドット（バッジ）は付けない（iOS 版もバッジを使わない）。
 */
private fun countdownChannel(context: Context): NotificationChannelCompat = NotificationChannelCompat.Builder(COUNTDOWN_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
    .setName(context.getString(R.string.countdown_channel_name))
    .setDescription(context.getString(R.string.countdown_channel_description))
    .setVibrationEnabled(true)
    .setShowBadge(false)
    .build()

/**
 * 予定の通知を出す。押すとアプリで予定の詳細を開く（ウィジェットと同じく `ExternalLaunchExtras` で渡す）。
 * アプリを開いている間も、ふだんどおり出す。本文（`あと 30 日 · 10月18日(日) · 開演 17:00`）は 1 行で切れることがあるので、
 * 広げたときに全文を出す（`BigTextStyle`）。
 *
 * iOS 版（`threadIdentifier` に予定の id）と同じく、同じ予定の通知をまとめる。予定の id をグループのキーにし、
 * 束ねて見せるまとめ役の通知（`setGroupSummary`）を予定ごとに 1 つ出す。まとめ役は鳴らさない（`GROUP_ALERT_CHILDREN`）。
 */
internal fun postReminderNotification(
    context: Context,
    identifier: String,
    eventId: UUID,
    content: ReminderContent,
) {
    if (!areReminderNotificationsAllowed(context)) return

    val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(content.title)
        .setSubText(content.subtitle)
        .setContentText(content.body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setContentIntent(openEventPendingIntent(context, identifier, eventId))
        .setAutoCancel(true)
        .setGroup(reminderGroupKey(eventId))
        .build()
    notifyWithTag(context, identifier, notification)
    postReminderGroupSummary(context, eventId, content)
}

/** 同じ予定の通知のグループのキー（iOS 版の `threadIdentifier` と同じく予定の id）。 */
private fun reminderGroupKey(eventId: UUID): String = eventId.toString()

/** 同じ予定の通知をまとめるまとめ役の通知。予定の名前と推しの名前を出し、押すと予定の詳細を開く。 */
private fun postReminderGroupSummary(
    context: Context,
    eventId: UUID,
    content: ReminderContent,
) {
    val tag = REMINDER_GROUP_SUMMARY_TAG_PREFIX + eventId
    val summary = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(content.title)
        .setSubText(content.subtitle)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setContentIntent(openEventPendingIntent(context, tag, eventId))
        .setAutoCancel(true)
        .setGroup(reminderGroupKey(eventId))
        .setGroupSummary(true)
        .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
        .build()
    notifyWithTag(context, tag, summary)
}

/** 通知をタグで出す（同じタグなら置き換わる）。 */
internal fun notifyWithTag(
    context: Context,
    tag: String,
    notification: Notification,
) {
    try {
        NotificationManagerCompat.from(context).notify(tag, NOTIFICATION_ID, notification)
    } catch (exception: SecurityException) {
        // 確かめた直後に許可を取り消された
    }
}

/** タグで出した通知を消す。 */
internal fun cancelWithTag(
    context: Context,
    tag: String,
) {
    NotificationManagerCompat.from(context).cancel(tag, NOTIFICATION_ID)
}

/** 押すとアプリで予定の詳細を開く。 */
internal fun openEventPendingIntent(
    context: Context,
    identifier: String,
    eventId: UUID,
): PendingIntent {
    val launchIntent = checkNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName))
        // 開いているアプリがあれば作り直さずに Intent を渡す
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        // 識別子を data に入れ、通知ごとに別の PendingIntent にする（extra は同じさの判定に入らず、同じだと後の通知の extra に置き換わる）
        .setData(Uri.fromParts(OPEN_EVENT_URI_SCHEME, identifier, null))
        .putExtra(ExternalLaunchExtras.EVENT_ID, eventId.toString())
    return PendingIntent.getActivity(
        context,
        0,
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
