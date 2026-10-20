package com.drquizman.tv.ui.studio

import com.drquizman.core.i18n.LocalizedText

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.drquizman.core.model.CompetitionType
import com.drquizman.core.model.HostMood
import com.drquizman.core.model.Phase
import com.drquizman.tv.R
import com.drquizman.tv.ui.TvColors
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.launch

private data class TitleSkin(
    val overlay: Color,
    val flashTint: Color,
    val cream: Color,
    val title: Color,
    val mid: Color,
    val hot: Color,
    val edge: Color,
    val sparkle: Color,
    val sparkleCount: Int,
) {
    companion object {
        fun of(type: CompetitionType?): TitleSkin = when (type) {
            CompetitionType.TRUE_FALSE -> TitleSkin(
                overlay = Color(0xCC061428),
                flashTint = Color(0xFF3DFFF3),
                cream = Color(0xFFE8FFFE),
                title = Color(0xFFE8FFFE),
                mid = Color(0xFF3DFFF3),
                hot = Color(0xFF2E6BFF),
                edge = Color(0xFF0A3A4A),
                sparkle = Color(0xFF3DFFF3),
                sparkleCount = 12,
            )
            CompetitionType.SPEED -> TitleSkin(
                overlay = Color(0xCC241000),
                flashTint = Color(0xFFFF8A1F),
                cream = Color(0xFFFFF7C2),
                title = TvColors.gold,
                mid = Color(0xFFFF8A1F),
                hot = Color(0xFF7A3A00),
                edge = Color(0xFF241000),
                sparkle = TvColors.gold,
                sparkleCount = 12,
            )
            CompetitionType.BOMB -> TitleSkin(
                overlay = Color(0xCC140008),
                flashTint = Color(0xFFE31C3D),
                cream = Color(0xFFFFF7C2),
                title = TvColors.gold,
                mid = Color(0xFFFF7A18),
                hot = Color(0xFFFF2D7A),
                edge = Color(0xFF7A0040),
                sparkle = Color(0xFFFF7A18),
                sparkleCount = 14,
            )
            CompetitionType.EAST -> TitleSkin(
                overlay = Color(0xCC1A0C04),
                flashTint = Color(0xFFFF8A1F),
                cream = Color(0xFFFFF6E8),
                title = TvColors.gold,
                mid = Color(0xFFFF8A1F),
                hot = Color(0xFF4A2808),
                edge = Color(0xFF1A0C04),
                sparkle = TvColors.gold,
                sparkleCount = 8,
            )
            CompetitionType.BETS -> TitleSkin(
                overlay = Color(0xCC061A12),
                flashTint = Color(0xFFFFD24A),
                cream = Color(0xFFFFF6E8),
                title = TvColors.gold,
                mid = Color(0xFF2ECC5A),
                hot = Color(0xFFE31C3D),
                edge = Color(0xFF0B3D2E),
                sparkle = TvColors.gold,
                sparkleCount = 12,
            )
            CompetitionType.PUZZLE -> TitleSkin(
                overlay = Color(0xCC120018),
                flashTint = Color(0xFF3DFFF3),
                cream = Color(0xFFFFF6E8),
                title = TvColors.gold,
                mid = Color(0xFFFF2D7A),
                hot = Color(0xFF3DFFF3),
                edge = Color(0xFF3A0048),
                sparkle = TvColors.gold,
                sparkleCount = 12,
            )
            CompetitionType.FINAL -> TitleSkin(
                overlay = Color(0xCC07140C),
                flashTint = Color(0xFF2ECC5A),
                cream = Color(0xFFC8FFD4),
                title = Color(0xFFC8FFD4),
                mid = Color(0xFF2ECC5A),
                hot = Color(0xFF0E5A2A),
                edge = Color(0xFF07140C),
                sparkle = Color(0xFF2ECC5A),
                sparkleCount = 5,
            )
            else -> TitleSkin(
                overlay = Color(0xCC14002B),
                flashTint = TvColors.magenta,
                cream = Color(0xFFFFF7C2),
                title = TvColors.gold,
                mid = Color(0xFFFF7A18),
                hot = Color(0xFFFF2D7A),
                edge = Color(0xFF7A0040),
                sparkle = TvColors.cyan,
                sparkleCount = 12,
            )
        }
    }
}

private fun emblemRes(type: CompetitionType?): Int = when (type) {
    CompetitionType.TRUE_FALSE -> R.drawable.title_emblem_truefalse
    CompetitionType.SPEED -> R.drawable.title_emblem_speed
    CompetitionType.BOMB -> R.drawable.title_emblem_bomb
    CompetitionType.WHO_IS_RIGHT -> R.drawable.title_emblem_who
    CompetitionType.EAST -> R.drawable.title_emblem_east
    CompetitionType.BETS -> R.drawable.title_emblem_bets
    CompetitionType.PUZZLE -> R.drawable.title_emblem_puzzle
    CompetitionType.FINAL -> R.drawable.title_emblem_final
    else -> R.drawable.title_emblem_first
}

