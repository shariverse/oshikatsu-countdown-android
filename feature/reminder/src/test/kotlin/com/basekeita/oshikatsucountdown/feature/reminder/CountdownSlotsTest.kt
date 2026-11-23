package com.basekeita.oshikatsucountdown.feature.reminder

import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.util.UUID

/** 当日のカウントダウンの、標準の通知の枠への置き方（「開演まで」と残りの時計を組にする）。 */
class CountdownSlotsTest {
    private val start = Instant.parse("2026-10-18T08:00:00Z")

    private val content = LiveCountdownContent(
        eventId = UUID.randomUUID(),
        heading = "テイルズ",
        title = "ライブ",
        color = OshiColorUiModel.PINK,
        start = start,
        end = start.plus(Duration.ofHours(1)),
        untilText = "開演まで",
        startText = "開演 17:00",
        startedText = "今日は当日！",
    )

    @Test
    fun `開演の前は、時計のすぐ前のサブテキストに残りの見出しを置き、時計で数える`() {
        val slots = content.slotsAt(start.minus(Duration.ofHours(2)))

        assertEquals(CountdownSlots(subText = "開演まで", countsDown = true, timeoutMillis = Duration.ofHours(3).toMillis()), slots)
    }

    @Test
    fun `開演ちょうどからは、サブテキストを今日は当日！にして時計を消す`() {
        val slots = content.slotsAt(start)

        assertEquals(CountdownSlots(subText = "今日は当日！", countsDown = false, timeoutMillis = Duration.ofHours(1).toMillis()), slots)
    }

    @Test
    fun `開演の1時間後を過ぎていたら、すぐ消えるよう時間切れを0にする`() {
        val slots = content.slotsAt(start.plus(Duration.ofHours(2)))

        assertEquals(0L, slots.timeoutMillis)
    }
}
