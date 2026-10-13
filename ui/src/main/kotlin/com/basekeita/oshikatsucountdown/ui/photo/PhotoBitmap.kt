package com.basekeita.oshikatsucountdown.ui.photo

import android.content.Context
import android.graphics.ImageDecoder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt

/** 表示に使う写真の長辺の上限。取り込んだ写真（長辺 1440px）を一覧の小さなアバターでも縮めて持つ。 */
private const val DEFAULT_MAX_EDGE_PX = 1080

/**
 * 写真を読み込む。`source` はアプリに取り込んだ写真の絶対パスか、Photo Picker が返した `content://` の URI。
 * 読み込み中・読めないとき・`source` が null のときは null。
 */
@Composable
fun rememberPhotoBitmap(
    source: String?,
    maxEdgePx: Int = DEFAULT_MAX_EDGE_PX,
): State<ImageBitmap?> {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(initialValue = null, source, maxEdgePx) {
        value = source?.let { decodePhoto(context, it, maxEdgePx) }
    }
}

private suspend fun decodePhoto(
    context: Context,
    source: String,
    maxEdgePx: Int,
): ImageBitmap? = withContext(Dispatchers.IO) {
    try {
        val imageSource = if (source.startsWith("content://")) {
            ImageDecoder.createSource(context.contentResolver, source.toUri())
        } else {
            ImageDecoder.createSource(File(source))
        }
        ImageDecoder.decodeBitmap(imageSource) { decoder, info, _ ->
            val longEdge = max(info.size.width, info.size.height)
            if (longEdge > maxEdgePx) {
                val scale = maxEdgePx.toFloat() / longEdge
                decoder.setTargetSize(
                    (info.size.width * scale).roundToInt().coerceAtLeast(1),
                    (info.size.height * scale).roundToInt().coerceAtLeast(1),
                )
            }
        }.asImageBitmap()
    } catch (exception: IOException) {
        null
    } catch (exception: SecurityException) {
        null
    }
}
