package com.basekeita.oshikatsucountdown.domain.countdown

/** 開催日から見た今日の位置。 */
enum class CountdownStatus {
    /** 開催日はまだ先。開催日の当日でも、開始の時刻の前は詳細ではこれ。 */
    UPCOMING,

    /** 開催日の当日。 */
    TODAY,

    /** 終わった（くり返さない予定で、開催日を過ぎた）。 */
    ENDED,
}
