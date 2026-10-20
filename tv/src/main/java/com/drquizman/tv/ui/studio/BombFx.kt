package com.drquizman.tv.ui.studio

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.drquizman.tv.ui.TvColors
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private data class BurstSpark(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val warm: Boolean,
)

internal const val BombPlaqueSp = 56
private const val BombAirSp = 86
private const val PlaqueScale = BombPlaqueSp / BombAirSp.toFloat()

@Composable
fun BombBurstOverlay(exploded: Boolean, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    var playing by remember { mutableStateOf(false) }
    val sparks = remember { mutableStateOf(emptyList<BurstSpark>()) }

    LaunchedEffect(exploded) {
        if (!exploded) return@LaunchedEffect
        sparks.value = List(28) { i ->
            BurstSpark(
                angle = (i / 28f) * 6.2831855f + Random.nextFloat() * 0.35f,
                speed = 0.22f + Random.nextFloat() * 0.55f,
                size = 10f + Random.nextFloat() * 22f,
                warm = Random.nextBoolean(),
            )
        }
        playing = true
        progress.snapTo(0f)
        progress.animateTo(1f, tween(920, easing = FastOutLinearInEasing))
        playing = false
    }

    if (!playing && progress.value == 0f) return
    val p = progress.value
    Canvas(modifier.fillMaxSize()) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.58f
        val center = Offset(cx, cy)
        val flash = (1f - p).coerceIn(0f, 1f)
        drawRect(Color(0xFFFFE7A0).copy(alpha = 0.55f * flash))
        drawRect(Color(0xFFFF3B1A).copy(alpha = 0.28f * flash))
        val core = 40f + p * size.minDimension * 0.42f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f * flash),
                    TvColors.gold.copy(alpha = 0.75f * flash),
                    Color(0xFFFF6A00).copy(alpha = 0.45f * (1f - p)),
                    Color.Transparent,
                ),
                center = center,
                radius = core.coerceAtLeast(1f),
            ),
            radius = core,
            center = center,
        )
        listOf(0.18f, 0.34f, 0.52f).forEach { ringAt ->
            val ringP = ((p - ringAt + 0.18f) / 0.55f).coerceIn(0f, 1f)
            if (ringP <= 0f) return@forEach
            drawCircle(
                color = Color.White.copy(alpha = 0.55f * (1f - ringP)),
                radius = 30f + ringP * size.minDimension * 0.48f,
                center = center,
                style = Stroke(width = 10f * (1f - ringP * 0.7f)),
            )
        }
        sparks.value.forEach { spark ->
            val dist = p * spark.speed * size.minDimension
            val pos = Offset(cx + cos(spark.angle) * dist, cy + sin(spark.angle) * dist)
            val alpha = (1f - p).coerceIn(0f, 1f)
            drawCircle(
                color = if (spark.warm) TvColors.gold.copy(alpha = alpha) else Color(0xFFFF4D2A).copy(alpha = alpha),
                radius = spark.size * (1.15f - p * 0.7f),
                center = pos,
            )
        }
    }
}

