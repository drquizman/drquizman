package com.drquizman.tv.ui

import com.drquizman.core.i18n.LocalizedText

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.drquizman.core.model.CompetitionType
import com.drquizman.core.model.GameSnapshot
import com.drquizman.core.model.Phase
import com.drquizman.core.model.QuestionView
import com.drquizman.tv.ui.studio.BombBurstOverlay
import com.drquizman.tv.ui.studio.BombFlightOverlay
import com.drquizman.tv.ui.studio.HostCharacter
import com.drquizman.tv.ui.studio.DirectedHostStage
import com.drquizman.tv.ui.studio.HostStageLayer
import com.drquizman.tv.ui.studio.PlayerPods
import com.drquizman.tv.ui.studio.PuzzleRevealMask
import com.drquizman.tv.ui.studio.StudioBackground
import com.drquizman.tv.ui.studio.SwampFloor
import com.drquizman.tv.ui.studio.TitleBangOverlayLegacy
import com.drquizman.tv.ui.studio.bombShakeOffset

@Composable
fun ShowScreen(
    snap: GameSnapshot,
    onSkip: () -> Unit,
    onAgain: () -> Unit,
    onMenu: () -> Unit,
    speaking: Boolean = false,
    viseme: Int = 0,
) {
    DirectedHostStage(snap, speaking, viseme) {
        val bombRound = snap.competition == CompetitionType.BOMB
        val bombAnchors = remember { mutableStateMapOf<String, Offset>() }
        var bombSettledId by remember { mutableStateOf<String?>(null) }
        var stageInWindow by remember { mutableStateOf(Offset.Zero) }
        var stageSize by remember { mutableStateOf(IntSize.Zero) }
        val bombFlying = bombRound &&
            snap.bombHolderId != null &&
            !snap.bombExploded &&
            snap.bombHolderId != bombSettledId
        val shakeX = bombShakeOffset(snap.bombExploded)
        val finalRound = snap.competition == CompetitionType.FINAL && snap.phase != Phase.GAME_OVER
        val swamp = if (!finalRound) {
            0f
        } else {
            val n = snap.players.size.coerceAtLeast(1)
            val sunk = snap.players.count { it.eliminated }
            0.32f + 0.55f * (sunk.toFloat() / n)
        }
        Box(
            Modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    stageInWindow = it.positionInWindow()
                    stageSize = it.size
                },
        ) {
            StudioBackground(swamp = swamp)
            if (finalRound) SwampFloor(swamp)
            // The actor can extend outside its slot; all game panels stay in front.
            HostStageLayer()
            Column(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationX = shakeX },
            ) {
                TopBar(snap, onMenu)
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (snap.phase) {
                        Phase.LOBBY -> LobbyBody(snap, speaking, viseme)
                        Phase.TITLE -> Box(Modifier.fillMaxSize())
                        Phase.INTRO -> IntroBody(snap, onSkip, speaking, viseme)
                        Phase.TOPIC -> TopicBody(snap, onSkip, speaking, viseme)
                        Phase.BET -> BetBody(snap, onSkip, speaking, viseme)
                        Phase.QUESTION, Phase.REVEAL -> QuestionBody(snap, speaking, viseme)
                        Phase.GAME_OVER -> WinnerBody(snap, onAgain, onMenu, speaking, viseme)
                    }
                }
                PlayerPods(
                    snap.players,
                    Modifier.fillMaxWidth(),
                    compact = snap.phase == Phase.QUESTION || snap.phase == Phase.REVEAL ||
                        snap.phase == Phase.TOPIC || snap.phase == Phase.BET ||
                        snap.phase == Phase.GAME_OVER,
                    winnerId = snap.winnerId,
                    bombHolderId = snap.bombHolderId,
                    bombExploded = snap.bombExploded,
                    bombActive = bombRound && snap.phase != Phase.TITLE && snap.phase != Phase.INTRO,
                    bombFlying = bombFlying,
                    onBombAnchor = { id, pos -> bombAnchors[id] = pos },
                    finalRound = finalRound,
                    finalIntro = finalRound && (snap.phase == Phase.INTRO || snap.phase == Phase.TITLE),
                    finalCapMs = snap.finalTimeCapMs,
                    raceGoal = snap.contestMarksGoal,
                    betsRound = snap.competition == CompetitionType.BETS &&
                        (snap.phase == Phase.BET || snap.phase == Phase.QUESTION || snap.phase == Phase.REVEAL),
                    bettingOpen = snap.bettingOpen,
                    puzzleRound = snap.competition == CompetitionType.PUZZLE &&
                        snap.phase != Phase.TITLE && snap.phase != Phase.INTRO,
                )
            }
            BombBurstOverlay(snap.bombExploded)
            if (snap.phase == Phase.TITLE) {
                TitleBangOverlayLegacy(
                    title = snap.competitionTitle.ifBlank { LocalizedText.get("ShowScreen.001") },
                    slogan = snap.competitionSlogan,
                    type = snap.competition,
                )
            }
            BombFlightOverlay(
                holderId = snap.bombHolderId,
                exploded = snap.bombExploded,
                bombRound = bombRound,
                anchors = bombAnchors,
                parentInWindow = stageInWindow,
                parentSize = stageSize,
                onSettled = { bombSettledId = it },
            )
            if (snap.paused) {
                Box(
                    Modifier.fillMaxSize().background(TvColors.night.copy(0.85f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        Modifier.background(Brush.verticalGradient(listOf(TvColors.panelRaised, TvColors.panel)), RoundedCornerShape(28.dp))
                            .border(2.dp, TvColors.gold, RoundedCornerShape(28.dp)).padding(horizontal = 72.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(LocalizedText.get("ShowScreen.002"), color = TvColors.goldSoft, fontSize = 54.sp, fontWeight = FontWeight.Black)
                        Text(LocalizedText.get("ShowScreen.003"), color = TvColors.cream, fontSize = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(snap: GameSnapshot, onMenu: () -> Unit) {
    val menuFocus = remember { FocusRequester() }
    LaunchedEffect(snap.phase) { if (snap.phase == Phase.LOBBY) runCatching { menuFocus.requestFocus() } }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 10.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(TvColors.panel.copy(0.98f), TvColors.panelRaised.copy(0.96f), TvColors.panel.copy(0.98f)),
                ),
                RoundedCornerShape(18.dp),
            )
            .border(1.dp, TvColors.line, RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("DR. QUIZMAN", color = TvColors.goldSoft, fontWeight = FontWeight.Black, fontSize = 22.sp)
        if (snap.competitionTitle.isNotEmpty() && snap.phase != Phase.TITLE && snap.phase != Phase.GAME_OVER) {
            val topic = snap.chosenTopic.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
            Text(snap.competitionTitle + topic, modifier = Modifier.weight(1f).padding(horizontal = 20.dp), color = TvColors.cream, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        when {
            snap.phase == Phase.LOBBY ->
                ShowButton(LocalizedText.get("ShowScreen.004"), Modifier.focusRequester(menuFocus), onMenu)
            snap.phase == Phase.QUESTION || snap.phase == Phase.REVEAL || snap.phase == Phase.BET -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        when {
                            snap.competition == CompetitionType.FINAL -> LocalizedText.get("ShowScreen.005")
                            snap.phase == Phase.BET ->
                                LocalizedText.get("ShowScreen.006", (snap.questionIndex), (snap.questionTotal))
                            snap.contestMarksGoal > 0 ->
                                LocalizedText.get("ShowScreen.007", (snap.questionIndex), (snap.contestMarksGoal))
                            snap.questionTotal in 1..20 ->
                                "${snap.questionIndex} / ${snap.questionTotal}"
                            else -> LocalizedText.get("ShowScreen.008", (snap.questionIndex))
                        },
                        color = TvColors.cream,
                        fontSize = 18.sp,
                    )
                    if (snap.showTimer) {
                        TimerRing(snap.timeLeftMs, snap.timeLimitMs, ringDp = 48)
                    } else if (snap.bombHolderId != null) {
                        val holder = snap.players.find { it.id == snap.bombHolderId }?.name.orEmpty()
                        Text(
                            if (holder.isBlank()) "💣" else "💣  $holder",
                            color = TvColors.gold,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                        )
                    }
                    if (snap.livePrize > 0) {
                        Text(
                            "${snap.livePrize}",
                            color = TvColors.gold,
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp,
                        )
                    }
                }
            }
            else -> Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
private fun LobbyBody(
    snap: GameSnapshot,
    speaking: Boolean,
    viseme: Int,
) {
    val readyN = snap.players.count { it.ready }
    val port = com.drquizman.core.net.Lan.PORT
    val emulator = snap.hostAddress.startsWith("10.0.2.")
    val url = when {
        emulator -> "http://127.0.0.1:$port/?pin=${snap.pin}"
        snap.hostAddress.isNotBlank() -> "http://${snap.hostAddress}:$port/?pin=${snap.pin}"
        else -> LocalizedText.get("ShowScreen.009", (port), (snap.pin))
    }
    Row(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        HostCharacter(snap.hostMood, Modifier.weight(0.18f).fillMaxHeight(), speaking, viseme, snap.phase)
        Column(Modifier.weight(0.82f).fillMaxHeight().padding(start = 14.dp)) {
            Text(LocalizedText.get("ShowScreen.010"), color = TvColors.goldSoft, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text(
                if (snap.wifiName.isNotBlank()) {
                    LocalizedText.get("ShowScreen.011", (snap.wifiName))
                } else {
                    LocalizedText.get("ShowScreen.012")
                },
                color = TvColors.cream,
                fontSize = 16.sp,
                fontWeight = if (snap.wifiName.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Box(
                    Modifier.weight(0.48f).fillMaxHeight()
                        .background(Color.White, RoundedCornerShape(22.dp))
                        .border(2.dp, TvColors.gold, RoundedCornerShape(22.dp)).padding(6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (snap.hostAddress.isNotBlank()) JoinQr(url, Modifier.fillMaxSize())
                    else Text(LocalizedText.get("ShowScreen.013"), color = TvColors.ink, fontSize = 20.sp)
                }
                Column(
                    Modifier.weight(0.52f).fillMaxHeight()
                        .background(Brush.verticalGradient(listOf(TvColors.panelRaised, TvColors.panel)), RoundedCornerShape(22.dp))
                        .border(1.dp, TvColors.gold, RoundedCornerShape(22.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(LocalizedText.get("ShowScreen.014"), color = TvColors.gold, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    Text(snap.pin, color = TvColors.goldSoft, fontSize = 52.sp, fontWeight = FontWeight.Black)
                    Text(if (snap.wifiName.isNotBlank()) "Wi-Fi · ${snap.wifiName}" else LocalizedText.get("ShowScreen.015"),
                        color = TvColors.cream, fontSize = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(url, color = TvColors.cream.copy(0.8f), fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (emulator) Text(LocalizedText.get("ShowScreen.016"), color = TvColors.gold, fontSize = 13.sp)
                    Spacer(Modifier.weight(1f))
                    Text(if (snap.players.isEmpty()) LocalizedText.get("ShowScreen.017") else LocalizedText.get("ShowScreen.018", (readyN), (snap.players.size)),
                        color = TvColors.goldSoft, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(LocalizedText.get("ShowScreen.019"), color = TvColors.cream, fontSize = 15.sp)
        }
    }
}

@Composable
private fun IntroBody(
    snap: GameSnapshot,
    onSkip: () -> Unit,
    speaking: Boolean,
    viseme: Int,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(snap.canSkipIntro) { if (snap.canSkipIntro) runCatching { focus.requestFocus() } }
    Row(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        HostCharacter(snap.hostMood, Modifier.weight(0.3f).fillMaxHeight(), speaking, viseme, snap.phase)
        Column(
            Modifier.weight(0.7f).fillMaxHeight()
                .background(Brush.verticalGradient(listOf(TvColors.panelRaised, TvColors.panel)), RoundedCornerShape(26.dp))
                .border(2.dp, TvColors.line, RoundedCornerShape(26.dp)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(LocalizedText.get("ShowScreen.020"), color = TvColors.gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(snap.competitionTitle, color = TvColors.goldSoft, fontSize = 36.sp,
                fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 2)
            Spacer(Modifier.height(12.dp))
            SpeechBubble(snap.hostLine, Modifier.weight(1f).fillMaxWidth(), maxLines = 5)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                TimerRing(snap.timeLeftMs, snap.timeLimitMs, ringDp = 48)
                if (snap.canSkipIntro) ShowButton(LocalizedText.get("ShowScreen.021"), Modifier.focusRequester(focus), onSkip)
            }
        }
    }
}

@Composable
private fun TopicBody(
    snap: GameSnapshot,
    onSkip: () -> Unit,
    speaking: Boolean,
    viseme: Int,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(snap.canSkipIntro) { if (snap.canSkipIntro) runCatching { focus.requestFocus() } }
    val chooser = snap.players.find { it.id == snap.topicChooserId }?.name
    Row(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HostCharacter(snap.hostMood, Modifier.weight(0.22f).fillMaxHeight(0.92f), speaking, viseme, snap.phase)
        Column(
            Modifier.weight(0.78f).fillMaxHeight().padding(start = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(LocalizedText.get("ShowScreen.022"), color = TvColors.gold, fontSize = 28.sp, fontWeight = FontWeight.Black)
                if (!chooser.isNullOrBlank()) {
                    Text(chooser.uppercase(), color = TvColors.magenta, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
                TimerRing(snap.timeLeftMs, snap.timeLimitMs, ringDp = 56)
            }
            Spacer(Modifier.height(8.dp))
            SpeechBubble(snap.hostLine, Modifier.fillMaxWidth(), maxLines = 2)
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                snap.topics.chunked(2).forEachIndexed { rowI, row ->
                    Row(
                        Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        row.forEachIndexed { colI, topic ->
                            val n = rowI * 2 + colI + 1
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(Brush.horizontalGradient(listOf(TvColors.panelRaised, TvColors.panel)), RoundedCornerShape(18.dp))
                                    .border(2.dp, TvColors.line, RoundedCornerShape(18.dp))
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "$n  ${topic.uppercase()}",
                                    color = TvColors.cream,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 26.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            if (snap.canSkipIntro) {
                Spacer(Modifier.height(8.dp))
                ShowButton(LocalizedText.get("ShowScreen.023"), Modifier.focusRequester(focus), onSkip)
            }
        }
    }
}

@Composable
private fun BetBody(
    snap: GameSnapshot,
    onSkip: () -> Unit,
    speaking: Boolean,
    viseme: Int,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(snap.canSkipIntro) { if (snap.canSkipIntro) runCatching { focus.requestFocus() } }
    val amounts = snap.betAmounts.ifEmpty { listOf(100, 200, 300, 400) }
    Row(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HostCharacter(snap.hostMood, Modifier.weight(0.22f).fillMaxHeight(0.92f), speaking, viseme, snap.phase)
        Column(
            Modifier.weight(0.78f).fillMaxHeight().padding(start = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (snap.bettingOpen) LocalizedText.get("ShowScreen.024") else LocalizedText.get("ShowScreen.025"),
                    color = TvColors.gold,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                )

            }
            Spacer(Modifier.height(8.dp))
            if (snap.hostLine.isNotBlank()) {
                SpeechBubble(snap.hostLine, Modifier.fillMaxWidth(), maxLines = 2)
                Spacer(Modifier.height(12.dp))
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFFBF4), Color(0xFFF3E5D2))), RoundedCornerShape(22.dp))
                    .border(2.dp, TvColors.goldSoft, RoundedCornerShape(22.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    snap.chosenTopic.ifBlank { LocalizedText.get("ShowScreen.026") }.uppercase(),
                    color = TvColors.ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 30.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                amounts.forEach { amount ->
                    val taken = snap.players.count { it.bet == amount }
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                if (snap.bettingOpen) TvColors.panelRaised else TvColors.panel.copy(0.55f),
                                RoundedCornerShape(16.dp),
                            )
                            .border(2.dp, if (snap.bettingOpen) TvColors.line else Color.White.copy(0.2f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                amount.toString(),
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 32.sp,
                            )
                            if (snap.bettingOpen && taken > 0) {
                                Text(
                                    "×$taken",
                                    color = TvColors.gold,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                )
                            }
                        }
                    }
                }
            }
            if (snap.canSkipIntro && !snap.bettingOpen) {
                Spacer(Modifier.height(8.dp))
                ShowButton(LocalizedText.get("ShowScreen.027"), Modifier.focusRequester(focus), onSkip)
            }
        }
    }
}

@Composable
private fun QuestionBody(
    snap: GameSnapshot,
    speaking: Boolean,
    viseme: Int,
) {
    val q = snap.question ?: return
    val revealing = snap.phase == Phase.REVEAL
    val puzzleProgress = if (!snap.puzzleAnswers || snap.timeLimitMs <= 0L) {
        1f
    } else {
        (1f - snap.timeLeftMs.toFloat() / snap.timeLimitMs.toFloat()).coerceIn(0f, 1f)
    }
    Row(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HostCharacter(snap.hostMood, Modifier.weight(0.24f).fillMaxHeight(0.96f), speaking, viseme, snap.phase)
        Column(Modifier.weight(0.76f).fillMaxHeight().padding(start = 14.dp)) {
            Box(
                Modifier
                    .background(TvColors.panelRaised, RoundedCornerShape(10.dp))
                    .border(1.dp, TvColors.line, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 5.dp),
            ) {
                val label = buildString {
                    append(q.topic.ifBlank { LocalizedText.get("ShowScreen.028") }.uppercase())
                    if (snap.reverseAnswers) append(LocalizedText.get("ShowScreen.029"))
                    if (snap.puzzleAnswers) append(LocalizedText.get("ShowScreen.030"))
                }
                Text(label, color = TvColors.goldSoft, fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 10.dp)
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFFFFFBF4), Color(0xFFF5EBDD))),
                        RoundedCornerShape(22.dp),
                    )
                    .border(2.dp, TvColors.goldSoft, RoundedCornerShape(22.dp))
                    .padding(horizontal = 26.dp, vertical = 18.dp),
            ) {
                Text(
                    q.text,
                    color = TvColors.ink,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (q.trueFalse) {
                Row(
                    Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    q.answers.forEachIndexed { i, ans ->
                        AnswerBar(
                            text = ans.uppercase(),
                            revealing = revealing,
                            isCorrect = q.correctIndex == i,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            rtl = snap.reverseAnswers,
                            puzzle = snap.puzzleAnswers,
                            puzzleSeed = q.id * 31 + i,
                            puzzleProgress = puzzleProgress,
                            puzzleCols = 6,
                            puzzleRows = 2,
                        )
                    }
                }
            } else {
                Column(
                    Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AnswerBar(q, 0, revealing, Modifier.weight(1f).fillMaxHeight(), snap.reverseAnswers, snap.puzzleAnswers, puzzleProgress)
                        if (q.answers.size > 1) AnswerBar(q, 1, revealing, Modifier.weight(1f).fillMaxHeight(), snap.reverseAnswers, snap.puzzleAnswers, puzzleProgress)
                    }
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (q.answers.size > 2) AnswerBar(q, 2, revealing, Modifier.weight(1f).fillMaxHeight(), snap.reverseAnswers, snap.puzzleAnswers, puzzleProgress)
                        if (q.answers.size > 3) AnswerBar(q, 3, revealing, Modifier.weight(1f).fillMaxHeight(), snap.reverseAnswers, snap.puzzleAnswers, puzzleProgress)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnswerBar(
    question: QuestionView,
    index: Int,
    revealing: Boolean,
    modifier: Modifier = Modifier,
    rtl: Boolean = false,
    puzzle: Boolean = false,
    puzzleProgress: Float = 1f,
) {
    val ans = question.answers.getOrNull(index) ?: return
    AnswerBar(
        text = ans,
        revealing = revealing,
        isCorrect = question.correctIndex == index,
        modifier = modifier,
        letter = ('A' + index).toString(),
        rtl = rtl,
        puzzle = puzzle,
        puzzleSeed = question.id * 31 + index,
        puzzleProgress = puzzleProgress,
        puzzleCols = 6,
        puzzleRows = 2,
    )
}

@Composable
private fun AnswerBar(
    text: String,
    revealing: Boolean,
    isCorrect: Boolean,
    modifier: Modifier = Modifier,
    letter: String? = null,
    rtl: Boolean = false,
    puzzle: Boolean = false,
    puzzleSeed: Int = 0,
    puzzleProgress: Float = 1f,
    puzzleCols: Int = 6,
    puzzleRows: Int = 2,
) {
    val dimWrong = revealing && !isCorrect
    val shape = RoundedCornerShape(16.dp)
    val shown = if (rtl) text.reversed() else text
    val accent = TvColors.gold

    Box(
        modifier
            .studioFinish(16.dp)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        if (revealing && isCorrect) TvColors.cream else TvColors.panelRaised.copy(alpha = if (dimWrong) 0.42f else 0.98f),
                        if (revealing && isCorrect) TvColors.gold else TvColors.panel.copy(alpha = if (dimWrong) 0.34f else 0.98f),
                    ),
                ),
            )
            .border(
                if (revealing && isCorrect) 4.dp else 2.dp,
                if (revealing && isCorrect) TvColors.goldSoft else accent.copy(if (dimWrong) 0.18f else 0.85f),
                shape,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (letter != null) {
                Box(
                    Modifier
                        .size(50.dp)
                        .background(accent.copy(0.14f), RoundedCornerShape(25.dp))
                        .border(1.dp, accent.copy(0.4f), RoundedCornerShape(25.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(letter, color = if (revealing && isCorrect) TvColors.ink else accent, fontSize = 27.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(12.dp))
            }
            Text(
                shown,
                modifier = Modifier.weight(1f),
                color = if (revealing && isCorrect) TvColors.ink else TvColors.cream.copy(if (dimWrong) 0.45f else 0.96f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                textAlign = if (rtl) TextAlign.End else TextAlign.Start,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (puzzle && !revealing) {
            PuzzleRevealMask(
                cols = puzzleCols,
                rows = puzzleRows,
                seed = puzzleSeed,
                progress = puzzleProgress,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun WinnerBody(
    snap: GameSnapshot,
    onAgain: () -> Unit,
    onMenu: () -> Unit,
    speaking: Boolean,
    viseme: Int,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val champ = snap.players.find { it.id == snap.winnerId }
    Row(
        Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HostCharacter(snap.hostMood, Modifier.weight(0.3f).fillMaxHeight(), speaking, viseme, snap.phase)
        Column(
            Modifier.weight(0.7f)
                .background(Brush.verticalGradient(listOf(TvColors.panelRaised, TvColors.panel)), RoundedCornerShape(28.dp))
                .border(2.dp, TvColors.goldSoft, RoundedCornerShape(28.dp)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(LocalizedText.get("ShowScreen.031"), color = TvColors.gold, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text(snap.winnerName?.uppercase() ?: "—", color = TvColors.goldSoft,
                fontWeight = FontWeight.Black, fontSize = 52.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (champ != null) Text(LocalizedText.get("ShowScreen.032", (champ.score)), color = TvColors.cream, fontSize = 24.sp)
            Spacer(Modifier.height(14.dp))
            SpeechBubble(snap.hostLine, Modifier.fillMaxWidth(), maxLines = 2)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ShowButton(LocalizedText.get("ShowScreen.033"), Modifier.focusRequester(focus), onAgain)
                ShowButton(LocalizedText.get("ShowScreen.034"), onClick = onMenu)
            }
        }
    }
}

@Composable
private fun TimerRing(left: Long, total: Long, ringDp: Int = 72) {
    val p by animateFloatAsState(
        if (total <= 0L) 0f else (left.toFloat() / total.toFloat()).coerceIn(0f, 1f),
        label = "timer",
    )
    val sec = (left / 1000f)
    val danger = sec <= 5f
    val accent = if (danger) TvColors.red else TvColors.gold
    Box(Modifier.size(ringDp.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = ringDp * 0.09f * density
            val inset = stroke / 2f
            drawCircle(Color(0xCC14002B))
            drawCircle(Color.White.copy(0.22f), style = Stroke(stroke))
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * p,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(this.size.width - stroke, this.size.height - stroke),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            sec.toInt().toString(),
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = (ringDp * 0.36).sp,
        )
    }
}
