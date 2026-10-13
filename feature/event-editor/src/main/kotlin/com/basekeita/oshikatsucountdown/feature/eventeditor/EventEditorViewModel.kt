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
import kotlinx.coroutines.flow.filterNotNull
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
class EventEditorViewModel
@AssistedInject
constructor(
    @Assisted private val eventId: String?,
    @Assisted private val onboarding: Boolean,
    private val eventRepository: EventRepository,
    oshiRepository: OshiRepository,
    private val clock: Clock,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(
            eventId: String?,
            onboarding: Boolean,
        ): EventEditorViewModel
    }

    sealed interface ViewState {
        data object Loading : ViewState

        data class Error(
            val message: String,
        ) : ViewState

        data class Success(
            val uiState: UiState,
        ) : ViewState
    }

    /**
     * @property isNew 追加する（編集ではない）。
     * @property onboarding 初回の導線の 2 歩目。
     * @property canSave タイトルが空でなく、保存中でもない。
     * @property canClose 保存中でない。保存中に閉じると `viewModelScope` ごと書き込みが止まるため、閉じる操作と戻る操作を止める。
     * @property isFinished 保存し終えた。
     * @property writeFailure 保存に失敗した。Snackbar で知らせ終えたら `onWriteFailureShown` で戻す。
     */
    data class UiState(
        val draft: EventDraftUiModel,
        val oshis: List<OshiUiModel>,
        val isNew: Boolean,
        val onboarding: Boolean,
        val canSave: Boolean,
        val canClose: Boolean,
        val isFinished: Boolean,
        val writeFailure: WriteFailureUiModel?,
    )

    /** 編集する予定。id・登録した時刻・ピン留めは保存するときに引き継ぐ。 */
    private var original: OshiEvent? = null

    private val draft = MutableStateFlow<EventDraftUiModel?>(null)
    private val saving = MutableStateFlow(false)
    private val finished = MutableStateFlow(false)
    private val writeFailure = MutableStateFlow<WriteFailureUiModel?>(null)

    /** 最初に購読されたときに、開いた予定（追加なら既定値）から入力を始める。読めなければ `.catch` で `Error` にする。 */
    private val loadedDraft = draft
        .onStart {
            if (draft.value == null) {
                val oshis = oshiRepository.oshis.first()
                val editing = eventId?.let { id -> eventRepository.events.first().firstOrNull { it.id.toString() == id } }
                original = editing
                draft.value = editing?.toDraft() ?: newDraft(oshis)
            }
        }
        .filterNotNull()

    val viewState: StateFlow<ViewState> =
        combine(loadedDraft, oshiRepository.oshis, saving, finished, writeFailure) { currentDraft, oshis, isSaving, isFinished, failure ->
            // 選んでいた推しが消えたら推しなしにする
            val validDraft = if (currentDraft.oshiId != null && oshis.none { it.id.toString() == currentDraft.oshiId }) {
                currentDraft.copy(oshiId = null)
            } else {
                currentDraft
            }
            val uiState = UiState(
                draft = validDraft,
                oshis = oshis.map { it.toUiModel() },
                isNew = original == null,
                onboarding = onboarding,
                canSave = OshiEvent.normalizeTitle(validDraft.title).isNotEmpty() && !isSaving && !isFinished,
                canClose = !isSaving,
                isFinished = isFinished,
                writeFailure = failure,
            )
            ViewState.Success(uiState) as ViewState
        }
            .catch { error -> emit(ViewState.Error(error.message.orEmpty())) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ViewState.Loading)

    fun onOshiSelect(oshiId: String?) {
        draft.update { it?.copy(oshiId = oshiId) }
    }

    /** 種類を選ぶ。誕生日は毎年くり返し、それ以外はくり返さない（あとから切り替えられる）。 */
    fun onTypeSelect(type: EventTypeUiModel) {
        draft.update { it?.copy(type = type, repeatsYearly = type == EventTypeUiModel.BIRTHDAY) }
    }

    fun onTitleChange(title: String) {
        draft.update { it?.copy(title = title.takeCharacters(OshiEvent.MAX_TITLE_LENGTH)) }
    }

    fun onDateChange(date: LocalDate) {
        draft.update { it?.copy(date = date) }
    }

    /** 開始の時刻。null で時刻なし。 */
    fun onStartTimeChange(startTime: LocalTime?) {
        draft.update { it?.copy(startTime = startTime) }
    }

    fun onVenueChange(venue: String) {
        draft.update { it?.copy(venue = venue.takeCharacters(OshiEvent.MAX_VENUE_LENGTH)) }
    }

    fun onRepeatsYearlyChange(repeatsYearly: Boolean) {
        draft.update { it?.copy(repeatsYearly = repeatsYearly) }
    }

    fun onSave() {
        val uiState = (viewState.value as? ViewState.Success)?.uiState ?: return

        if (!uiState.canSave) return

        // 公開している状態は combine を通って遅れて届くので、二重に保存しないよう元の値で確かめる
        if (saving.value || finished.value) return

        saving.value = true
        val currentDraft = uiState.draft
        val editing = original
        val event = OshiEvent(
            id = editing?.id ?: UUID.randomUUID(),
            oshiId = currentDraft.oshiId?.let(UUID::fromString),
            type = currentDraft.type.toModel(),
            title = OshiEvent.normalizeTitle(currentDraft.title),
            date = currentDraft.date,
            startTime = currentDraft.startTime,
            venue = OshiEvent.normalizeVenue(currentDraft.venue),
            repeatsYearly = currentDraft.repeatsYearly,
            pinned = editing?.pinned ?: false,
            createdAt = editing?.createdAt ?: clock.instant(),
        )
        viewModelScope.launch {
            try {
                eventRepository.save(event)
                finished.value = true
            } catch (exception: IOException) {
                writeFailure.value = WriteFailureUiModel.SAVE
                saving.value = false
            }
        }
    }

    fun onWriteFailureShown() {
        writeFailure.value = null
    }

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
