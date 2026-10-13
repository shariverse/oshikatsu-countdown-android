package com.basekeita.oshikatsucountdown.ui.uimodel

import androidx.annotation.StringRes
import com.basekeita.oshikatsucountdown.ui.R

/** 書き込み（保存・削除）に失敗したこと。画面は Snackbar で知らせ、出し終えたら ViewModel に返す。 */
enum class WriteFailureUiModel(
    @param:StringRes val messageRes: Int,
) {
    SAVE(R.string.save_error),
    DELETE(R.string.delete_error),
}
