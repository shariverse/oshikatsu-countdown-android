package com.basekeita.oshikatsucountdown.local

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

/**
 * 保存する推しの形。色は enum の名前ではなく保存用の文字列で持ち、名前を変えても読めるようにする（対応は data:impl）。
 *
 * @property photoPath 取り込んだ写真の絶対パス。
 */
@Serializable
data class StoredOshi(
    @Serializable(with = UuidSerializer::class)
    val id: UUID,
    val name: String,
    val color: String,
    val photoPath: String? = null,
)

/**
 * 保存する予定の形。日付は ISO 8601 の `yyyy-MM-dd`、時刻は `HH:mm`、登録した時刻は UNIX 時間のミリ秒。
 * 後から足した項目にも既定値を置き、古い形の JSON も読めるようにする。
 */
@Serializable
data class StoredEvent(
    @Serializable(with = UuidSerializer::class)
    val id: UUID,
    @Serializable(with = UuidSerializer::class)
    val oshiId: UUID? = null,
    val type: String,
    val title: String,
    val date: String,
    val startTime: String? = null,
    val venue: String = "",
    val repeatsYearly: Boolean = false,
    val pinned: Boolean = false,
    val createdAtEpochMillis: Long,
)

/** UUID を文字列で読み書きする。UUID として読めない文字列は例外にし、JSON 全体を読めないものとして扱う。 */
internal object UuidSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java.util.UUID", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: UUID,
    ) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
}
