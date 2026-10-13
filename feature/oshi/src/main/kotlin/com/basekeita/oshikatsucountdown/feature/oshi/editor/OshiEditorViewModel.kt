package com.basekeita.oshikatsucountdown.feature.oshi.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.model.OshiColor
import com.basekeita.oshikatsucountdown.data.definition.model.takeCharacters
import com.basekeita.oshikatsucountdown.data.definition.repository.EventRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.OshiRepository
import com.basekeita.oshikatsucountdown.domain.oshi.DeleteOshiUseCase
import com.basekeita.oshikatsucountdown.domain.oshi.PhotoChange
import com.basekeita.oshikatsucountdown.domain.oshi.SaveOshiUseCase
import com.basekeita.oshikatsucountdown.feature.oshi.uimodel.toModel
import com.basekeita.oshikatsucountdown.feature.oshi.uimodel.toUiModel
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel
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

/**
 * 推しの登録・編集。写真の取り込みと差し替え（`SaveOshiUseCase`）、結び付いた予定ごとの削除（`DeleteOshiUseCase`）を呼ぶ。
 * 保存・削除し終えると `isFinished` を立て、画面はそれを見て閉じる（先に閉じると `viewModelScope` が止まり、書き込みが途中で切れるため）。
 */
@HiltViewModel(assistedFactory = OshiEditorViewModel.Factory::class)
class OshiEditorViewModel
@AssistedInject
constructor(
    @Assisted private val oshiId: String?,
    @Assisted private val onboarding: Boolean,
    oshiRepository: OshiRepository,
    eventRepository: EventRepository,
    private val saveOshi: SaveOshiUseCase,
    private val deleteOshi: DeleteOshiUseCase,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(
            oshiId: String?,
            onboarding: Boolean,
        ): OshiEditorViewModel
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
     * @property photoSource 表示する写真。取り込み済みの写真の絶対パスか、選んだばかりの写真の `content://` の URI。
     * @property eventCount この推しに結び付いた予定の数（削除の確認に出す）。
     * @property canSave 名前が空でなく、保存中でもない。
     * @property canClose 保存・削除の途中でない。途中で閉じると `viewModelScope` ごと書き込みが止まるため、閉じる操作と戻る操作を止める。
     * @property isFinished 保存・削除し終えた。
     * @property writeFailure 保存・削除に失敗した。Snackbar で知らせ終えたら `onWriteFailureShown` で戻す。
     */
    data class UiState(
        val name: String,
        val color: OshiColorUiModel,
        val photoSource: String?,
        val isNew: Boolean,
        val onboarding: Boolean,
        val eventCount: Int,
        val canSave: Boolean,
        val canClose: Boolean,
        val isFinished: Boolean,
        val writeFailure: WriteFailureUiModel?,
    )

    /** 入力中の値。写真は「今のまま」「外す」「差し替える」のどれかで持ち、保存するときに `PhotoChange` にする。 */
    private data class Draft(
        val name: String,
        val color: OshiColorUiModel,
        val photoChange: PhotoChange,
    )

    /** 編集する推し。 */
    private var original: Oshi? = null

    private val draft = MutableStateFlow<Draft?>(null)
    private val busy = MutableStateFlow(false)
    private val finished = MutableStateFlow(false)
    private val writeFailure = MutableStateFlow<WriteFailureUiModel?>(null)

    /** 最初に購読されたときに、編集する推し（登録なら空）から入力を始める。読めなければ `.catch` で `Error` にする。 */
    private val loadedDraft = draft
        .onStart {
            if (draft.value == null) {
                val editing = oshiId?.let { id -> oshiRepository.oshis.first().firstOrNull { it.id.toString() == id } }
                original = editing
                draft.value = Draft(
                    name = editing?.name.orEmpty(),
                    color = (editing?.color ?: OshiColor.DEFAULT).toUiModel(),
                    photoChange = PhotoChange.Keep,
                )
            }
        }
        .filterNotNull()

    val viewState: StateFlow<ViewState> =
        combine(loadedDraft, eventRepository.events, busy, finished, writeFailure) { currentDraft, events, isBusy, isFinished, failure ->
            val editing = original
            val photoSource = when (val change = currentDraft.photoChange) {
                PhotoChange.Keep -> editing?.photoPath
                PhotoChange.Remove -> null
                is PhotoChange.Replace -> change.sourceUri
            }
            val uiState = UiState(
                name = currentDraft.name,
                color = currentDraft.color,
                photoSource = photoSource,
                isNew = editing == null,
                onboarding = onboarding,
                eventCount = editing?.let { oshi -> events.count { it.oshiId == oshi.id } } ?: 0,
                canSave = Oshi.normalizeName(currentDraft.name).isNotEmpty() && !isBusy && !isFinished,
                canClose = !isBusy,
                isFinished = isFinished,
                writeFailure = failure,
            )
            ViewState.Success(uiState) as ViewState
        }
            .catch { error -> emit(ViewState.Error(error.message.orEmpty())) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ViewState.Loading)

    fun onNameChange(name: String) {
        draft.update { it?.copy(name = name.takeCharacters(Oshi.MAX_NAME_LENGTH)) }
    }

    fun onColorSelect(color: OshiColorUiModel) {
        draft.update { it?.copy(color = color) }
    }

    /** Photo Picker で選んだ写真。取り込みは保存するときに行う。 */
    fun onPhotoPick(sourceUri: String) {
        draft.update { it?.copy(photoChange = PhotoChange.Replace(sourceUri)) }
    }

    fun onPhotoRemove() {
        draft.update { it?.copy(photoChange = PhotoChange.Remove) }
    }

    fun onSave() {
        val currentDraft = draft.value ?: return

        val uiState = (viewState.value as? ViewState.Success)?.uiState ?: return

        if (!uiState.canSave) return

        // 公開している状態は combine を通って遅れて届くので、二重に保存しないよう元の値で確かめる
        if (busy.value || finished.value) return

        busy.value = true
        viewModelScope.launch {
            try {
                saveOshi(
                    current = original,
                    name = currentDraft.name,
                    color = currentDraft.color.toModel(),
                    photoChange = currentDraft.photoChange,
                )
                finished.value = true
            } catch (exception: IOException) {
                writeFailure.value = WriteFailureUiModel.SAVE
                busy.value = false
            }
        }
    }

    /** 推しと、結び付いた予定と写真を削除する。登録前（新しい推し）は何もしない。 */
    fun onDelete() {
        val editing = original ?: return

        if (busy.value || finished.value) return

        busy.value = true
        viewModelScope.launch {
            try {
                deleteOshi(editing)
                finished.value = true
            } catch (exception: IOException) {
                writeFailure.value = WriteFailureUiModel.DELETE
                busy.value = false
            }
        }
    }

    fun onWriteFailureShown() {
        writeFailure.value = null
    }
}
