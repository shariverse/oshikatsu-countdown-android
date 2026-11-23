package com.basekeita.oshikatsucountdown.local

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class UserSettingsLocalDataSourceTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val testScope = TestScope(UnconfinedTestDispatcher())
    private val dataSource by lazy { UserSettingsLocalDataSource(testDataStore(temporaryFolder.root, testScope.backgroundScope)) }

    @Test
    fun `設定は未設定ならnullで保存した値が戻る`() = testScope.runTest {
        assertNull(dataSource.showPastEvents.first())
        assertNull(dataSource.onboardingCompleted.first())

        dataSource.setShowPastEvents(true)
        dataSource.setOnboardingCompleted(true)

        assertEquals(true, dataSource.showPastEvents.first())
        assertEquals(true, dataSource.onboardingCompleted.first())
    }

    @Test
    fun `通知の設定は未設定ならnullか空で保存した値が戻る`() = testScope.runTest {
        assertNull(dataSource.remindersEnabled.first())
        assertEquals(emptyMap<String, Boolean>(), dataSource.reminderTimings.first())
        assertNull(dataSource.reminderMinutesOfDay.first())
        assertNull(dataSource.notificationPermissionRequested.first())

        dataSource.setRemindersEnabled(false)
        dataSource.setReminderTimingEnabled("dayBefore", false)
        dataSource.setReminderTimingEnabled("onTheDay", true)
        dataSource.setReminderMinutesOfDay(20 * 60 + 30)
        dataSource.setNotificationPermissionRequested(true)

        assertEquals(false, dataSource.remindersEnabled.first())
        assertEquals(mapOf("dayBefore" to false, "onTheDay" to true), dataSource.reminderTimings.first())
        assertEquals(20 * 60 + 30, dataSource.reminderMinutesOfDay.first())
        assertEquals(true, dataSource.notificationPermissionRequested.first())
    }

    @Test
    fun `通知の設定を1回読むと保存した値が戻る`() = testScope.runTest {
        assertEquals(UserSettingsLocalDataSource.StoredReminders(enabled = null, timings = emptyMap(), minutesOfDay = null), dataSource.readReminders())

        dataSource.setRemindersEnabled(false)
        dataSource.setReminderTimingEnabled("dayBefore", false)
        dataSource.setReminderMinutesOfDay(20 * 60 + 30)

        assertEquals(
            UserSettingsLocalDataSource.StoredReminders(enabled = false, timings = mapOf("dayBefore" to false), minutesOfDay = 20 * 60 + 30),
            dataSource.readReminders(),
        )
    }

    @Test
    fun `ファイルが読めなければ、流れは値が無い扱いで、1回読むほうはIOExceptionを投げる`() = testScope.runTest {
        temporaryFolder.root.resolve("test.preferences_pb").writeBytes(byteArrayOf(0x7f, 0x7f, 0x7f, 0x7f))

        assertNull(dataSource.remindersEnabled.first())
        assertThrows(IOException::class.java) { runBlocking { dataSource.readReminders() } }
    }
}
