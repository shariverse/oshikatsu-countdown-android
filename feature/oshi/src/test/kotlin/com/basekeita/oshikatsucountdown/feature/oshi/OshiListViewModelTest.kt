package com.basekeita.oshikatsucountdown.feature.oshi

import com.basekeita.oshikatsucountdown.data.definition.model.EventType
import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.model.OshiColor
import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import com.basekeita.oshikatsucountdown.feature.oshi.list.OshiListViewModel
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class OshiListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `登録した順に予定の数を添えて出す`() = runTest(mainDispatcherRule.dispatcher) {
        val lumina = Oshi(name = "LUMINA", color = OshiColor.PINK)
        val minato = Oshi(name = "ミナト", color = OshiColor.BLUE, photoPath = "/photos/minato.jpg")
        val event = OshiEvent(
            oshiId = lumina.id,
            type = EventType.LIVE,
            title = "5th LIVE",
            date = LocalDate.of(2026, 10, 18),
            createdAt = Instant.EPOCH,
        )
        val events = listOf(event, event.copy(id = UUID.randomUUID()), event.copy(id = UUID.randomUUID(), oshiId = null))
        val viewModel = OshiListViewModel(FakeOshiRepository(listOf(lumina, minato)), FakeEventRepository(events))
        backgroundScope.launch { viewModel.viewState.collect {} }
        runCurrent()

        val items = (viewModel.viewState.value as OshiListViewModel.ViewState.Success).uiState.oshis
        assertEquals(listOf("LUMINA", "ミナト"), items.map { it.oshi.name })
        assertEquals(listOf(2, 0), items.map { it.eventCount })
        assertEquals(listOf(OshiColorUiModel.PINK, OshiColorUiModel.BLUE), items.map { it.oshi.color })
        assertEquals("/photos/minato.jpg", items[1].oshi.photoPath)
        assertEquals("ミ", items[1].oshi.initial)
    }
}