@Composable
fun BombFlightOverlay(
    holderId: String?,
    exploded: Boolean,
    bombRound: Boolean,
    anchors: Map<String, Offset>,
    parentInWindow: Offset,
    parentSize: IntSize,
    onSettled: (String?) -> Unit,
) {
    val x = remember { Animatable(0f) }
    val y = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    val rot = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    var lastId by remember { mutableStateOf<String?>(null) }
    val half = with(LocalDensity.current) { (BombAirSp / 2f).sp.toPx() }
    val anchorsNow = rememberUpdatedState(anchors)
    val parentNow = rememberUpdatedState(parentInWindow to parentSize)

    LaunchedEffect(holderId, exploded, bombRound) {
        if (!bombRound || holderId == null) {
            lastId = null
            onSettled(null)
            alpha.snapTo(0f)
            return@LaunchedEffect
        }
        if (exploded) {
            lastId = holderId
            onSettled(holderId)
            alpha.snapTo(0f)
            return@LaunchedEffect
        }
        if (holderId == lastId) {
            onSettled(holderId)
            return@LaunchedEffect
        }

        val (destWindow, origin, size) = snapshotFlow {
            val dest = anchorsNow.value[holderId]
            val (originPx, sizePx) = parentNow.value
            if (dest == null || sizePx.width <= 0) null else Triple(dest, originPx, sizePx)
        }.filterNotNull().first()
        val dest = destWindow - origin
        val fromWindow = lastId?.let { anchorsNow.value[it] }
        lastId = holderId

        if (fromWindow == null) {
            val spawn = Offset(
                if (size.width > 0) size.width / 2f else dest.x,
                dest.y - 240f,
            )
            x.snapTo(spawn.x)
            y.snapTo(spawn.y - 170f)
            scale.snapTo(0.25f)
            rot.snapTo(-18f)
            alpha.snapTo(1f)
            y.animateTo(spawn.y, tween(420, easing = LinearOutSlowInEasing))
            scale.animateTo(1.35f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.42f))
            scale.animateTo(1f, tween(160, easing = FastOutSlowInEasing))
            rot.animateTo(0f, tween(180))
            delay(70)
            coroutineScope {
                launch { flyAxis(x, spawn.x, dest.x, 420) }
                launch { flyAxis(y, spawn.y, dest.y, 420) }
                launch { scale.animateTo(PlaqueScale, tween(420, easing = FastOutSlowInEasing)) }
            }
            scale.animateTo(PlaqueScale * 1.12f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMedium))
            scale.animateTo(PlaqueScale, tween(120))
        } else {
            val start = fromWindow - origin
            val apex = Offset(
                (start.x + dest.x) / 2f,
                min(start.y, dest.y) - 220f,
            )
            val lift = Offset(start.x + (apex.x - start.x) * 0.2f, start.y - 110f)
            x.snapTo(start.x)
            y.snapTo(start.y)
            scale.snapTo(PlaqueScale)
            rot.snapTo(0f)
            alpha.snapTo(1f)
            coroutineScope {
                launch { flyAxis(x, start.x, lift.x, 240) }
                launch { flyAxis(y, start.y, lift.y, 240) }
                launch { scale.animateTo(1.05f, tween(240, easing = FastOutLinearInEasing)) }
                launch { rot.animateTo(22f, tween(240)) }
            }
            coroutineScope {
                launch { flyAxis(x, lift.x, apex.x, 200) }
                launch { flyAxis(y, lift.y, apex.y, 200) }
            }
            delay(40)
            coroutineScope {
                launch { flyAxis(x, apex.x, dest.x, 400) }
                launch { flyAxis(y, apex.y, dest.y, 400) }
                launch { scale.animateTo(PlaqueScale, tween(400, easing = FastOutSlowInEasing)) }
                launch { rot.animateTo(-10f, tween(320)) }
            }
            rot.animateTo(0f, tween(140))
            scale.animateTo(PlaqueScale * 1.14f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
            scale.animateTo(PlaqueScale, tween(120))
        }
        onSettled(holderId)
        alpha.snapTo(0f)
    }

    if (alpha.value == 0f) return
    Box(Modifier.fillMaxSize()) {
        Text(
            "💣",
            fontSize = BombAirSp.sp,
            modifier = Modifier.graphicsLayer {
                translationX = x.value - half
                translationY = y.value - half
                scaleX = scale.value
                scaleY = scale.value
                rotationZ = rot.value
                this.alpha = alpha.value
                transformOrigin = TransformOrigin.Center
            },
        )
    }
}

private suspend fun flyAxis(axis: Animatable<Float, *>, from: Float, to: Float, ms: Int) {
    axis.snapTo(from)
    axis.animateTo(to, tween(ms, easing = FastOutSlowInEasing))
}

@Composable
fun bombShakeOffset(exploded: Boolean): Float {
    val shake = remember { Animatable(0f) }
    val density = LocalDensity.current.density
    LaunchedEffect(exploded) {
        if (!exploded) {
            shake.snapTo(0f)
            return@LaunchedEffect
        }
        repeat(7) { i ->
            shake.animateTo(if (i % 2 == 0) 14f else -14f, tween(38))
        }
        shake.animateTo(0f, tween(80))
    }
    return shake.value * density
}