@Composable
fun TitleBangOverlay(
    title: String,
    modifier: Modifier = Modifier,
    slogan: String = "",
    type: CompetitionType? = null,
) {
    val enter = remember { Animatable(0.86f) }
    val glow by rememberInfiniteTransition(label = "premium-title").animateFloat(
        0.45f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "title-glow",
    )
    LaunchedEffect(type, title) {
        enter.snapTo(0.86f)
        enter.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow))
    }
    Box(
        modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF07030E), Color(0xFF180925), Color(0xFF090311))),
        ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(TvColors.magenta.copy(0.13f * glow), size.minDimension * 0.62f, Offset(size.width * 0.16f, size.height * 0.43f))
            drawCircle(TvColors.gold.copy(0.09f * glow), size.minDimension * 0.55f, Offset(size.width * 0.82f, size.height * 0.38f))
            repeat(5) { i ->
                drawCircle(
                    color = TvColors.gold.copy(0.10f + i * 0.025f),
                    radius = size.minDimension * (0.26f + i * 0.055f),
                    center = Offset(size.width * 0.18f, size.height * 0.46f),
                    style = Stroke(width = 2.5f),
                )
            }
        }
        Row(Modifier.fillMaxSize().padding(horizontal = 34.dp, vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(0.34f).fillMaxHeight()) {
                HostCharacter(HostMood.HYPE, Modifier.fillMaxSize(), phase = Phase.TITLE)
            }
            Column(
                Modifier.weight(0.66f).graphicsLayer { scaleX = enter.value; scaleY = enter.value },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .background(Brush.horizontalGradient(listOf(TvColors.panel, TvColors.panelRaised)), RoundedCornerShape(16.dp))
                        .border(1.dp, TvColors.gold.copy(0.7f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 28.dp, vertical = 8.dp),
                ) {
                    Text(LocalizedText.get("TitleBang.001"), color = TvColors.goldSoft, fontSize = 17.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(16.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color(0xFFFFFBF4), Color(0xFFF3E5D2))), RoundedCornerShape(28.dp))
                        .border(3.dp, TvColors.gold, RoundedCornerShape(28.dp))
                        .padding(horizontal = 34.dp, vertical = 34.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(painterResource(emblemRes(type)), null, Modifier.size(106.dp), contentScale = ContentScale.Fit)
                    Text(title, color = TvColors.ink, fontSize = if (title.length > 18) 42.sp else 52.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 2)
                    if (slogan.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(slogan, color = TvColors.magenta, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2)
                    }
                }
            }
        }
    }
}

