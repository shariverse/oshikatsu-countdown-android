package com.basekeita.oshikatsucountdown.ui.component.atoms

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.basekeita.oshikatsucountdown.ui.photo.rememberPhotoBitmap
import com.basekeita.oshikatsucountdown.ui.uimodel.OshiColorUiModel

/**
 * 推しのアバター。写真があれば丸く切り抜き、無ければ推し色の丸に頭文字を置く。
 * 名前は横に並べて出す前提なので、読み上げの対象にしない。
 */
@Composable
fun OshiAvatar(
    initial: String,
    color: OshiColorUiModel,
    modifier: Modifier = Modifier,
    photoPath: String? = null,
    size: Dp = 40.dp,
) {
    val photoEdgePx = with(LocalDensity.current) { size.roundToPx() }
    val photo by rememberPhotoBitmap(source = photoPath, maxEdgePx = photoEdgePx.coerceAtLeast(1))
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.color),
        contentAlignment = Alignment.Center,
    ) {
        val loadedPhoto = photo
        if (loadedPhoto != null) {
            Image(
                bitmap = loadedPhoto,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        } else {
            Text(
                text = initial,
                color = color.onColor,
                fontWeight = FontWeight.Bold,
                // 丸の大きさに合わせる。文字サイズの設定で丸からはみ出さないよう sp に換算しない
                fontSize = with(LocalDensity.current) { (size * 0.4f).toSp() },
                lineHeight = with(LocalDensity.current) { (size * 0.4f).toSp() },
            )
        }
    }
}

@Preview
@Composable
private fun OshiAvatarPreview() {
    OshiAvatar(initial = "L", color = OshiColorUiModel.PINK, size = 48.dp)
}
