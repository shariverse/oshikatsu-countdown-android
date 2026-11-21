package com.basekeita.oshikatsucountdown.domain.oshi

import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.repository.OshiRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.PhotoRepository
import javax.inject.Inject

/** 推しを削除する。結び付いた予定は推しと同じ書き込みで消え（`OshiRepository.delete`）、そのあと写真を消す。 */
class DeleteOshiUseCase
@Inject
constructor(
    private val oshiRepository: OshiRepository,
    private val photoRepository: PhotoRepository,
) {
    suspend operator fun invoke(oshi: Oshi) {
        oshiRepository.delete(oshi.id)
        oshi.photoPath?.let { path -> photoRepository.delete(path) }
    }
}
