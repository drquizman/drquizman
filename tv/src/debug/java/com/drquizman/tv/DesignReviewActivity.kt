package com.drquizman.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.drquizman.core.model.*
import com.drquizman.tv.ui.*
import com.drquizman.tv.ui.studio.StudioBackground

/** Debug-only deterministic views of the real production components. */
class DesignReviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screen = intent.getStringExtra("screen") ?: "menu"
        val speaking = intent.getBooleanExtra("speaking", false)
        val viseme = intent.getIntExtra("viseme", if (speaking) 2 else 0)
        val type = runCatching { CompetitionType.valueOf(intent.getStringExtra("contest") ?: "FIRST") }.getOrDefault(CompetitionType.FIRST)
        val players = listOf("АННА", "МАКС", "ОЛЯ", "ДЕНИС", "ИРА", "ЛЕВ").mapIndexed { i, name ->
            PlayerState(id = "$i", name = name, colorIndex = i, avatarIndex = i, score = 3200 - i * 300,
                ready = i < 3, finalTimeMs = 30000 - i * 2000L, contestMarks = i % 4, bet = 100)
        }
        val phase = when (screen) {
            "lobby" -> Phase.LOBBY
            "title" -> Phase.TITLE
            "intro" -> Phase.INTRO
            "topic" -> Phase.TOPIC
            "bet" -> Phase.BET
            "winner" -> Phase.GAME_OVER
            "reveal" -> Phase.REVEAL
            else -> Phase.QUESTION
        }
        val snap = GameSnapshot(
            pin = "4821", hostAddress = "192.168.1.105", wifiName = "Домашний Wi-Fi",
            phase = phase, players = players, competition = type,
            competitionTitle = "СТАРТОВЫЙ КАПИТАЛ", competitionSlogan = "НАБЕРИ, ЧТОБЫ БЫЛО ЧТО ТЕРЯТЬ",
            questionIndex = 2, questionTotal = 8,
            hostLine = "Выберите ответ на телефоне. Кто наберёт больше очков?",
            hostMood = HostMood.IDLE,
            question = QuestionView(1, "Какой океан самый большой на Земле?",
                listOf("Атлантический", "Индийский", "Тихий", "Северный Ледовитый"), false,
                if (phase == Phase.REVEAL) 2 else null, "ГЕОГРАФИЯ"),
            timeLeftMs = 12000, timeLimitMs = 20000, paused = screen == "pause",
            canSkipIntro = true, winnerId = "0", winnerName = "АННА",
            topicChooserId = "1", topics = listOf("География", "История", "Наука", "Кино"),
            chosenTopic = if (screen == "bet") "ГЕОГРАФИЯ" else "",
            bettingOpen = screen == "bet", betAmounts = listOf(100, 200, 300, 400),
            bombHolderId = if (type == CompetitionType.BOMB) "2" else null,
            finalTimeCapMs = 30000, contestMarksGoal = if (type == CompetitionType.WHO_IS_RIGHT) 7 else 0,
            puzzleAnswers = type == CompetitionType.PUZZLE, reverseAnswers = type == CompetitionType.EAST,
        )
        setContent {
            var step by remember { mutableIntStateOf(0) }
            LaunchedEffect(screen) {
                if (screen == "director") {
                    delay(42000); step = 1
                    delay(4000); step = 2
                    delay(3000); step = 3
                    delay(3000); step = 4
                    delay(5000); step = 5
                    delay(4000); step = 6
                    delay(3000); step = 7
                    delay(3000); step = 8
                }
            }
            val directed = when (step) {
                0 -> snap.copy(phase = Phase.INTRO)
                1 -> snap.copy(phase = Phase.QUESTION)
                2 -> snap.copy(phase = Phase.QUESTION, paused = true)
                3 -> snap.copy(phase = Phase.REVEAL)
                4 -> snap.copy(phase = Phase.BET, bettingOpen = true)
                7 -> snap.copy(phase = Phase.TITLE)
                8 -> snap.copy(phase = Phase.INTRO)
                else -> snap.copy(phase = Phase.GAME_OVER)
            }
            MaterialTheme(colorScheme = darkColorScheme(primary = TvColors.gold, background = TvColors.deep, surface = TvColors.panel)) {
                when (screen) {
                    "director" -> ShowScreen(directed, {}, {}, {}, speaking = step < 4 || step == 5, viseme = 2)
                    "menu" -> MenuScreen(false, {}, {}, {})
                    "help" -> Box(Modifier.fillMaxSize()) { StudioBackground(); HelpOverlay {} }
                    "rounds" -> Box(Modifier.fillMaxSize()) { StudioBackground(); RoundPickOverlay({}, {}) }
                    else -> ShowScreen(snap, {}, {}, {}, speaking = speaking, viseme = viseme)
                }
            }
        }
    }
}
