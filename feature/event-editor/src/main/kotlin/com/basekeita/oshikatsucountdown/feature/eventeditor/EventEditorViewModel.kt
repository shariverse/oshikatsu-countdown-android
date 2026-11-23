package com.basekeita.oshikatsucountdown.feature.eventeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basekeita.oshikatsucountdown.data.definition.model.EventType
import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.model.OshiColor
import com.basekeita.oshikatsucountdown.data.definition.model.OshiEvent
import com.basekeita.oshikatsucountdown.data.definition.model.firstCharacter
import com.basekeita.oshikatsucountdown.data.definition.model.takeCharacters
import com.basekeita.oshikatsucountdown.data.definition.repository.EventRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.OshiRepository
import com.basekeita.oshikatsucountdown.feature.eventeditor.uimodel.EventDraftUiModel
import com.basekeita.oshikatsucountdown.feature.eventeditor.uimodel.withExistingOshi
import com.basekeita.oshikatsucountdown.feature.eventeditor.uimodel.withType
import com.basekeita.oshikatsucountdown.ui.uimodel.EventTypeUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.WriteFailureUiModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * 予定の追加・編集。開いたときの予定（追加なら既定値）から入力を始め、保存は Repository を直接呼ぶ。
 * 保存し終えると `isFinished` を立て、画面はそれを見て閉じる（先に閉じると `viewModelScope` が止まり、書き込みが途中で切れるため）。
 */
@HiltViewModel(assistedFactory = EventEditorViewModel.Factory::class)
class EventEditorViewModel @AssistedInject constructor(
    @Assisted private val eventId: String?,
    @Assisted private val onboarding: Boolean,
    private val eventRepository: EventRepository,
    private val oshiRepository: OshiRepository,
    private val clock: Clock,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(
            eventId: String?,
            onboarding: Boolean,
        ): EventEditorViewModel
    }

    /**
     * @property isLoadFailed 開いた予定・推しを読めなかった。
     * @property isNew 追加する（編集ではない）。
     * @property onboarding 初回の導線の 2 歩目。
     * @property isSaving 保存している途中。
     * @property isFinished 保存し終えた。
     * @property writeFailure 保存に失敗した。Snackbar で知らせ終えたら `onWriteFailureShown` で戻す。
     */
    data class UiState(
        val isLoading: Boolean = true,
        val isLoadFailed: Boolean = false,
        val draft: EventDraftUiModel = EventDraftUiModel(),
        val oshis: List<OshiUiModel> = emptyList(),
        val isNew: Boolean = true,
        val onboarding: Boolean = false,
        val isSaving: Boolean = false,
        val isFinished: Boolean = false,
        val writeFailure: WriteFailureUiModel? = null,
    ) {
        /** タイトルが空でなく、保存している途中でも保存し終えてもいない。 */
        val canSave: Boolean
            get() = !isLoading && !isLoadFailed && OshiEvent.normalizeTitle(draft.title).isNotEmpty() && !isSaving && !isFinished

        /** 保存している途中でない。保存中に閉じると `viewModelScope` ごと書き込みが止まるため、閉じる操作と戻る操作を止める。 */
        val canClose: Boolean
            get() = !isSaving
    }

    /**
     * 画面が持つ状態。
     *
     * @property isLoaded 開いた予定（追加なら既定値）から入力を始めた。
     * @property original 編集する予定。id・登録した時刻・ピン留めは保存するときに引き継ぐ。追加は null。
     */
    private data class ScreenState(
        val isLoaded: Boolean = false,
        val draft: EventDraftUiModel = EventDraftUiModel(),
        val original: OshiEvent? = null,
        val isSaving: Boolean = false,
        val isFinished: Boolean = false,
        val writeFailure: WriteFailureUiModel? = null,
    )

    private val screenState = MutableStateFlow(ScreenState())

    /** 最初に購読されたときに 1 回だけ、開いた予定（追加なら既定値）から入力を始める。読めなければ `.catch` で `isLoadFailed` にする。 */
    private val loadedScreenState = screenState
        .onStart {
            if (!screenState.value.isLoaded) {
                val oshis = oshiRepository.oshis.first()
                val editing = eventId?.let { id -> eventRepository.events.first().firstOrNull { it.id.toString() == id } }
                screenState.update { it.copy(isLoaded = true, original = editing, draft = editing?.toDraft() ?: newDraft(oshis)) }
            }
        }
        .filter { it.isLoaded }

    val uiState: StateFlow<UiState> =
        combine(loadedScreenState, oshiRepository.oshis) { screen, oshis ->
            UiState(
                isLoading = false,
                draft = screen.draft.withExistingOshi(oshis.map { it.id.toString() }.toSet()),
                oshis = oshis.map { it.toUiModel() },
                isNew = screen.original == null,
                onboarding = onboarding,
                isSaving = screen.isSaving,
                isFinished = screen.isFinished,
                writeFailure = screen.writeFailure,
            )
        }
            .catch { emit(UiState(isLoading = false, isLoadFailed = true, onboarding = onboarding)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun onOshiSelect(oshiId: String?) {
        updateDraft { it.copy(oshiId = oshiId) }
    }

    fun onTypeSelect(type: EventTypeUiModel) {
        updateDraft { it.withType(type) }
    }

    fun onTitleChange(title: String) {
        updateDraft { it.copy(title = title.takeCharacters(OshiEvent.MAX_TITLE_LENGTH)) }
    }

    fun onDateChange(date: LocalDate) {
        updateDraft { it.copy(date = date) }
    }

    /** 開始の時刻。null で時刻なし。 */
    fun onStartTimeChange(startTime: LocalTime?) {
        updateDraft { it.copy(startTime = startTime) }
    }

    fun onVenueChange(venue: String) {
        updateDraft { it.copy(venue = venue.takeCharacters(OshiEvent.MAX_VENUE_LENGTH)) }
    }

    fun onRepeatsYearlyChange(repeatsYearly: Boolean) {
        updateDraft { it.copy(repeatsYearly = repeatsYearly) }
    }

    /** 保存してよいかは、公開の状態（`combine` を通って遅れて届く）ではなく、画面が持つ状態の今の値で決める。 */
    fun onSave() {
        val screen = screenState.value
        if (!screen.isLoaded || screen.isSaving || screen.isFinished) return

        if (OshiEvent.normalizeTitle(screen.draft.title).isEmpty()) return

        screenState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                // 選んでいた推しが入力の途中で消えていたら、推しなしで保存する
                val oshiIds = oshiRepository.oshis.first().map { it.id.toString() }.toSet()
                eventRepository.save(screen.draft.withExistingOshi(oshiIds).toEvent(screen.original))
                screenState.update { it.copy(isSaving = false, isFinished = true) }
            } catch (exception: IOException) {
                screenState.update { it.copy(isSaving = false, writeFailure = WriteFailureUiModel.SAVE) }
            }
        }
    }

    fun onWriteFailureShown() {
        screenState.update { it.copy(writeFailure = null) }
    }

    private fun updateDraft(change: (EventDraftUiModel) -> EventDraftUiModel) {
        screenState.update { it.copy(draft = change(it.draft)) }
    }

    /** 保存する予定。編集なら id・登録した時刻・ピン留めを引き継ぐ。 */
    private fun EventDraftUiModel.toEvent(original: OshiEvent?): OshiEvent = OshiEvent(
        id = original?.id ?: UUID.randomUUID(),
        oshiId = oshiId?.let(UUID::fromString),
        type = type.toModel(),
        title = OshiEvent.normalizeTitle(title),
        date = date,
        startTime = startTime,
        venue = OshiEvent.normalizeVenue(venue),
        repeatsYearly = repeatsYearly,
        pinned = original?.pinned ?: false,
        createdAt = original?.createdAt ?: clock.instant(),
    )

    /** 追加するときの既定値。初回の導線では登録したばかりの推し（いちばん後ろ）、それ以外は先頭の推しを選んでおく。 */
    private fun newDraft(oshis: List<Oshi>): EventDraftUiModel {
        val defaultOshi = if (onboarding) {
            oshis.lastOrNull()
        } else {
            oshis.firstOrNull()
        }
        return EventDraftUiModel(
            oshiId = defaultOshi?.id?.toString(),
            type = EventTypeUiModel.LIVE,
            title = "",
            date = LocalDate.now(clock),
            startTime = null,
            venue = "",
            repeatsYearly = false,
        )
    }
}