@Composable
fun TitleBangOverlayLegacy(
    title: String,
    modifier: Modifier = Modifier,
    slogan: String = "",
    type: CompetitionType? = null,
) {
    val skin = TitleSkin.of(type)
    val scale = remember { Animatable(0.08f) }
    val flash = remember { Animatable(1f) }
    val wobble = remember { Animatable(-16f) }
    val ring = remember { Animatable(0.2f) }
    val twinkle by rememberInfiniteTransition(label = "bang").animateFloat(
        0.45f,
        1f,
        infiniteRepeatable(tween(280), RepeatMode.Reverse),
        label = "spark",
    )
    LaunchedEffect(type, title) {
        scale.snapTo(0.08f)
        flash.snapTo(1f)
        wobble.snapTo(-16f)
        ring.snapTo(0.15f)
        launch {
            scale.animateTo(1.12f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMedium))
            scale.animateTo(0.96f, tween(140))
            scale.animateTo(1.03f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow))
            scale.animateTo(1f, tween(160))
        }
        launch { flash.animateTo(0f, tween(340, easing = FastOutLinearInEasing)) }
        launch {
            wobble.animateTo(5f, spring(dampingRatio = 0.32f, stiffness = Spring.StiffnessLow))
            wobble.animateTo(0f, tween(220))
        }
        launch { ring.animateTo(1f, tween(520, easing = FastOutLinearInEasing)) }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(TvColors.night),
        contentAlignment = Alignment.Center,
    ) {
        StudioBackground()
        Box(Modifier.fillMaxSize().background(TvColors.night.copy(0.48f)))
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val s = scale.value
                    scaleX = s
                    scaleY = s
                    rotationZ = wobble.value
                },
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val r = min(size.width, size.height) * 0.58f
            val seed = (type?.name ?: title).hashCode()

            drawRect(Color.White.copy(alpha = 0.55f * flash.value))
            drawRect(skin.flashTint.copy(alpha = 0.28f * flash.value))

            speedLines(center, r * 1.72f, Color.White.copy(alpha = 0.28f + 0.3f * flash.value), n = 22, width = 4.5f)
            speedLines(center, r * 1.42f, skin.hot.copy(alpha = 0.45f), offset = 0.11f, n = 18, width = 5.5f)

            val shock = 0.4f + ring.value * 0.85f
            drawPath(
                burstPath(center, 24, r * 0.52f * shock, r * 1.12f * shock, 0.2f, seed + 3),
                skin.hot.copy(alpha = 0.4f * (1f - ring.value * 0.45f)),
                style = Stroke(width = 12f, cap = StrokeCap.Round),
            )

            val outer = burstPath(center, 20, r * 0.62f, r, 0.2f, seed)
            drawPath(outer, skin.edge.copy(alpha = 0.7f))
            drawPath(
                burstPath(center, 20, r * 0.58f, r * 0.97f, 0.2f, seed),
                skin.hot,
            )
            drawPath(
                burstPath(center, 18, r * 0.5f, r * 0.84f, 0.16f, seed + 7),
                skin.mid,
            )
            val inner = burstPath(center, 16, r * 0.38f, r * 0.7f, 0.12f, seed * 13)
            drawPath(
                inner,
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, skin.cream, skin.title),
                    center = center,
                    radius = r * 0.7f,
                ),
            )
            halftone(center, r * 0.58f, TvColors.ink.copy(alpha = 0.16f))
            drawPath(outer, TvColors.ink, style = Stroke(width = 20f))
            drawPath(outer, Color.White.copy(alpha = 0.4f), style = Stroke(width = 7f))

            if (skin.sparkleCount > 0) {
                sparkles(center, r * 1.02f, twinkle, seed, skin.sparkle, skin.sparkleCount)
            }
        }

        Column(
            Modifier
                .graphicsLayer {
                    val s = scale.value
                    scaleX = s
                    scaleY = s
                    rotationZ = wobble.value * 0.28f
                }
                .padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(emblemRes(type)),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(176.dp),
            )
            ComicTitle(title, skin.title)
            if (slogan.isNotBlank()) {
                ComicSlogan(slogan, Color.White)
            }
        }
    }
}

