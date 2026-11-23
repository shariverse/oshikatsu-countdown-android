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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Clock

/**
 * 推しの登録・編集。写真の取り込みと差し替え（`SaveOshiUseCase`）、結び付いた予定ごとの削除（`DeleteOshiUseCase`）を呼ぶ。
 * 保存・削除し終えると `isFinished` を立て、画面はそれを見て閉じる（先に閉じると `viewModelScope` が止まり、書き込みが途中で切れるため）。
 */
@HiltViewModel(assistedFactory = OshiEditorViewModel.Factory::class)
class OshiEditorViewModel @AssistedInject constructor(
    @Assisted private val oshiId: String?,
    @Assisted private val onboarding: Boolean,
    private val oshiRepository: OshiRepository,
    eventRepository: EventRepository,
    private val saveOshi: SaveOshiUseCase,
    private val deleteOshi: DeleteOshiUseCase,
    private val clock: Clock,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(
            oshiId: String?,
            onboarding: Boolean,
        ): OshiEditorViewModel
    }

    /**
     * @property isLoadFailed 編集する推しを読めなかった。
     * @property photoSource 表示する写真。取り込み済みの写真の絶対パスか、選んだばかりの写真の `content://` の URI。
     * @property eventCount この推しに結び付いた予定の数（削除の確認に出す）。
     * @property isWriting 保存・削除の途中。
     * @property isFinished 保存・削除し終えた。
     * @property writeFailure 保存・削除に失敗した。Snackbar で知らせ終えたら `onWriteFailureShown` で戻す。
     */
    data class UiState(
        val isLoading: Boolean = true,
        val isLoadFailed: Boolean = false,
        val name: String = "",
        val color: OshiColorUiModel = OshiColor.DEFAULT.toUiModel(),
        val photoSource: String? = null,
        val isNew: Boolean = true,
        val onboarding: Boolean = false,
        val eventCount: Int = 0,
        val isWriting: Boolean = false,
        val isFinished: Boolean = false,
        val writeFailure: WriteFailureUiModel? = null,
    ) {
        /** 名前が空でなく、保存・削除の途中でも、し終えてもいない。 */
        val canSave: Boolean
            get() = !isLoading && !isLoadFailed && Oshi.normalizeName(name).isNotEmpty() && !isWriting && !isFinished

        /** 保存・削除の途中でない。途中で閉じると `viewModelScope` ごと書き込みが止まるため、閉じる操作と戻る操作を止める。 */
        val canClose: Boolean
            get() = !isWriting
    }

    /** 入力中の値。写真は「今のまま」「外す」「差し替える」のどれかで持ち、保存するときに `PhotoChange` にする。 */
    private data class Draft(
        val name: String = "",
        val color: OshiColorUiModel = OshiColor.DEFAULT.toUiModel(),
        val photoChange: PhotoChange = PhotoChange.Keep,
    )

    /**
     * 画面が持つ状態。
     *
     * @property isLoaded 編集する推し（登録なら空）から入力を始めた。
     * @property original 編集する推し。登録は null。
     */
    private data class ScreenState(
        val isLoaded: Boolean = false,
        val draft: Draft = Draft(),
        val original: Oshi? = null,
        val isWriting: Boolean = false,
        val isFinished: Boolean = false,
        val writeFailure: WriteFailureUiModel? = null,
    )

    private val screenState = MutableStateFlow(ScreenState())

    /** 最初に購読されたときに 1 回だけ、編集する推し（登録なら空）から入力を始める。読めなければ `.catch` で `isLoadFailed` にする。 */
    private val loadedScreenState = screenState
        .onStart {
            if (!screenState.value.isLoaded) {
                val editing = oshiId?.let { id -> oshiRepository.oshis.first().firstOrNull { it.id.toString() == id } }
                val draft = Draft(
                    name = editing?.name.orEmpty(),
                    color = (editing?.color ?: OshiColor.DEFAULT).toUiModel(),
                    photoChange = PhotoChange.Keep,
                )
                screenState.update { it.copy(isLoaded = true, original = editing, draft = draft) }
            }
        }
        .filter { it.isLoaded }

    val uiState: StateFlow<UiState> =
        combine(loadedScreenState, eventRepository.events) { screen, events ->
            val editing = screen.original
            val photoSource = when (val change = screen.draft.photoChange) {
                PhotoChange.Keep -> editing?.photoPath
                PhotoChange.Remove -> null
                is PhotoChange.Replace -> change.sourceUri
            }
            UiState(
                isLoading = false,
                name = screen.draft.name,
                color = screen.draft.color,
                photoSource = photoSource,
                isNew = editing == null,
                onboarding = onboarding,
                eventCount = editing?.let { oshi -> events.count { it.oshiId == oshi.id } } ?: 0,
                isWriting = screen.isWriting,
                isFinished = screen.isFinished,
                writeFailure = screen.writeFailure,
            )
        }
            .catch { emit(UiState(isLoading = false, isLoadFailed = true, onboarding = onboarding)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun onNameChange(name: String) {
        updateDraft { it.copy(name = name.takeCharacters(Oshi.MAX_NAME_LENGTH)) }
    }

    fun onColorSelect(color: OshiColorUiModel) {
        updateDraft { it.copy(color = color) }
    }

    /** Photo Picker で選んだ写真。取り込みは保存するときに行う。 */
    fun onPhotoPick(sourceUri: String) {
        updateDraft { it.copy(photoChange = PhotoChange.Replace(sourceUri)) }
    }

    fun onPhotoRemove() {
        updateDraft { it.copy(photoChange = PhotoChange.Remove) }
    }

    /** 保存してよいかは、公開の状態（`combine` を通って遅れて届く）ではなく、画面が持つ状態の今の値で決める。 */
    fun onSave() {
        val screen = screenState.value
        if (!screen.isLoaded || screen.isWriting || screen.isFinished) return

        if (Oshi.normalizeName(screen.draft.name).isEmpty()) return

        screenState.update { it.copy(isWriting = true) }
        viewModelScope.launch {
            try {
                saveOshi(
                    current = screen.original,
                    name = screen.draft.name,
                    color = screen.draft.color.toModel(),
                    photoChange = screen.draft.photoChange,
                    now = clock.instant(),
                )
                screenState.update { it.copy(isWriting = false, isFinished = true) }
            } catch (exception: IOException) {
                screenState.update { it.copy(isWriting = false, writeFailure = WriteFailureUiModel.SAVE) }
            }
        }
    }

    /** 推しと、結び付いた予定と写真を削除する。登録前（新しい推し）は何もしない。 */
    fun onDelete() {
        val screen = screenState.value
        val editing = screen.original ?: return

        if (screen.isWriting || screen.isFinished) return

        screenState.update { it.copy(isWriting = true) }
        viewModelScope.launch {
            try {
                deleteOshi(editing)
                screenState.update { it.copy(isWriting = false, isFinished = true) }
            } catch (exception: IOException) {
                screenState.update { it.copy(isWriting = false, writeFailure = WriteFailureUiModel.DELETE) }
            }
        }
    }

    fun onWriteFailureShown() {
        screenState.update { it.copy(writeFailure = null) }
    }

    private fun updateDraft(change: (Draft) -> Draft) {
        screenState.update { it.copy(draft = change(it.draft)) }
    }
}
