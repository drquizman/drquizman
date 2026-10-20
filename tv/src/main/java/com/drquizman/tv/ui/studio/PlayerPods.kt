package com.drquizman.tv.ui.studio

import com.drquizman.core.i18n.LocalizedText

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.drquizman.tv.ui.studioFinish
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.drquizman.core.engine.GameEngine
import com.drquizman.core.mechanic.puzzle.PuzzleMechanic
import com.drquizman.core.model.PlayerState
import com.drquizman.tv.ui.TvColors
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable
fun PlayerPods(
    players: List<PlayerState>,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    winnerId: String? = null,
    bombHolderId: String? = null,
    bombExploded: Boolean = false,
    bombActive: Boolean = false,
    bombFlying: Boolean = false,
    onBombAnchor: (playerId: String, windowCenter: Offset) -> Unit = { _, _ -> },
    finalRound: Boolean = false,
    finalIntro: Boolean = false,
    finalCapMs: Long = 0,
    raceGoal: Int = 0,
    betsRound: Boolean = false,
    bettingOpen: Boolean = false,
    puzzleRound: Boolean = false,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = if (compact) 8.dp else 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(GameEngine.MAX_PLAYERS) { i ->
            val p = players.find { it.colorIndex == i }
            val slot = if (p == null) {
                Modifier.width(if (compact) 64.dp else 80.dp)
            } else {
                Modifier.weight(1f).widthIn(max = 360.dp)
            }
            Pod(
                index = i,
                player = p,
                compact = compact,
                modifier = slot,
                winner = p?.id == winnerId,
                hasBomb = p?.id == bombHolderId,
                bombExploded = bombExploded && p?.id == bombHolderId,
                bombActive = bombActive,
                bombFlying = bombFlying,
                onBombAnchor = onBombAnchor,
                finalRound = finalRound,
                finalIntro = finalIntro,
                finalCapMs = finalCapMs,
                raceGoal = raceGoal,
                betsRound = betsRound,
                bettingOpen = bettingOpen,
                puzzleRound = puzzleRound,
            )
        }
    }
}

