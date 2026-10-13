package com.basekeita.oshikatsucountdown.domain.countdown

import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import java.time.LocalDate

/**
 * `today` から見た開催日。くり返さない予定は、過ぎていても登録した日付のまま。
 * 毎年くり返す予定は、登録した日付がまだ来ていなければその日、過ぎていれば今年か来年の同じ日（今日を含む）。
 * 2/29 の予定は、平年には 2/28 に数える（`LocalDate.withYear` が月末にそろえる）。
 */
fun OshiEvent.occurrenceOn(today: LocalDate): LocalDate {
    if (!repeatsYearly || date >= today) return date

    val thisYear = date.withYear(today.year)
    return if (thisYear >= today) {
        thisYear
    } else {
        date.withYear(today.year + 1)
    }
}

/** `occurrence` の 1 回前の開催日。くり返さない予定と、初回（登録した日付）は null。 */
fun OshiEvent.previousOccurrence(occurrence: LocalDate): LocalDate? {
    if (!repeatsYearly || occurrence <= date) return null

    return date.withYear(occurrence.year - 1)
}
