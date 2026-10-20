package com.drquizman.tv

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.darkColorScheme
import com.drquizman.tv.ui.MenuScreen
import com.drquizman.tv.ui.ShowScreen
import com.drquizman.tv.ui.TvColors

class MainActivity : ComponentActivity() {
    private val vm: HostViewModel by viewModels()
    private val networkPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { }

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        networkPermission.launch(
            if (Build.VERSION.SDK_INT >= 33) {
                arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
            } else {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            },
        )
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = TvColors.magenta,
                    onPrimary = TvColors.cream,
                    background = TvColors.deep,
                    surface = TvColors.mid,
                ),
            ) {
                Surface(Modifier.fillMaxSize(), shape = RectangleShape) {
                    val screen by vm.screen.collectAsState()
                    val snap by vm.snapshot.collectAsState()
                    val help by vm.help.collectAsState()
                    val starting by vm.starting.collectAsState()
                    val speaking by vm.speaking.collectAsState()
                    val viseme by vm.viseme.collectAsState()
                    val speechEngines by vm.speechEngines.collectAsState()
                    val speechVoices by vm.speechVoices.collectAsState()
                    val selectedSpeechEngine by vm.selectedSpeechEngine.collectAsState()
                    val selectedSpeechVoice by vm.selectedSpeechVoice.collectAsState()
                    val speechStatus by vm.speechStatus.collectAsState()
                    val language by vm.language.collectAsState()
                    val packs by vm.packs.collectAsState()
                    BoxWithConstraints(
                        Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing),
                    ) {
                    val screenDensity = LocalDensity.current
                    val fitScale = minOf(
                        maxWidth.value / 960f,
                        maxHeight.value / 540f,
                    ).coerceAtMost(1f)
                    val fittedDensity = Density(
                        density = screenDensity.density * fitScale,
                        fontScale = screenDensity.fontScale,
                    )
                    CompositionLocalProvider(
                        LocalDensity provides fittedDensity,
                        LocalLayoutDirection provides if (language == "he") LayoutDirection.Rtl else LayoutDirection.Ltr,
                    ) {
                    key(language) {
                    when (screen) {
                        HostViewModel.Screen.MENU -> MenuScreen(
                            help = help,
                            starting = starting || packs.busy,
                            onPlay = vm::startShow,
                            onHelp = { vm.showHelp(true) },
                            onBackFromHelp = { vm.showHelp(false) },
                            speechEngines = speechEngines,
                            speechVoices = speechVoices,
                            selectedSpeechEngine = selectedSpeechEngine,
                            selectedSpeechVoice = selectedSpeechVoice,
                            speechStatus = speechStatus,
                            onSpeechEngineSelected = vm::selectSpeechEngine,
                            onSpeechVoiceSelected = vm::selectSpeechVoice,
                            onSpeechVoiceFocused = vm::previewSpeechVoice,
                            language = language,
                            onLanguageSelected = vm::selectLanguage,
                            packs = packs,
                            onPackSelected = vm::selectPack,
                            onLinkTelegram = vm::linkTelegram,
                            onRefreshPacks = vm::refreshPacks,
                            onDownloadPacks = vm::downloadAllPacks,
                            onClosePacks = vm::closePacks,
                        )
                        HostViewModel.Screen.SHOW -> snap?.let {
                            ShowScreen(
                                snap = it,
                                onSkip = vm::skipIntro,
                                onAgain = vm::newShow,
                                onMenu = vm::backToMenu,
                                speaking = speaking,
                                viseme = viseme,
                            )
                        }
                    }
                    }
                    }
                    }
                }
            }
        }
    }

    override fun onPause() {
        vm.onAppPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        vm.onAppResume()
    }
}