@Composable
private fun Pod(
    index: Int,
    player: PlayerState?,
    compact: Boolean,
    modifier: Modifier = Modifier,
    winner: Boolean = false,
    hasBomb: Boolean = false,
    bombExploded: Boolean = false,
    bombActive: Boolean = false,
    bombFlying: Boolean = false,
    onBombAnchor: (playerId: String, windowCenter: Offset) -> Unit = { _, _ -> },
    finalRound: Boolean = false,
    finalIntro: Boolean = false,
    finalCapMs: Long = 0,
    raceGoal: Int = 0,
    betsRound: Boolean = false,
    bettingOpen: Boolean = false,
    puzzleRound: Boolean = false,
) {
    val color = TvColors.player(index)
    val eliminated = player?.eliminated == true
    val frozen = finalRound && player?.finalFrozen == true && !eliminated
    val danger = finalRound && !eliminated && !frozen && (player?.finalTimeMs ?: Long.MAX_VALUE) <= 8_000L
    val raceOn = raceGoal > 0 && player != null && !puzzleRound
    val raceWon = raceOn && (player?.contestMarks ?: 0) >= raceGoal
    val puzzleOn = puzzleRound && player != null
    val puzzleOut = puzzleOn && (player?.contestMarks ?: 0) >= PuzzleMechanic.MAX_QUARTERS
    val betOn = betsRound && (player?.bet ?: 0) > 0
    val scale by animateFloatAsState(
        when {
            bombExploded -> 1.08f
            hasBomb -> 1.06f
            frozen -> 1.03f
            raceWon -> 1.05f
            betOn -> 1.04f
            player?.answered == true -> 1.04f
            else -> 1f
        },
        label = "pod",
    )
    val sink by animateFloatAsState(if (finalRound && eliminated) 26f else 0f, tween(700), label = "sink")
    val fade by animateFloatAsState(
        when {
            eliminated || puzzleOut -> 0.48f
            player?.connected == false -> 0.4f
            else -> 1f
        },
        tween(500),
        label = "fade",
    )
    val blast = remember { Animatable(0f) }
    LaunchedEffect(bombExploded) {
        if (bombExploded) {
            blast.snapTo(0f)
            blast.animateTo(1f, tween(520))
            blast.animateTo(0f, tween(280))
        } else {
            blast.snapTo(0f)
        }
    }
    val empty = player == null
    val h = when {
        compact && empty -> 72.dp
        compact && raceOn -> 94.dp
        compact -> 84.dp
        empty -> 88.dp
        raceOn -> 106.dp
        else -> 96.dp
    }
    val portrait = if (compact) 40.dp else 46.dp
    val sideHud = bombActive || finalRound || betsRound || puzzleOn
    Box(
        modifier
            .graphicsLayer {
                translationY = sink
                alpha = fade
            }
            .scale(scale)
            .height(h)
            .studioFinish(20.dp)
            .shadow(12.dp, RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        lerp(TvColors.panel, color, if (empty) 0.10f else 0.68f),
                        lerp(TvColors.panel, color, if (empty) 0.12f else 0.42f),
                        TvColors.panel.copy(alpha = if (empty) 0.4f else 0.98f),
                    ),
                ),
                RoundedCornerShape(20.dp),
            )
            .border(
                when {
                    frozen || raceWon -> 4.dp
                    hasBomb || danger -> 5.dp
                    else -> 3.dp
                },
                when {
                    eliminated -> Color.White.copy(0.2f)
                    bombExploded -> TvColors.red
                    danger -> TvColors.red
                    frozen -> Color.White
                    raceWon -> TvColors.gold
                    hasBomb -> TvColors.gold
                    else -> color.copy(if (empty) 0.18f else 0.9f)
                },
                RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        if (player == null) {
            Text(LocalizedText.get("PlayerPods.001", (index + 1)), color = Color.White.copy(0.35f), fontSize = 16.sp, modifier = Modifier.align(Alignment.Center))
        } else {
            Row(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .padding(bottom = if (raceOn) 16.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(portrait)
                        .graphicsLayer { alpha = if (eliminated) 0.4f else 1f }
                        .border(3.dp, Color.White, CircleShape)
                        .padding(3.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    PlayerFace(
                        avatarIndex = player.avatarIndex,
                        mood = playerFaceMood(player, winner, bombExploded),
                        contentDescription = player.name,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f).padding(end = if (finalRound) 18.dp else 0.dp)) {
                    Text(
                        player.name.uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        podValueText(player, finalRound, finalIntro),
                        color = Color.White.copy(0.92f),
                        fontWeight = FontWeight.Bold,
                        fontSize = if (raceOn) 22.sp else 26.sp,
                    )
                }
                val mark = when {
                    bombExploded || hasBomb || finalRound || raceOn || betsRound || puzzleOn -> ""
                    eliminated -> "×"
                    player.lastCorrect == true -> "✓"
                    player.lastCorrect == false -> "✗"
                    player.answered -> "●"
                    player.ready -> "★"
                    else -> ""
                }
                // Answer feedback also drives the portrait; keep the six-seat rail legible.
            }
            if (raceOn) {
                RacePips(
                    filled = player.contestMarks,
                    goal = raceGoal,
                    justGained = player.lastCorrect == true,
                    close = player.contestMarks == raceGoal - 1,
                    won = raceWon,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                )
            }
            if (betsRound && player != null) {
                val bet = player.bet
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .graphicsLayer { translationX = 8f; translationY = -6f }
                        .size(52.dp)
                        .shadow(6.dp, CircleShape)
                        .background(
                            when {
                                bet > 0 -> TvColors.gold
                                player.lastCorrect != null -> Color(0xFF4A2040)
                                else -> Color(0xCC14002B)
                            },
                            CircleShape,
                        )
                        .border(2.dp, Color.White.copy(if (bet > 0) 0.9f else 0.35f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        when {
                            bet > 0 -> bet.toString()
                            bettingOpen -> "?"
                            else -> "—"
                        },
                        color = if (bet > 0) TvColors.ink else Color.White.copy(0.7f),
                        fontWeight = FontWeight.Black,
                        fontSize = if (bet > 0) 14.sp else 18.sp,
                    )
                }
            }
            if (bombActive) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .graphicsLayer { translationX = 10f; translationY = -8f }
                        .size(56.dp)
                        .onGloballyPositioned { coords ->
                            val center = coords.localToWindow(
                                Offset(coords.size.width / 2f, coords.size.height / 2f),
                            )
                            onBombAnchor(player.id, center)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        bombExploded -> Text(
                            "💥",
                            fontSize = BombPlaqueSp.sp,
                            modifier = Modifier.graphicsLayer {
                                scaleX = 1f + blast.value * 0.55f
                                scaleY = 1f + blast.value * 0.55f
                                rotationZ = blast.value * 18f
                            },
                        )
                        hasBomb && !bombFlying -> Text("💣", fontSize = BombPlaqueSp.sp)
                    }
                }
            }
            if (finalRound) {
                val cap = finalCapMs.coerceAtLeast(1L)
                val targetFill = if (eliminated) 0f else (player.finalTimeMs.toFloat() / cap).coerceIn(0f, 1.12f)
                var introArmed by remember(player.id) { mutableStateOf(finalIntro) }
                LaunchedEffect(finalIntro, player.id) {
                    if (finalIntro) {
                        introArmed = true
                        delay(380)
                        introArmed = false
                    } else {
                        introArmed = false
                    }
                }
                val fill by animateFloatAsState(
                    if (introArmed) 0f else targetFill,
                    tween(if (finalIntro) 820 else 160),
                    label = "flask",
                )
                TimeFlask(
                    fill = fill,
                    color = color,
                    frozen = frozen,
                    danger = danger,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(18.dp)
                        .fillMaxHeight()
                        .padding(vertical = 2.dp),
                )
            }
            if (puzzleOn) {
                PuzzleCoverMask(
                    filled = player.contestMarks,
                    seed = player.id.hashCode(),
                    out = puzzleOut,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp)),
                )
            }
        }
    }
}

