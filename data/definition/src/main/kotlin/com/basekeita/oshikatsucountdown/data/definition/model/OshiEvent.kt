package com.basekeita.oshikatsucountdown.data.definition.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * カウントダウンする予定。日付と時刻は端末のタイムゾーンでの値として持つ（遠征先で時差があっても、書いた日時のまま数える）。
 *
 * @property oshiId 結び付く推し。推しを登録せずに作った予定は null。
 * @property startTime 開演・開始の時刻。決まっていなければ null で、その日の 0:00 までを数える。
 * @property repeatsYearly 毎年くり返す（誕生日・記念日）。過ぎたら翌年の同じ日を数える。
 * @property pinned ホームの先頭に大きく出す。ピン留めできる予定は 1 つだけ。
 * @property createdAt 登録した時刻。詳細の「経過」の起点に使う。
 */
data class OshiEvent(
    val id: UUID = UUID.randomUUID(),
    val oshiId: UUID?,
    val type: EventType,
    val title: String,
    val date: LocalDate,
    val startTime: LocalTime? = null,
    val venue: String = "",
    val repeatsYearly: Boolean = false,
    val pinned: Boolean = false,
    val createdAt: Instant,
) {
    companion object {
        /** タイトルの最大文字数（正規化後）。 */
        const val MAX_TITLE_LENGTH = 40

        /** 会場・場所の最大文字数（正規化後）。 */
        const val MAX_VENUE_LENGTH = 40

        fun normalizeTitle(raw: String): String = raw.collapseWhitespace().takeCharacters(MAX_TITLE_LENGTH)

        fun normalizeVenue(raw: String): String = raw.collapseWhitespace().takeCharacters(MAX_VENUE_LENGTH)
    }
}
