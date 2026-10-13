package com.basekeita.oshikatsucountdown.feature.oshi

import com.basekeita.oshikatsucountdown.data.definition.model.EventType
import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.model.OshiColor
import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import com.basekeita.oshikatsucountdown.domain.oshi.DeleteOshiUseCase
import com.basekeita.oshikatsucountdown.domain.oshi.SaveOshiUseCase
import com.basekeita.oshikatsucountdown.feature.oshi.editor.OshiEditorViewModel
import com.basekeita.oshikatsucountdown.feature.oshi.editor.OshiEditorViewModel.UiState
import com.basekeita.oshikatsucountdown.feature.oshi.editor.OshiEditorViewModel.ViewState
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.WriteFailureUiModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class OshiEditorViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val lumina = Oshi(name = "LUMINA", color = OshiColor.PURPLE, photoPath = "/photos/lumina.jpg")

    private class Fixture(
        oshis: List<Oshi> = emptyList(),
        events: List<OshiEvent> = emptyList(),
    ) {
        val oshiRepository = FakeOshiRepository(oshis)
        val eventRepository = FakeEventRepository(events)
        val photoRepository = RecordingPhotoRepository()

        fun createViewModel(
            oshiId: String? = null,
            onboarding: Boolean = false,
        ) = OshiEditorViewModel(
            oshiId = oshiId,
            onboarding = onboarding,
            oshiRepository = oshiRepository,
            eventRepository = eventRepository,
            saveOshi = SaveOshiUseCase(oshiRepository, photoRepository),
            deleteOshi = DeleteOshiUseCase(oshiRepository, eventRepository, photoRepository),
        )
    }

    /** `WhileSubscribed` の状態を購読しておき、いまの UiState を読めるようにする。 */
    private fun TestScope.observe(viewModel: OshiEditorViewModel): () -> UiState {
        backgroundScope.launch { viewModel.viewState.collect {} }
        runCurrent()
        return { (viewModel.viewState.value as ViewState.Success).uiState }
    }

    @Test
    fun `登録は空の名前と既定の推し色から始め名前を入れると保存できる`() = runTest(mainDispatcherRule.dispatcher) {
        val fixture = Fixture()
        val viewModel = fixture.createViewModel(onboarding = true)
        val uiState = observe(viewModel)
        assertEquals("", uiState().name)
        assertEquals(OshiColorUiModel.PINK, uiState().color)
        assertEquals(false, uiState().canSave)
        assertEquals(true, uiState().onboarding)

        viewModel.onNameChange(" ミナト ")
        viewModel.onColorSelect(OshiColorUiModel.BLUE)
        viewModel.onPhotoPick("content://picked/42")
        runCurrent()
        assertEquals("content://picked/42", uiState().photoSource)
        assertEquals(true, uiState().canSave)

        viewModel.onSave()
        runCurrent()

        val saved = fixture.oshiRepository.oshis.value.single()
        assertEquals("ミナト", saved.name)
        assertEquals(OshiColor.BLUE, saved.color)
        assertEquals("/photos/42.jpg", saved.photoPath)
        assertEquals(true, uiState().isFinished)
    }

    @Test
    fun `編集は保存済みの値から始め写真を外して保存すると前の写真を消す`() = runTest(mainDispatcherRule.dispatcher) {
        val fixture = Fixture(oshis = listOf(lumina))
        val viewModel = fixture.createViewModel(oshiId = lumina.id.toString())
        val uiState = observe(viewModel)
        assertEquals("LUMINA", uiState().name)
        assertEquals(OshiColorUiModel.PURPLE, uiState().color)
        assertEquals("/photos/lumina.jpg", uiState().photoSource)
        assertEquals(false, uiState().isNew)

        viewModel.onPhotoRemove()
        runCurrent()
        assertNull(uiState().photoSource)

        viewModel.onSave()
        runCurrent()

        assertEquals(lumina.copy(photoPath = null), fixture.oshiRepository.oshis.value.single())
        assertEquals(listOf("/photos/lumina.jpg"), fixture.photoRepository.deletedPaths)
    }

    @Test
    fun `削除すると予定と写真も消し終えたことを出す`() = runTest(mainDispatcherRule.dispatcher) {
        val event = OshiEvent(
            oshiId = lumina.id,
            type = EventType.LIVE,
            title = "5th LIVE",
            date = LocalDate.of(2026, 10, 18),
            createdAt = Instant.EPOCH,
        )
        val fixture = Fixture(oshis = listOf(lumina), events = listOf(event))
        val viewModel = fixture.createViewModel(oshiId = lumina.id.toString())
        val uiState = observe(viewModel)
        assertEquals(1, uiState().eventCount)

        viewModel.onDelete()
        runCurrent()

        assertEquals(emptyList<Oshi>(), fixture.oshiRepository.oshis.value)
        assertEquals(emptyList<OshiEvent>(), fixture.eventRepository.events.value)
        assertEquals(listOf("/photos/lumina.jpg"), fixture.photoRepository.deletedPaths)
        assertEquals(true, uiState().isFinished)
    }

    @Test
    fun `名前は上限の文字数で切る`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = Fixture().createViewModel()
        val uiState = observe(viewModel)

        viewModel.onNameChange("あ".repeat(Oshi.MAX_NAME_LENGTH + 5))
        runCurrent()

        assertEquals(Oshi.MAX_NAME_LENGTH, uiState().name.length)
    }

    @Test
    fun `保存に失敗したら知らせて取り込んだ写真を消しもう一度保存できる`() = runTest(mainDispatcherRule.dispatcher) {
        val fixture = Fixture(oshis = listOf(lumina))
        fixture.oshiRepository.failsToWrite = true
        val viewModel = fixture.createViewModel(oshiId = lumina.id.toString())
        val uiState = observe(viewModel)

        viewModel.onPhotoPick("content://picked/2")
        viewModel.onSave()
        runCurrent()

        assertEquals(WriteFailureUiModel.SAVE, uiState().writeFailure)
        assertEquals(false, uiState().isFinished)
        assertEquals(true, uiState().canSave)
        assertEquals(true, uiState().canClose)
        assertEquals(listOf("/photos/2.jpg"), fixture.photoRepository.deletedPaths)

        viewModel.onWriteFailureShown()
        runCurrent()
        assertNull(uiState().writeFailure)
    }

    @Test
    fun `削除に失敗したら知らせて閉じない`() = runTest(mainDispatcherRule.dispatcher) {
        val fixture = Fixture(oshis = listOf(lumina))
        fixture.oshiRepository.failsToWrite = true
        val viewModel = fixture.createViewModel(oshiId = lumina.id.toString())
        val uiState = observe(viewModel)

        viewModel.onDelete()
        runCurrent()

        assertEquals(WriteFailureUiModel.DELETE, uiState().writeFailure)
        assertEquals(false, uiState().isFinished)
        assertEquals(true, uiState().canClose)
    }
}