private fun playerFaceMood(
    player: PlayerState,
    winner: Boolean,
    bombExploded: Boolean,
): PlayerFaceMood = when {
    winner -> PlayerFaceMood.YES
    player.eliminated || bombExploded -> PlayerFaceMood.NO
    player.lastCorrect == true -> PlayerFaceMood.YES
    player.lastCorrect == false -> PlayerFaceMood.NO
    player.answered -> PlayerFaceMood.TALK
    else -> PlayerFaceMood.IDLE
}

@Composable
private fun podValueText(player: PlayerState, finalRound: Boolean, finalIntro: Boolean): String {
    val sec = ((player.finalTimeMs + 999L) / 1000L).toInt().coerceAtLeast(0)
    val introNum = remember(player.id) { Animatable(player.score.toFloat()) }
    var showingScore by remember(player.id) { mutableStateOf(finalIntro) }
    LaunchedEffect(finalIntro, player.id, player.score, sec) {
        if (finalRound && finalIntro) {
            showingScore = true
            introNum.snapTo(player.score.toFloat())
            delay(380)
            introNum.animateTo(sec.toFloat(), tween(820))
            showingScore = false
        } else {
            showingScore = false
        }
    }
    return when {
        player.eliminated -> LocalizedText.get("PlayerPods.002")
        !finalRound -> "${player.score}"
        showingScore -> introNum.value.roundToInt().toString()
        else -> sec.toString()
    }
}

@Composable
private fun RacePips(
    filled: Int,
    goal: Int,
    justGained: Boolean,
    close: Boolean,
    won: Boolean,
    modifier: Modifier = Modifier,
) {
    val pulse = rememberInfiniteTransition(label = "pip-pulse")
    val glow by pulse.animateFloat(
        0.5f,
        1f,
        infiniteRepeatable(tween(720, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow",
    )
    val pop = remember { Animatable(1f) }
    LaunchedEffect(filled, justGained) {
        if (justGained && filled > 0) {
            pop.snapTo(1f)
            pop.animateTo(1.4f, tween(130))
            pop.animateTo(1f, tween(200))
        } else {
            pop.snapTo(1f)
        }
    }
    val popScale = pop.value
    Canvas(modifier.height(14.dp).fillMaxWidth()) {
        if (goal <= 0) return@Canvas
        val n = goal
        val gap = 3.5.dp.toPx()
        val d = ((size.width - gap * (n - 1)) / n).coerceIn(7.dp.toPx(), 12.dp.toPx())
        val total = d * n + gap * (n - 1)
        var x = (size.width - total) / 2f
        val cy = size.height / 2f
        repeat(n) { i ->
            val on = i < filled
            val last = i == n - 1
            val newest = justGained && i == filled - 1
            val r = (d / 2f) * (if (newest) popScale else 1f)
            val alpha = if (!on && close && last) glow else 1f
            val c = Offset(x + d / 2f, cy)
            if (on) {
                drawCircle(TvColors.gold.copy(alpha = if (won) 1f else 0.95f), radius = r, center = c)
                drawCircle(
                    Color.White.copy(0.55f),
                    radius = r * 0.32f,
                    center = Offset(c.x - r * 0.22f, c.y - r * 0.22f),
                )
            } else {
                drawCircle(Color(0x5514002B), radius = r, center = c)
                drawCircle(
                    (if (last) TvColors.gold else Color.White).copy(alpha = 0.5f * alpha),
                    radius = r,
                    center = c,
                    style = Stroke(width = 1.7.dp.toPx()),
                )
            }
            x += d + gap
        }
    }
}
