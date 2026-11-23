package com.basekeita.oshikatsucountdown.feature.reminder

/**
 * 予約の記録（入れたアラーム・出した通知の識別子）の置き換え方。
 * 途中で止まっても（プロセスが死ぬ・呼んだ scope が取り消される）入れたものを後で取り消せるよう、
 * 足す前に「前の記録と今回のもの」を記録に書き（`writeAhead`）、外れたもの（`toCancel`）を取り消して今回のものを入れ、
 * 終えてから今回のものだけ（`final`）にする。
 */
internal data class RecordReplacement(
    val writeAhead: Set<String>,
    val toCancel: Set<String>,
    val final: Set<String>,
)

internal fun recordReplacement(
    previous: Set<String>,
    planned: Set<String>,
): RecordReplacement = RecordReplacement(
    writeAhead = previous + planned,
    toCancel = previous - planned,
    final = planned,
)
