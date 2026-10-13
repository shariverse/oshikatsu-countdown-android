package com.basekeita.oshikatsucountdown.domain.oshi

import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.repository.EventRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.OshiRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.PhotoRepository
import javax.inject.Inject

/** 推しを削除する。結び付いた予定と写真も消す。 */
class DeleteOshiUseCase
@Inject
constructor(
    private val oshiRepository: OshiRepository,
    private val eventRepository: EventRepository,
    private val photoRepository: PhotoRepository,
) {
    suspend operator fun invoke(oshi: Oshi) {
        eventRepository.deleteByOshi(oshi.id)
        oshiRepository.delete(oshi.id)
        oshi.photoPath?.let { path -> photoRepository.delete(path) }
    }
}
