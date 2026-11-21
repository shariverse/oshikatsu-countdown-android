package com.basekeita.oshikatsucountdown.local

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

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
}
