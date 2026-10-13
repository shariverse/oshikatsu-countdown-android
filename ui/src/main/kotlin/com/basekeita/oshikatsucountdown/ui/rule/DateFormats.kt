package com.basekeita.oshikatsucountdown.ui.rule

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthDayWeekdayFormatter = DateTimeFormatter.ofPattern("M月d日(E)", Locale.JAPAN)
private val fullDateFormatter = DateTimeFormatter.ofPattern("y年M月d日(E)", Locale.JAPAN)
private val shortDateFormatter = DateTimeFormatter.ofPattern("M/d", Locale.JAPAN)
private val dateBadgeFormatter = DateTimeFormatter.ofPattern("M.d EEE", Locale.US)
private val timeFormatter = DateTimeFormatter.ofPattern("H:mm", Locale.JAPAN)

/** 一覧の行の日付（`10月18日(日)`）。 */
fun formatMonthDayWeekday(date: LocalDate): String = date.format(monthDayWeekdayFormatter)

/** 年まで出す日付（`2026年10月18日(日)`）。入力欄に使う。 */
fun formatFullDate(date: LocalDate): String = date.format(fullDateFormatter)

/** 進み具合のバーの両端の日付（`10/18`）。 */
fun formatShortDate(date: LocalDate): String = date.format(shortDateFormatter)

/** 先頭のカードの日付（`10.18 SUN`）。曜日は英語の大文字で、数字と並べたときの見た目をそろえる。 */
fun formatDateBadge(date: LocalDate): String = date.format(dateBadgeFormatter).uppercase(Locale.US)

/** 時刻（`17:00`、`9:30`）。 */
fun formatTime(time: LocalTime): String = time.format(timeFormatter)