private fun OshiEvent.toDraft(): EventDraftUiModel = EventDraftUiModel(
    oshiId = oshiId?.toString(),
    type = type.toUiModel(),
    title = title,
    date = date,
    startTime = startTime,
    venue = venue,
    repeatsYearly = repeatsYearly,
)

private fun Oshi.toUiModel(): OshiUiModel = OshiUiModel(
    id = id.toString(),
    name = name,
    initial = name.firstCharacter(),
    color = color.toUiModel(),
    photoPath = photoPath,
)

private fun OshiColor.toUiModel(): OshiColorUiModel = when (this) {
    OshiColor.PINK -> OshiColorUiModel.PINK
    OshiColor.RED -> OshiColorUiModel.RED
    OshiColor.ORANGE -> OshiColorUiModel.ORANGE
    OshiColor.YELLOW -> OshiColorUiModel.YELLOW
    OshiColor.GREEN -> OshiColorUiModel.GREEN
    OshiColor.MINT -> OshiColorUiModel.MINT
    OshiColor.BLUE -> OshiColorUiModel.BLUE
    OshiColor.NAVY -> OshiColorUiModel.NAVY
    OshiColor.PURPLE -> OshiColorUiModel.PURPLE
    OshiColor.LAVENDER -> OshiColorUiModel.LAVENDER
    OshiColor.BROWN -> OshiColorUiModel.BROWN
    OshiColor.BLACK -> OshiColorUiModel.BLACK
}

private fun EventType.toUiModel(): EventTypeUiModel = when (this) {
    EventType.LIVE -> EventTypeUiModel.LIVE
    EventType.BIRTHDAY -> EventTypeUiModel.BIRTHDAY
    EventType.RELEASE -> EventTypeUiModel.RELEASE
    EventType.STREAM -> EventTypeUiModel.STREAM
    EventType.OTHER -> EventTypeUiModel.OTHER
}

private fun EventTypeUiModel.toModel(): EventType = when (this) {
    EventTypeUiModel.LIVE -> EventType.LIVE
    EventTypeUiModel.BIRTHDAY -> EventType.BIRTHDAY
    EventTypeUiModel.RELEASE -> EventType.RELEASE
    EventTypeUiModel.STREAM -> EventType.STREAM
    EventTypeUiModel.OTHER -> EventType.OTHER
}
