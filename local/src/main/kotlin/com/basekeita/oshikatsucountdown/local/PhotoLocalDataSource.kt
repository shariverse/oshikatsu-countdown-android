package com.basekeita.oshikatsucountdown.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 推しの写真の取り込み先（アプリの内部ストレージの `oshi_photos/`）。
 * Photo Picker が返す URI の読み取り権限は一時的なので、選んだ時点で縮小した JPEG を自分のファイルとして持つ。
 */
class PhotoLocalDataSource
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val photoDirectory: File
        get() = File(context.filesDir, PHOTO_DIRECTORY_NAME)

    /** 長辺を [MAX_PHOTO_EDGE_PX] 以下に縮めて JPEG で書き出し、書き出したファイルの絶対パスを返す。読めなければ null。 */
    suspend fun import(sourceUri: String): String? = withContext(Dispatchers.IO) {
        val bitmap = decodeScaled(sourceUri) ?: return@withContext null

        val file = File(photoDirectory.apply { mkdirs() }, "${UUID.randomUUID()}.jpg")
        val written = try {
            file.outputStream().use { output -> bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output) }
        } catch (exception: IOException) {
            false
        } finally {
            bitmap.recycle()
        }
        if (written) {
            file.absolutePath
        } else {
            // 書きかけのファイルを残すと、どの推しからも参照されないまま取り込み先に残り続ける
            file.delete()
            null
        }
    }

    private fun decodeScaled(sourceUri: String): Bitmap? = try {
        val source = ImageDecoder.createSource(context.contentResolver, sourceUri.toUri())
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val longEdge = max(info.size.width, info.size.height)
            if (longEdge > MAX_PHOTO_EDGE_PX) {
                val scale = MAX_PHOTO_EDGE_PX.toFloat() / longEdge
                decoder.setTargetSize(
                    (info.size.width * scale).roundToInt().coerceAtLeast(1),
                    (info.size.height * scale).roundToInt().coerceAtLeast(1),
                )
            }
            // ハードウェアの Bitmap は compress できないため、ソフトウェアで展開する
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } catch (exception: IOException) {
        null
    } catch (exception: SecurityException) {
        null
    }

    /** 取り込み先の中のファイルだけを消す。 */
    suspend fun delete(path: String) {
        withContext(Dispatchers.IO) {
            val file = File(path)
            if (file.parentFile?.canonicalPath == photoDirectory.canonicalPath) {
                file.delete()
            }
        }
    }

    private companion object {
        const val PHOTO_DIRECTORY_NAME = "oshi_photos"
        const val MAX_PHOTO_EDGE_PX = 1440
        const val JPEG_QUALITY = 88
    }
}
