package com.basekeita.oshikatsucountdown.feature.home

import com.basekeita.oshikatsucountdown.data.definition.model.EventType
import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.model.OshiColor
import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import com.basekeita.oshikatsucountdown.data.definition.model.UserSettings
import com.basekeita.oshikatsucountdown.data.definition.repository.EventRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.OshiRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.UserSettingsRepository
import com.basekeita.oshikatsucountdown.domain.countdown.BuildEventScheduleUseCase
import com.basekeita.oshikatsucountdown.feature.home.HomeViewModel.UiState
import com.basekeita.oshikatsucountdown.feature.home.HomeViewModel.ViewState
import com.basekeita.oshikatsucountdown.feature.home.uimodel.EventStatusUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zone = ZoneId.of("Asia/Tokyo")

    /** 読むたびに `now` を返す時計。日付が変わる場面を作る。 */
    private class MutableClock(
        var now: Instant,
        private val zone: ZoneId,
    ) : Clock() {
        override fun getZone(): ZoneId = zone

        override fun withZone(zone: ZoneId): Clock = MutableClock(now, zone)

        override fun instant(): Instant = now
    }

    private class FakeEventRepository(
        initial: List<OshiEvent>,
    ) : EventRepository {
        override val events = MutableStateFlow(initial)

        override suspend fun save(event: OshiEvent) = Unit

        override suspend fun delete(id: UUID) = Unit

        override suspend fun deleteByOshi(oshiId: UUID) = Unit

        override suspend fun setPinned(
            id: UUID,
            pinned: Boolean,
        ) = Unit
    }

    private class FakeOshiRepository(
        initial: List<Oshi>,
    ) : OshiRepository {
        override val oshis = MutableStateFlow(initial)

        override suspend fun save(oshi: Oshi) = Unit

        override suspend fun delete(id: UUID) {
            oshis.value = oshis.value.filter { it.id != id }
        }
    }

    private class FakeUserSettingsRepository(
        initial: UserSettings = UserSettings(),
    ) : UserSettingsRepository {
        override val settings = MutableStateFlow(initial)

        override suspend fun setShowPastEvents(show: Boolean) {
            settings.value = settings.value.copy(showPastEvents = show)
        }

        override suspend fun setOnboardingCompleted(completed: Boolean) = Unit
    }

    private val lumina = Oshi(name = "LUMINA", color = OshiColor.PINK)
    private val minato = Oshi(name = "ミナト", color = OshiColor.BLUE, photoPath = "/photos/minato.jpg")

    private fun event(
        title: String,
        date: LocalDate,
        oshi: Oshi? = null,
        pinned: Boolean = false,
    ) = OshiEvent(
        oshiId = oshi?.id,
        type = EventType.LIVE,
        title = title,
        date = date,
        pinned = pinned,
        createdAt = Instant.EPOCH,
    )

    private val live = event("5th LIVE", LocalDate.of(2026, 10, 18), lumina, pinned = true)
    private val release = event("写真集", LocalDate.of(2026, 10, 16), minato)
    private val ended = event("FCイベント", LocalDate.of(2026, 9, 1), lumina)
    private val noOshi = event("特典会", LocalDate.of(2026, 11, 1))

    private val clock = MutableClock(LocalDateTime.parse("2026-10-13T12:00:00").atZone(zone).toInstant(), zone)

    private fun createViewModel(
        events: List<OshiEvent> = listOf(live, release, ended, noOshi),
        oshiRepository: OshiRepository = FakeOshiRepository(listOf(lumina, minato)),
        settingsRepository: UserSettingsRepository = FakeUserSettingsRepository(),
        eventRepository: EventRepository = FakeEventRepository(events),
    ) = HomeViewModel(
        eventRepository = eventRepository,
        oshiRepository = oshiRepository,
        userSettingsRepository = settingsRepository,
        buildEventSchedule = BuildEventScheduleUseCase(),
        clock = clock,
    )

    /** `WhileSubscribed` の状態を購読しておき、いまの UiState を読めるようにする。 */
    private fun TestScope.observe(viewModel: HomeViewModel): () -> UiState {
        backgroundScope.launch { viewModel.viewState.collect {} }
        runCurrent()
        return { (viewModel.viewState.value as ViewState.Success).uiState }
    }

    @Test
    fun `ピン留めした予定を先頭に出し近い順に並べる`() = runTest(mainDispatcherRule.dispatcher) {
        val uiState = observe(createViewModel())

        assertEquals("5th LIVE", uiState().featured?.title)
        assertEquals(listOf("写真集", "5th LIVE", "特典会"), uiState().upcoming.map { it.title })
        assertEquals(listOf(3L, 5L, 19L), uiState().upcoming.map { it.daysUntil })
        assertEquals(emptyList<String>(), uiState().past.map { it.title })
        assertEquals(listOf("LUMINA", "ミナト"), uiState().oshis.map { it.name })
    }

    @Test
    fun `行には推しの名前と色と写真を出し推しなしの予定はタイトルの頭文字`() = runTest(mainDispatcherRule.dispatcher) {
        val uiState = observe(createViewModel())

        val releaseRow = uiState().upcoming.first { it.title == "写真集" }
        assertEquals("ミナト", releaseRow.oshiName)
        assertEquals("ミ", releaseRow.initial)
        assertEquals(OshiColorUiModel.BLUE, releaseRow.color)
        assertEquals("/photos/minato.jpg", releaseRow.photoPath)

        val noOshiRow = uiState().upcoming.first { it.title == "特典会" }
        assertNull(noOshiRow.oshiName)
        assertEquals("特", noOshiRow.initial)
        assertEquals(OshiColorUiModel.BLACK, noOshiRow.color)
    }

    @Test
    fun `推しで絞り込み消えたらすべてに戻す`() = runTest(mainDispatcherRule.dispatcher) {
        val oshiRepository = FakeOshiRepository(listOf(lumina, minato))
        val viewModel = createViewModel(oshiRepository = oshiRepository)
        val uiState = observe(viewModel)

        viewModel.onOshiFilterSelect(minato.id.toString())
        runCurrent()
        assertEquals(minato.id.toString(), uiState().selectedOshiId)
        assertEquals(listOf("写真集"), uiState().upcoming.map { it.title })
        assertEquals("写真集", uiState().featured?.title)

        oshiRepository.delete(minato.id)
        runCurrent()
        assertNull(uiState().selectedOshiId)
        assertEquals(3, uiState().upcoming.size)
    }

    @Test
    fun `終わった予定は設定がONのときだけ出す`() = runTest(mainDispatcherRule.dispatcher) {
        val settingsRepository = FakeUserSettingsRepository()
        val uiState = observe(createViewModel(settingsRepository = settingsRepository))
        assertEquals(emptyList<String>(), uiState().past.map { it.title })

        settingsRepository.setShowPastEvents(true)
        runCurrent()

        assertEquals(listOf("FCイベント"), uiState().past.map { it.title })
        assertEquals(EventStatusUiModel.ENDED, uiState().past.single().status)
        assertEquals(-42L, uiState().past.single().daysUntil)
    }

    @Test
    fun `予定が無ければ無いことを出す`() = runTest(mainDispatcherRule.dispatcher) {
        val uiState = observe(createViewModel(events = emptyList()))

        assertEquals(false, uiState().hasAnyEvent)
        assertNull(uiState().featured)
    }

    @Test
    fun `日付が変わったら数え直す`() = runTest(mainDispatcherRule.dispatcher) {
        val uiState = observe(createViewModel())
        assertEquals(3L, uiState().upcoming.first().daysUntil)

        clock.now = LocalDateTime.parse("2026-10-14T00:00:30").atZone(zone).toInstant()
        advanceTimeBy(TODAY_CHECK_INTERVAL_MILLIS + 1)

        assertEquals(2L, uiState().upcoming.first().daysUntil)
    }

    @Test
    fun `読めなければErrorを出す`() = runTest(mainDispatcherRule.dispatcher) {
        val failingRepository = object : EventRepository {
            override val events: Flow<List<OshiEvent>> = flow { throw IllegalStateException("読めません") }

            override suspend fun save(event: OshiEvent) = Unit

            override suspend fun delete(id: UUID) = Unit

            override suspend fun deleteByOshi(oshiId: UUID) = Unit

            override suspend fun setPinned(
                id: UUID,
                pinned: Boolean,
            ) = Unit
        }
        val viewModel = createViewModel(eventRepository = failingRepository)
        backgroundScope.launch { viewModel.viewState.collect {} }
        runCurrent()

        assertEquals(ViewState.Error("読めません"), viewModel.viewState.value)
    }
}