@Composable
private fun ComicTitle(title: String, fill: Color) {
    val size = when {
        title.length > 20 -> 40.sp
        title.length > 12 -> 50.sp
        else -> 64.sp
    }
    // Leave enough room for the heavy font and its 4 dp comic outline.
    // With the old 1.14 multiplier the outline of adjacent wrapped lines
    // overlapped on devices with different font metrics.
    val line = (size.value * 1.32f).sp
    val offsets = listOf(
        -4 to 0, 4 to 0, 0 to -4, 0 to 4,
        -3 to -3, 3 to -3, -3 to 3, 3 to 3,
    )
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        offsets.forEach { (dx, dy) ->
            Text(
                title,
                modifier = Modifier
                    .offset(dx.dp, dy.dp)
                    .fillMaxWidth(),
                color = TvColors.ink,
                fontSize = size,
                lineHeight = line,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
        Text(
            title,
            modifier = Modifier
                .offset(0.dp, (-2).dp)
                .fillMaxWidth(),
            color = Color(0xFFFFF6E8),
            fontSize = size,
            lineHeight = line,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        Text(
            title,
            modifier = Modifier.fillMaxWidth(),
            color = fill,
            fontSize = size,
            lineHeight = line,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun ComicSlogan(slogan: String, fill: Color) {
    val offsets = listOf(-3 to 0, 3 to 0, 0 to -3, 0 to 3)
    Box(
        Modifier.padding(top = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        offsets.forEach { (dx, dy) ->
            Text(
                slogan,
                modifier = Modifier
                    .offset(dx.dp, dy.dp)
                    .widthIn(max = 760.dp),
                color = TvColors.ink,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
        Text(
            slogan,
            modifier = Modifier.widthIn(max = 760.dp),
            color = fill,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

private fun burstPath(
    center: Offset,
    spikes: Int,
    inner: Float,
    outer: Float,
    jag: Float,
    seed: Int,
): Path {
    val path = Path()
    val n = spikes * 2
    for (i in 0 until n) {
        val ang = (-Math.PI / 2.0 + i * Math.PI / spikes).toFloat()
        val wob = 1f + jag * sin(i * 1.618f + seed * 0.01f)
        val extra = if (i % 2 == 0) 1f + 0.1f * sin((seed + i) * 0.37f) else 1f
        val radius = (if (i % 2 == 0) outer else inner) * wob * extra
        val x = center.x + cos(ang) * radius
        val y = center.y + sin(ang) * radius
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun DrawScope.speedLines(
    center: Offset,
    length: Float,
    color: Color,
    offset: Float = 0f,
    n: Int = 14,
    width: Float = 7f,
) {
    for (i in 0 until n) {
        val ang = (i / n.toFloat() + offset) * 6.2831855f
        val inner = length * 0.38f
        val outer = length * (0.9f + 0.08f * sin(i * 2.1f))
        drawLine(
            color = color,
            start = Offset(center.x + cos(ang) * inner, center.y + sin(ang) * inner),
            end = Offset(center.x + cos(ang) * outer, center.y + sin(ang) * outer),
            strokeWidth = width,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.sparkles(
    center: Offset,
    radius: Float,
    twinkle: Float,
    seed: Int,
    alt: Color,
    count: Int,
) {
    for (i in 0 until count) {
        val ang = -1.5708f + i * 6.2831855f / count + 0.18f * sin(seed * 0.1f + i)
        val pos = Offset(center.x + cos(ang) * radius, center.y + sin(ang) * radius)
        val s = (16f + (i % 3) * 9f) * twinkle
        drawStar(pos, s, if (i % 2 == 0) Color.White else alt)
    }
}

private fun DrawScope.drawStar(c: Offset, size: Float, color: Color) {
    val path = Path()
    for (i in 0 until 8) {
        val ang = -1.5708f + i * 0.785398f
        val r = if (i % 2 == 0) size else size * 0.34f
        val x = c.x + cos(ang) * r
        val y = c.y + sin(ang) * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

private fun DrawScope.halftone(center: Offset, radius: Float, color: Color) {
    var y = center.y - radius
    var row = 0
    while (y <= center.y + radius) {
        var x = center.x - radius + if (row % 2 == 0) 0f else 8f
        while (x <= center.x + radius) {
            val dx = x - center.x
            val dy = y - center.y
            if (dx * dx + dy * dy <= radius * radius) {
                drawCircle(color, 3.1f, Offset(x, y))
            }
            x += 16f
        }
        y += 14f
        row++
    }
}

@Preview(name = "TitleBang.002", widthDp = 960, heightDp = 540)
@Composable
private fun TitleFirstPreview() {
    TitleBangOverlay(LocalizedText.get("TitleBang.003"), slogan = LocalizedText.get("TitleBang.004"), type = CompetitionType.FIRST)
}

@Preview(name = "TitleBang.005", widthDp = 960, heightDp = 540)
@Composable
private fun TitleTrueFalsePreview() {
    TitleBangOverlay(LocalizedText.get("TitleBang.006"), slogan = LocalizedText.get("TitleBang.007"), type = CompetitionType.TRUE_FALSE)
}

@Preview(name = "TitleBang.008", widthDp = 960, heightDp = 540)
@Composable
private fun TitleSpeedPreview() {
    TitleBangOverlay(LocalizedText.get("TitleBang.009"), slogan = LocalizedText.get("TitleBang.010"), type = CompetitionType.SPEED)
}

@Preview(name = "TitleBang.011", widthDp = 960, heightDp = 540)
@Composable
private fun TitleBombPreview() {
    TitleBangOverlay(LocalizedText.get("TitleBang.012"), slogan = LocalizedText.get("TitleBang.013"), type = CompetitionType.BOMB)
}

@Preview(name = "TitleBang.014", widthDp = 960, heightDp = 540)
@Composable
private fun TitleWhoPreview() {
    TitleBangOverlay(LocalizedText.get("TitleBang.015"), slogan = LocalizedText.get("TitleBang.016"), type = CompetitionType.WHO_IS_RIGHT)
}

@Preview(name = "TitleBang.017", widthDp = 960, heightDp = 540)
@Composable
private fun TitleEastPreview() {
    TitleBangOverlay(LocalizedText.get("TitleBang.018"), slogan = LocalizedText.get("TitleBang.019"), type = CompetitionType.EAST)
}

@Preview(name = "TitleBang.020", widthDp = 960, heightDp = 540)
@Composable
private fun TitleBetsPreview() {
    TitleBangOverlay(LocalizedText.get("TitleBang.021"), slogan = LocalizedText.get("TitleBang.022"), type = CompetitionType.BETS)
}

@Preview(name = "TitleBang.023", widthDp = 960, heightDp = 540)
@Composable
private fun TitlePuzzlePreview() {
    TitleBangOverlay(LocalizedText.get("TitleBang.024"), slogan = LocalizedText.get("TitleBang.025"), type = CompetitionType.PUZZLE)
}

@Preview(name = "TitleBang.026", widthDp = 960, heightDp = 540)
@Composable
private fun TitleFinalPreview() {
    TitleBangOverlay(LocalizedText.get("TitleBang.027"), slogan = LocalizedText.get("TitleBang.028"), type = CompetitionType.FINAL)
}
