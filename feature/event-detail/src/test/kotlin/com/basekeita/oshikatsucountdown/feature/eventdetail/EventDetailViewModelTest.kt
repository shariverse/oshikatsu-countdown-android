package com.basekeita.oshikatsucountdown.feature.eventdetail

import com.basekeita.oshikatsucountdown.data.definition.model.EventType
import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.model.OshiColor
import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import com.basekeita.oshikatsucountdown.data.definition.repository.EventRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.OshiRepository
import com.basekeita.oshikatsucountdown.domain.countdown.CalculateCountdownUseCase
import com.basekeita.oshikatsucountdown.feature.eventdetail.EventDetailViewModel.UiState
import com.basekeita.oshikatsucountdown.feature.eventdetail.EventDetailViewModel.ViewState
import com.basekeita.oshikatsucountdown.feature.eventdetail.uimodel.CountdownStatusUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.EventTypeUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.WriteFailureUiModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class EventDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zone = ZoneId.of("Asia/Tokyo")

    /** テストの仮想時間とともに進む時計。 */
    private class SchedulerClock(
        private val start: Instant,
        private val zone: ZoneId,
        private val scheduler: TestCoroutineScheduler,
    ) : Clock() {
        override fun getZone(): ZoneId = zone

        override fun withZone(zone: ZoneId): Clock = SchedulerClock(start, zone, scheduler)

        override fun instant(): Instant = start.plusMillis(scheduler.currentTime)
    }

    /** メモリに持つ `EventRepository`。ピン留めは 1 件だけにする。`failsToWrite` なら削除・ピン留めで `IOException` を投げる。 */
    private class FakeEventRepository(
        initial: List<OshiEvent>,
        private val failsToWrite: Boolean = false,
    ) : EventRepository {
        override val events = MutableStateFlow(initial)

        override suspend fun save(event: OshiEvent) = Unit

        override suspend fun delete(id: UUID) {
            if (failsToWrite) throw IOException("disk full")

            events.value = events.value.filter { it.id != id }
        }

        override suspend fun deleteByOshi(oshiId: UUID) = Unit

        override suspend fun setPinned(
            id: UUID,
            pinned: Boolean,
        ) {
            if (failsToWrite) throw IOException("disk full")

            events.value = events.value.map { event ->
                if (event.id == id) {
                    event.copy(pinned = pinned)
                } else {
                    event.copy(pinned = event.pinned && !pinned)
                }
            }
        }
    }

    private class FakeOshiRepository(
        initial: List<Oshi>,
    ) : OshiRepository {
        override val oshis = MutableStateFlow(initial)

        override suspend fun save(oshi: Oshi) = Unit

        override suspend fun delete(id: UUID) = Unit
    }

    private val lumina = Oshi(name = "LUMINA", color = OshiColor.PINK, photoPath = "/photos/lumina.jpg")
    private val live = OshiEvent(
        oshiId = lumina.id,
        type = EventType.LIVE,
        title = "5th LIVE 東京公演",
        date = LocalDate.of(2026, 10, 18),
        startTime = LocalTime.of(17, 0),
        venue = "東京ドーム",
        createdAt = LocalDateTime.parse("2026-09-02T00:00:00").atZone(zone).toInstant(),
    )

    private fun TestScope.createViewModel(
        eventRepository: EventRepository = FakeEventRepository(listOf(live)),
        eventId: String = live.id.toString(),
    ) = EventDetailViewModel(
        eventId = eventId,
        eventRepository = eventRepository,
        oshiRepository = FakeOshiRepository(listOf(lumina)),
        calculateCountdown = CalculateCountdownUseCase(),
        clock = SchedulerClock(LocalDateTime.parse("2026-10-07T10:17:42").atZone(zone).toInstant(), zone, testScheduler),
    )

    /** `WhileSubscribed` の状態を購読しておき、いまの UiState を読めるようにする。 */
    private fun TestScope.observe(viewModel: EventDetailViewModel): () -> UiState {
        backgroundScope.launch { viewModel.viewState.collect {} }
        runCurrent()
        return { (viewModel.viewState.value as ViewState.Success).uiState }
    }

    @Test
    fun `予定と推しと開始までの残りを出す`() = runTest(mainDispatcherRule.dispatcher) {
        val uiState = observe(createViewModel())

        with(uiState().event) {
            assertEquals("5th LIVE 東京公演", title)
            assertEquals(EventTypeUiModel.LIVE, type)
            assertEquals("LUMINA", oshiName)
            assertEquals(OshiColorUiModel.PINK, color)
            assertEquals("/photos/lumina.jpg", photoPath)
            assertEquals("東京ドーム", venue)
        }
        with(uiState().countdown) {
            assertEquals(CountdownStatusUiModel.UPCOMING, status)
            assertEquals(listOf(11L, 6L, 42L, 18L), listOf(days, hours.toLong(), minutes.toLong(), seconds.toLong()))
            assertEquals(LocalDate.of(2026, 9, 2), progressStartDate)
        }
    }

    @Test
    fun `1秒ごとに数え直す`() = runTest(mainDispatcherRule.dispatcher) {
        val uiState = observe(createViewModel())
        assertEquals(18, uiState().countdown.seconds)

        advanceTimeBy(TICK_INTERVAL_MILLIS)
        runCurrent()
        assertEquals(17, uiState().countdown.seconds)

        advanceTimeBy(TICK_INTERVAL_MILLIS * 18)
        runCurrent()
        assertEquals(41, uiState().countdown.minutes)
        assertEquals(59, uiState().countdown.seconds)
    }

    @Test
    fun `ピン留めを切り替える`() = runTest(mainDispatcherRule.dispatcher) {
        val eventRepository = FakeEventRepository(listOf(live))
        val viewModel = createViewModel(eventRepository = eventRepository)
        val uiState = observe(viewModel)

        viewModel.onPinnedChange(true)
        runCurrent()
        assertEquals(true, uiState().event.pinned)

        viewModel.onPinnedChange(false)
        runCurrent()
        assertEquals(false, uiState().event.pinned)
    }

    @Test
    fun `削除し終えたらNotFoundにする`() = runTest(mainDispatcherRule.dispatcher) {
        val eventRepository = FakeEventRepository(listOf(live))
        val viewModel = createViewModel(eventRepository = eventRepository)
        observe(viewModel)

        viewModel.onDelete()
        runCurrent()

        assertEquals(emptyList<OshiEvent>(), eventRepository.events.value)
        assertEquals(ViewState.NotFound, viewModel.viewState.value)
    }

    @Test
    fun `idが読めなければNotFound`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel(eventId = "not-a-uuid")
        backgroundScope.launch { viewModel.viewState.collect {} }
        runCurrent()

        assertEquals(ViewState.NotFound, viewModel.viewState.value)
    }

    @Test
    fun `削除に失敗したら知らせて詳細に留まる`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel(eventRepository = FakeEventRepository(listOf(live), failsToWrite = true))
        val uiState = observe(viewModel)

        viewModel.onDelete()
        runCurrent()

        assertEquals(WriteFailureUiModel.DELETE, uiState().writeFailure)
        assertEquals(true, uiState().canClose)

        viewModel.onWriteFailureShown()
        runCurrent()
        assertEquals(null, uiState().writeFailure)
    }

    @Test
    fun `ピン留めに失敗したら知らせる`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel(eventRepository = FakeEventRepository(listOf(live), failsToWrite = true))
        val uiState = observe(viewModel)

        viewModel.onPinnedChange(true)
        runCurrent()

        assertEquals(WriteFailureUiModel.SAVE, uiState().writeFailure)
        assertEquals(false, uiState().event.pinned)
    }
}
