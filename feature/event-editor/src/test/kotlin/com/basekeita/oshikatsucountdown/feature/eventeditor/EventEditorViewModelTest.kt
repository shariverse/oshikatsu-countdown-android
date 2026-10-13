package com.basekeita.oshikatsucountdown.feature.eventeditor

import com.basekeita.oshikatsucountdown.data.definition.model.EventType
import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.model.OshiColor
import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import com.basekeita.oshikatsucountdown.data.definition.repository.EventRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.OshiRepository
import com.basekeita.oshikatsucountdown.feature.eventeditor.EventEditorViewModel.UiState
import com.basekeita.oshikatsucountdown.feature.eventeditor.EventEditorViewModel.ViewState
import com.basekeita.oshikatsucountdown.ui.uimodel.EventTypeUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.WriteFailureUiModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class EventEditorViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val now = Instant.parse("2026-10-13T03:00:00Z")
    private val clock = Clock.fixed(now, ZoneId.of("Asia/Tokyo"))

    /**
     * @param saveGate 渡すと、完了するまで保存を待たせる。
     * @param failsToSave 保存で `IOException` を投げる。
     */
    private class FakeEventRepository(
        initial: List<OshiEvent> = emptyList(),
        private val saveGate: CompletableDeferred<Unit>? = null,
        var failsToSave: Boolean = false,
    ) : EventRepository {
        override val events = MutableStateFlow(initial)

        override suspend fun save(event: OshiEvent) {
            saveGate?.await()
            if (failsToSave) throw IOException("disk full")

            events.value = events.value.filter { it.id != event.id } + event
        }

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

    private val lumina = Oshi(name = "LUMINA", color = OshiColor.PINK)
    private val minato = Oshi(name = "ミナト", color = OshiColor.BLUE)

    private fun createViewModel(
        eventRepository: EventRepository = FakeEventRepository(),
        oshiRepository: OshiRepository = FakeOshiRepository(listOf(lumina, minato)),
        eventId: String? = null,
        onboarding: Boolean = false,
    ) = EventEditorViewModel(
        eventId = eventId,
        onboarding = onboarding,
        eventRepository = eventRepository,
        oshiRepository = oshiRepository,
        clock = clock,
    )

    /** `WhileSubscribed` の状態を購読しておき、いまの UiState を読めるようにする。 */
    private fun TestScope.observe(viewModel: EventEditorViewModel): () -> UiState {
        backgroundScope.launch { viewModel.viewState.collect {} }
        runCurrent()
        return { (viewModel.viewState.value as ViewState.Success).uiState }
    }

    @Test
    fun `追加は今日と先頭の推しから始め初回の導線では最後に登録した推し`() = runTest(mainDispatcherRule.dispatcher) {
        val uiState = observe(createViewModel())
        assertEquals(true, uiState().isNew)
        assertEquals(lumina.id.toString(), uiState().draft.oshiId)
        assertEquals(LocalDate.of(2026, 10, 13), uiState().draft.date)
        assertEquals(EventTypeUiModel.LIVE, uiState().draft.type)
        assertEquals(false, uiState().canSave)

        val onboardingState = observe(createViewModel(onboarding = true))
        assertEquals(minato.id.toString(), onboardingState().draft.oshiId)
        assertEquals(true, onboardingState().onboarding)
    }

    @Test
    fun `入力して保存すると整えて保存し終えたことを出す`() = runTest(mainDispatcherRule.dispatcher) {
        val eventRepository = FakeEventRepository()
        val viewModel = createViewModel(eventRepository = eventRepository)
        val uiState = observe(viewModel)

        viewModel.onOshiSelect(minato.id.toString())
        viewModel.onTypeSelect(EventTypeUiModel.RELEASE)
        viewModel.onTitleChange("  1st写真集   発売日 ")
        viewModel.onDateChange(LocalDate.of(2026, 10, 16))
        viewModel.onStartTimeChange(LocalTime.of(10, 0))
        viewModel.onVenueChange(" 書店 ")
        runCurrent()
        assertEquals(true, uiState().canSave)

        viewModel.onSave()
        runCurrent()

        val saved = eventRepository.events.value.single()
        assertEquals(minato.id, saved.oshiId)
        assertEquals(EventType.RELEASE, saved.type)
        assertEquals("1st写真集 発売日", saved.title)
        assertEquals(LocalDate.of(2026, 10, 16), saved.date)
        assertEquals(LocalTime.of(10, 0), saved.startTime)
        assertEquals("書店", saved.venue)
        assertEquals(false, saved.pinned)
        assertEquals(now, saved.createdAt)
        assertEquals(true, uiState().isFinished)
        assertEquals(false, uiState().canSave)
    }

    @Test
    fun `タイトルが空白だけなら保存しない`() = runTest(mainDispatcherRule.dispatcher) {
        val eventRepository = FakeEventRepository()
        val viewModel = createViewModel(eventRepository = eventRepository)
        val uiState = observe(viewModel)

        viewModel.onTitleChange("  　")
        viewModel.onSave()
        runCurrent()

        assertEquals(false, uiState().canSave)
        assertEquals(emptyList<OshiEvent>(), eventRepository.events.value)
    }

    @Test
    fun `誕生日を選ぶと毎年くり返しほかの種類に戻すとくり返さない`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)

        viewModel.onTypeSelect(EventTypeUiModel.BIRTHDAY)
        runCurrent()
        assertEquals(true, uiState().draft.repeatsYearly)

        viewModel.onTypeSelect(EventTypeUiModel.LIVE)
        runCurrent()
        assertEquals(false, uiState().draft.repeatsYearly)
    }

    @Test
    fun `編集は保存済みの値から始め id と登録時刻とピン留めを引き継ぐ`() = runTest(mainDispatcherRule.dispatcher) {
        val existing = OshiEvent(
            oshiId = lumina.id,
            type = EventType.LIVE,
            title = "5th LIVE",
            date = LocalDate.of(2026, 10, 18),
            startTime = LocalTime.of(17, 0),
            venue = "東京ドーム",
            pinned = true,
            createdAt = Instant.parse("2026-09-02T00:00:00Z"),
        )
        val eventRepository = FakeEventRepository(listOf(existing))
        val viewModel = createViewModel(eventRepository = eventRepository, eventId = existing.id.toString())
        val uiState = observe(viewModel)

        assertEquals(false, uiState().isNew)
        assertEquals("5th LIVE", uiState().draft.title)
        assertEquals("東京ドーム", uiState().draft.venue)

        viewModel.onTitleChange("5th LIVE 東京公演")
        viewModel.onStartTimeChange(null)
        viewModel.onSave()
        runCurrent()

        assertEquals(
            existing.copy(title = "5th LIVE 東京公演", startTime = null),
            eventRepository.events.value.single(),
        )
    }

    @Test
    fun `選んでいた推しが消えたら推しなしにする`() = runTest(mainDispatcherRule.dispatcher) {
        val oshiRepository = FakeOshiRepository(listOf(lumina, minato))
        val viewModel = createViewModel(oshiRepository = oshiRepository)
        val uiState = observe(viewModel)

        oshiRepository.delete(lumina.id)
        runCurrent()

        assertNull(uiState().draft.oshiId)
        assertEquals(listOf("ミナト"), uiState().oshis.map { it.name })
    }

    @Test
    fun `タイトルは上限の文字数で切る`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)

        viewModel.onTitleChange("あ".repeat(OshiEvent.MAX_TITLE_LENGTH + 10))
        runCurrent()

        assertEquals(OshiEvent.MAX_TITLE_LENGTH, uiState().draft.title.length)
    }

    @Test
    fun `保存中は閉じられず保存し終えるまで待つ`() = runTest(mainDispatcherRule.dispatcher) {
        val saveGate = CompletableDeferred<Unit>()
        val viewModel = createViewModel(eventRepository = FakeEventRepository(saveGate = saveGate))
        val uiState = observe(viewModel)
        assertEquals(true, uiState().canClose)

        viewModel.onTitleChange("5th LIVE")
        viewModel.onSave()
        runCurrent()
        assertEquals(false, uiState().canClose)
        assertEquals(false, uiState().isFinished)

        saveGate.complete(Unit)
        runCurrent()
        assertEquals(true, uiState().isFinished)
    }

    @Test
    fun `保存に失敗したら知らせてもう一度保存できる`() = runTest(mainDispatcherRule.dispatcher) {
        val eventRepository = FakeEventRepository(failsToSave = true)
        val viewModel = createViewModel(eventRepository = eventRepository)
        val uiState = observe(viewModel)

        viewModel.onTitleChange("5th LIVE")
        viewModel.onSave()
        runCurrent()

        assertEquals(WriteFailureUiModel.SAVE, uiState().writeFailure)
        assertEquals(false, uiState().isFinished)
        assertEquals(true, uiState().canSave)
        assertEquals(true, uiState().canClose)

        viewModel.onWriteFailureShown()
        eventRepository.failsToSave = false
        viewModel.onSave()
        runCurrent()

        assertNull(uiState().writeFailure)
        assertEquals("5th LIVE", eventRepository.events.value.single().title)
        assertEquals(true, uiState().isFinished)
    }
}
