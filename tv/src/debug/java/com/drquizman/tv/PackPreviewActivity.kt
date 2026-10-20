package com.drquizman.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.drquizman.tv.questions.*
import com.drquizman.tv.ui.QuestionPacksScreen
import com.drquizman.tv.ui.TvColors

/** Debug-only screenshot harness. No account, network access or changes to installed packs. */
class PackPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val info = PackInfo("new_750_questions_ru", "Дополнительный пакет №1", "ru", 750, 15, "0".repeat(64))
        val downloadable = intent.getBooleanExtra("downloadable", false)
        val unlinked = intent.getBooleanExtra("unlinked", false)
        setContent {
            var selected by remember { mutableStateOf(true) }
            MaterialTheme(colorScheme = darkColorScheme(primary = TvColors.magenta, background = TvColors.deep, surface = TvColors.mid)) {
                Box(Modifier.fillMaxSize().background(TvColors.deep)) {
                    QuestionPacksScreen(
                        state = PacksState(
                            installed = if (downloadable || unlinked) emptyList() else listOf(InstalledPack(info, selected, emptyList())),
                            available = if (downloadable && !unlinked) listOf(info) else emptyList(), linked = !unlinked,
                            code = if (unlinked) "ABCD2345" else null, busy = unlinked),
                        onSelect = { _, value -> selected = value }, onLink = {}, onRefresh = {}, onDownload = {}, onClose = { finish() })
                }
            }
        }
    }
}
