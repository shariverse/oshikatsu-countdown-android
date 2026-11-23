package com.basekeita.oshikatsucountdown.feature.reminder

import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** 置き直すきっかけのうち、システムから届くもの（Receiver が受ける知らせと、マニフェストで受け取りを登録した知らせ）。 */
class ReminderSystemActionsTest {
    @Test
    fun `再起動・更新・時刻とタイムゾーンの変更・正確なアラームの許可で置き直す`() {
        assertEquals(
            setOf(
                "android.intent.action.BOOT_COMPLETED",
                "android.intent.action.MY_PACKAGE_REPLACED",
                "android.intent.action.TIME_SET",
                "android.intent.action.TIMEZONE_CHANGED",
                "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
            ),
            REMINDER_SYSTEM_ACTIONS,
        )
    }

    @Test
    fun `マニフェストで受け取りを登録した知らせは、Receiver が受ける知らせと同じ`() {
        // テストはモジュールのフォルダで動く
        val manifest = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File("src/main/AndroidManifest.xml"))
        val receivers = manifest.getElementsByTagName("receiver")
        val systemReceiver = (0 until receivers.length)
            .map { receivers.item(it) as Element }
            .single { it.getAttribute("android:name") == ".ReminderSystemReceiver" }
        val actions = systemReceiver.getElementsByTagName("action")
        val declared = (0 until actions.length).map { (actions.item(it) as Element).getAttribute("android:name") }.toSet()

        assertEquals(REMINDER_SYSTEM_ACTIONS, declared)
    }
}
