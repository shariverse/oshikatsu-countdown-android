package com.basekeita.oshikatsucountdown

/**
 * 予定を保存した直後に通知の許可を求めるか。
 * 許可のダイアログを出せる版（Android 13 以降）で、まだ許可されておらず、まだ一度も求めておらず、いま求めている途中でもないときだけ求める。
 * 設定の「予定を通知する」が OFF でも求める（当日のカウントダウンは「予定を通知する」とは別で、Android では同じ通知の許可が要るため）。
 *
 * @param canRequest 許可のダイアログを出せる版（Android 13 以降）。
 * @param granted もう許可されている。
 * @param requested 一度求めた印が残っている（次からは設定の画面で「設定を開く」を出す）。
 * @param requesting いま許可のダイアログを出している途中（続けて保存しても 2 回出さない）。
 */
internal fun asksNotificationPermissionOnSave(
    canRequest: Boolean,
    granted: Boolean,
    requested: Boolean,
    requesting: Boolean,
): Boolean = canRequest && !granted && !requested && !requesting
