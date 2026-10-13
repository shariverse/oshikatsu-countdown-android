package com.basekeita.oshikatsucountdown.feature.widget

import com.basekeita.oshikatsucountdown.data.definition.model.EventType
import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.model.OshiColor
import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import com.basekeita.oshikatsucountdown.data.definition.repository.EventRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.OshiRepository
import com.basekeita.oshikatsucountdown.domain.countdown.BuildEventScheduleUseCase
import com.basekeita.oshikatsucountdown.feature.widget.WidgetConfigureViewModel.UiState
import com.basekeita.oshikatsucountdown.feature.widget.WidgetConfigureViewModel.ViewState
import com.basekeita.oshikatsucountdown.ui.uimodel.WriteFailureUiModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetConfigureViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zone = ZoneId.of("Asia/Tokyo")

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

        override suspend fun delete(id: UUID) = Unit
    }

    /** メモリに持つ選択。`saveGate` を渡すと、それが完了するまで保存を終えない。`failsToSave` なら保存で `IOException` を投げる。 */
    private class FakeWidgetEventSelectionStore(
        initial: Map<Int, String> = emptyMap(),
        private val saveGate: CompletableDeferred<Unit>? = null,
        var failsToSave: Boolean = false,
    ) : WidgetEventSelectionStore {
        val selections = initial.toMutableMap()
        var saveCount = 0

        override suspend fun load(appWidgetId: Int): String? = selections[appWidgetId]

        override suspend fun save(
            appWidgetId: Int,
            eventId: String,
        ) {
            saveCount++
            saveGate?.await()
            if (failsToSave) throw IOException("disk full")

            selections[appWidgetId] = eventId
        }
    }

    private val lumina = Oshi(name = "LUMINA", color = OshiColor.PINK)

    private fun event(
        title: String,
        date: LocalDate,
    ) = OshiEvent(oshiId = lumina.id, type = EventType.LIVE, title = title, date = date, createdAt = Instant.EPOCH)

    private val live = event("5th LIVE", LocalDate.of(2026, 10, 18))
    private val release = event("写真集", LocalDate.of(2026, 10, 10))
    private val ended = event("FCイベント", LocalDate.of(2026, 9, 1))

    private val clock = Clock.fixed(LocalDateTime.parse("2026-10-07T12:00:00").atZone(zone).toInstant(), zone)

    private val appWidgetId = 42

    private fun createViewModel(store: WidgetEventSelectionStore) = WidgetConfigureViewModel(
        appWidgetId = appWidgetId,
        eventRepository = FakeEventRepository(listOf(live, release, ended)),
        oshiRepository = FakeOshiRepository(listOf(lumina)),
        buildEventSchedule = BuildEventScheduleUseCase(),
        selectionStore = store,
        clock = clock,
    )

    /** `WhileSubscribed` の状態を購読しておき、いまの UiState を読めるようにする。 */
    private fun TestScope.observe(viewModel: WidgetConfigureViewModel): () -> UiState {
        backgroundScope.launch { viewModel.viewState.collect {} }
        runCurrent()
        return { (viewModel.viewState.value as ViewState.Success).uiState }
    }

    @Test
    fun `これからの予定を近い順に出し終わった予定は出さない`() = runTest(mainDispatcherRule.dispatcher) {
        val uiState = observe(createViewModel(FakeWidgetEventSelectionStore()))

        assertEquals(listOf("写真集", "5th LIVE"), uiState().events.map { it.title })
        assertNull(uiState().selectedEventId)
        assertFalse(uiState().isFinished)
    }

    @Test
    fun `選び直すときは今出している予定を選んである`() = runTest(mainDispatcherRule.dispatcher) {
        val store = FakeWidgetEventSelectionStore(initial = mapOf(appWidgetId to live.id.toString()))
        val uiState = observe(createViewModel(store))

        assertEquals(live.id.toString(), uiState().selectedEventId)
    }

    @Test
    fun `選ぶと保存し終えてから閉じる`() = runTest(mainDispatcherRule.dispatcher) {
        val saveGate = CompletableDeferred<Unit>()
        val store = FakeWidgetEventSelectionStore(saveGate = saveGate)
        val viewModel = createViewModel(store)
        val uiState = observe(viewModel)

        viewModel.onEventSelect(release.id.toString())
        runCurrent()
        assertFalse(uiState().isFinished)
        assertFalse(uiState().canClose)

        saveGate.complete(Unit)
        runCurrent()
        assertTrue(uiState().isFinished)
        assertEquals(release.id.toString(), store.selections[appWidgetId])
    }

    @Test
    fun `続けて押しても1回だけ保存する`() = runTest(mainDispatcherRule.dispatcher) {
        val saveGate = CompletableDeferred<Unit>()
        val store = FakeWidgetEventSelectionStore(saveGate = saveGate)
        val viewModel = createViewModel(store)
        observe(viewModel)

        viewModel.onEventSelect(release.id.toString())
        viewModel.onEventSelect(live.id.toString())
        saveGate.complete(Unit)
        runCurrent()

        assertEquals(1, store.saveCount)
        assertEquals(release.id.toString(), store.selections[appWidgetId])
    }

    @Test
    fun `保存に失敗したら知らせて選び直せる`() = runTest(mainDispatcherRule.dispatcher) {
        val store = FakeWidgetEventSelectionStore(failsToSave = true)
        val viewModel = createViewModel(store)
        val uiState = observe(viewModel)

        viewModel.onEventSelect(release.id.toString())
        runCurrent()

        assertEquals(WriteFailureUiModel.SAVE, uiState().writeFailure)
        assertFalse(uiState().isFinished)
        assertTrue(uiState().canClose)

        viewModel.onWriteFailureShown()
        store.failsToSave = false
        viewModel.onEventSelect(release.id.toString())
        runCurrent()

        assertNull(uiState().writeFailure)
        assertTrue(uiState().isFinished)
        assertEquals(release.id.toString(), store.selections[appWidgetId])
    }
}
