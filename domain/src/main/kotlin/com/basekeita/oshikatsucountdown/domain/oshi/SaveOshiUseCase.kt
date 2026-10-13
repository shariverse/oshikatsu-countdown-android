package com.basekeita.oshikatsucountdown.domain.oshi

import com.basekeita.oshikatsucountdown.data.definition.model.Oshi
import com.basekeita.oshikatsucountdown.data.definition.model.OshiColor
import com.basekeita.oshikatsucountdown.data.definition.repository.OshiRepository
import com.basekeita.oshikatsucountdown.data.definition.repository.PhotoRepository
import java.io.IOException
import javax.inject.Inject

/**
 * 推しを保存する。写真を差し替えるときは新しい写真を取り込んでから保存し、使わなくなった写真を消す。
 * 取り込みに失敗したときは今の写真のまま保存する。保存に失敗したときは取り込んだ写真を消し、`IOException` をそのまま投げる。
 */
class SaveOshiUseCase
@Inject
constructor(
    private val oshiRepository: OshiRepository,
    private val photoRepository: PhotoRepository,
) {
    /**
     * @param current 編集中の推し。新しく登録するときは null。
     * @return 保存した推し。名前が空のときは保存せず null。
     */
    suspend operator fun invoke(
        current: Oshi?,
        name: String,
        color: OshiColor,
        photoChange: PhotoChange,
    ): Oshi? {
        val normalizedName = Oshi.normalizeName(name)
        if (normalizedName.isEmpty()) return null

        val currentPhotoPath = current?.photoPath
        val importedPhotoPath = (photoChange as? PhotoChange.Replace)?.let { photoRepository.import(it.sourceUri) }
        val photoPath = when (photoChange) {
            PhotoChange.Keep -> currentPhotoPath
            PhotoChange.Remove -> null
            is PhotoChange.Replace -> importedPhotoPath ?: currentPhotoPath
        }

        val oshi = current?.copy(name = normalizedName, color = color, photoPath = photoPath)
            ?: Oshi(name = normalizedName, color = color, photoPath = photoPath)
        try {
            oshiRepository.save(oshi)
        } catch (exception: IOException) {
            // キャンセルでは消さない。書き込みが終わった後のキャンセルだと、保存した推しの写真を消してしまうため
            if (importedPhotoPath != null) photoRepository.delete(importedPhotoPath)
            throw exception
        }

        if (currentPhotoPath != null && currentPhotoPath != photoPath) {
            photoRepository.delete(currentPhotoPath)
        }
        return oshi
    }
}
