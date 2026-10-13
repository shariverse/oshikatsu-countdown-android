package com.basekeita.oshikatsucountdown.feature.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.basekeita.oshikatsucountdown.ui.theme.OshikatsuTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 1 件のウィジェット（`EventCountdownWidget`）に出す予定を選ぶ画面。ホーム画面がウィジェットを置くとき・選び直すときに開く。
 * ウィジェットの「予定が見つかりません」を押したときも開く。
 */
@AndroidEntryPoint
class WidgetConfigureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // 選ばずに閉じたら、ホーム画面は置こうとしたウィジェットを置かない
        setResult(RESULT_CANCELED, resultIntent(appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            OshikatsuTheme {
                WidgetConfigureScreen(
                    appWidgetId = appWidgetId,
                    onFinish = {
                        setResult(RESULT_OK, resultIntent(appWidgetId))
                        finish()
                    },
                    onClose = ::finish,
                )
            }
        }
    }

    private fun resultIntent(appWidgetId: Int): Intent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}
