package com.basekeita.oshikatsucountdown.ui.component.molecules

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.basekeita.oshikatsucountdown.ui.component.atoms.StripedBackground
import com.basekeita.oshikatsucountdown.ui.photo.rememberPhotoBitmap
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel

/**
 * 推しの写真を領域いっぱいに切り抜いて敷く。写真が無い・読み込み中は推し色の縞で埋める。
 * 飾りなので読み上げの対象にしない。
 *
 * @param photoSource 取り込んだ写真の絶対パスか、選んだばかりの写真の `content://` の URI。
 */
@Composable
fun OshiPhoto(
    photoSource: String?,
    color: OshiColorUiModel,
    modifier: Modifier = Modifier,
) {
    val photo by rememberPhotoBitmap(source = photoSource)
    Crossfade(targetState = photo, modifier = modifier, label = "OshiPhoto") { loadedPhoto ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (loadedPhoto != null) {
                Image(
                    bitmap = loadedPhoto,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                StripedBackground(color = color.color, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun OshiPhotoPreview() {
    OshiPhoto(
        photoSource = null,
        color = OshiColorUiModel.BLUE,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
    )
}
