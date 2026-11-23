package com.basekeita.oshikatsucountdown.feature.reminder

import android.content.Context
import android.util.Log
import com.basekeita.oshikatsucountdown.data.definition.model.ReminderTiming
import com.basekeita.oshikatsucountdown.data.definition.repository.EventRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.OshiRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.UserSettingsRepository
import com.basekeita.oshikatsucountdown.domain.reminder.PlanRemindersUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * 予約した時刻に届いた予定の通知（アラームの extra から読んだもの）。
 *
 * @property identifier 予約の識別子（通知のタグ）。
 * @property fireAt 予約した時刻。古い予約が届いたときに、出す直前の計画と照らす。
 */
internal data class FiredReminder(
    val identifier: String,
    val eventId: UUID,
    val timing: ReminderTiming,
    val occurrenceDate: LocalDate,
    val fireAt: Instant,
)

/**
 * 届いた予定の通知を出す。出す直前の計画に同じ通知があるときだけ出し（`isReminderStillPlanned`）、
 * 文面は出すときに読み直す（予約の後に推しの名前を変えても、今の名前で出す）。
 * そのあと、空いた枠を埋めるために予定の通知と当日のカウントダウンを置き直す
 * （当日のカウントダウンは 8 日先までしか予約しないので、7 日前・前日の通知のときに、アプリを開かなくても先の予約を足す）。
 */
@Singleton
class ReminderPoster @Inject internal constructor(
    @param:ApplicationContext private val context: Context,
    private val eventRepository: EventRepository,
    private val oshiRepository: OshiRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val planReminders: PlanRemindersUseCase,
    private val clockProvider: Provider<Clock>,
    private val reminderSync: ReminderSync,
) {
    internal suspend fun post(fired: FiredReminder) {
        try {
            // 読めなければ（IOException）出さずに終える
            val settings = userSettingsRepository.readReminderSettings()
            val event = eventRepository.readEvents().firstOrNull { it.id == fired.eventId }
            // 端末のタイムゾーンを読み直すため、呼ぶたびに作る
            val zone = clockProvider.get().zone
            if (event != null && isReminderStillPlanned(planReminders, event, settings, fired.identifier, fired.fireAt, zone)) {
                val oshiName = event.oshiId?.let { oshiId -> oshiRepository.readOshis().firstOrNull { it.id == oshiId }?.name }
                val content = reminderContentOf(fired.timing, fired.occurrenceDate, event, oshiName, ResourceReminderWording(context))
                postReminderNotification(context, fired.identifier, fired.eventId, content)
            }
            reminderSync.refreshAll()
        } catch (exception: IOException) {
            Log.w(TAG, "通知を出せませんでした", exception)
        }
    }

    private companion object {
        const val TAG = "ReminderPoster"
    }
}
