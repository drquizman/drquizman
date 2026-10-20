package com.drquizman.tv.ui.studio

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import com.drquizman.tv.ui.TvColors
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min
import kotlin.random.Random

/** Доля плашки: и длина, и ширина ушка. */
private const val TAB_OF_BAR = 0.30f

/**
 * Ровная сетка. На общей грани — ушко 30% меньшей стороны плашки.
 * Случайно только направление замка. Сосед делит ту же грань.
 */
internal class PuzzleCut(
    val cols: Int,
    val rows: Int,
    seed: Int,
) {
    private val vTab = Array(rows) { IntArray((cols - 1).coerceAtLeast(0)) }
    private val hTab = Array((rows - 1).coerceAtLeast(0)) { IntArray(cols) }

    init {
        val rng = Random(seed)
        for (r in 0 until rows) {
            for (c in 0 until cols - 1) {
                vTab[r][c] = if (rng.nextBoolean()) 1 else -1
            }
        }
        for (r in 0 until rows - 1) {
            for (c in 0 until cols) {
                hTab[r][c] = if (rng.nextBoolean()) 1 else -1
            }
        }
    }

    fun path(col: Int, row: Int, width: Float, height: Float): Path {
        val cellW = width / cols
        val cellH = height / rows
        val x0 = col * cellW
        val x1 = x0 + cellW
        val y0 = row * cellH
        val y1 = y0 + cellH
        val tab = TAB_OF_BAR * min(width, height)
        val p = Path()
        p.moveTo(x0, y0)
        if (row == 0) p.lineTo(x1, y0)
        else appendTab(p, x0, y0, x1, y0, -hTab[row - 1][col], tab)
        if (col == cols - 1) p.lineTo(x1, y1)
        else appendTab(p, x1, y0, x1, y1, vTab[row][col], tab)
        if (row == rows - 1) p.lineTo(x0, y1)
        else appendTab(p, x1, y1, x0, y1, hTab[row][col], tab)
        if (col == 0) p.lineTo(x0, y0)
        else appendTab(p, x0, y1, x0, y0, -vTab[row][col - 1], tab)
        p.close()
        return p
    }

    fun order(extraSeed: Int): IntArray {
        val idx = IntArray(cols * rows) { it }
        idx.shuffle(Random(extraSeed))
        return idx
    }
}

/**
 * Ушко пазла: шейка прямыми линиями + круглая голова дугой.
 * Классическая форма, как у настоящих пазлов.
 */
private fun appendTab(
    path: Path,
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    tab: Int,
    size: Float,
) {
    if (tab == 0) {
        path.lineTo(x1, y1)
        return
    }
    val dx = x1 - x0
    val dy = y1 - y0
    val len = hypot(dx, dy).coerceAtLeast(1f)
    val ux = dx / len
    val uy = dy / len
    val nx = -uy * tab
    val ny = ux * tab
    val d = size.coerceAtMost(len * 0.62f)
    val headR = d * 0.264f        // радиус круглой головы
    val midX = (x0 + x1) / 2f
    val midY = (y0 + y1) / 2f
    // Центр окружности: на расстоянии R от грани (касается грани снизу)
    val hcX = midX + nx * headR
    val hcY = midY + ny * headR
    // Левая и правая точки окружности (на уровне грани, ±R вдоль грани)
    val nLX = midX - ux * headR
    val nLY = midY - uy * headR
    val nRX = midX + ux * headR
    val nRY = midY + uy * headR
    // Углы от центра к левой/правой точкам
    val angL = atan2(nLY - hcY, nLX - hcX)
    val angR = atan2(nRY - hcY, nRX - hcX)
    // Как в HTML: ccw = tab > 0, кратчайшая дуга в нужном направлении
    val ccw = tab > 0
    var sweep = angR - angL
    if (ccw) {
        if (sweep > 0) sweep -= 2f * Math.PI.toFloat()
    } else {
        if (sweep < 0) sweep += 2f * Math.PI.toFloat()
    }
    path.lineTo(nLX, nLY)
    path.arcTo(
        Rect(hcX - headR, hcY - headR, hcX + headR, hcY + headR),
        Math.toDegrees(angL.toDouble()).toFloat(),
        Math.toDegrees(sweep.toDouble()).toFloat(),
        false,
    )
    path.lineTo(x1, y1)
}

@Composable
fun PuzzleRevealMask(
    cols: Int,
    rows: Int,
    seed: Int,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val cut = remember(cols, rows, seed) { PuzzleCut(cols, rows, seed) }
    val order = remember(cut, seed) { cut.order(seed xor 0x9E3779B9.toInt()) }
    val total = cols * rows
    val openCount = (progress.coerceIn(0f, 1f) * total).toInt().coerceIn(0, total)
    Canvas(modifier.fillMaxSize()) {
        val opened = order.take(openCount).toHashSet()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val i = r * cols + c
                if (i in opened) continue
                val path = cut.path(c, r, size.width, size.height)
                drawPath(path, Color(0xF84A1658))
                drawPath(
                    path,
                    TvColors.gold.copy(alpha = 0.95f),
                    style = Stroke(width = 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
                drawPath(path, Color.White.copy(0.16f), style = Stroke(width = 1.3f))
            }
        }
    }
}

@Composable
fun PuzzleCoverMask(
    filled: Int,
    seed: Int,
    modifier: Modifier = Modifier,
    cols: Int = 2,
    rows: Int = 2,
    out: Boolean = false,
) {
    val cut = remember(cols, rows, seed) { PuzzleCut(cols, rows, seed) }
    val order = remember(cut, seed) { cut.order(seed) }
    val total = cols * rows
    val n = filled.coerceIn(0, total)
    var prev by remember(seed) { mutableIntStateOf(n) }
    val pop = remember(seed) { Animatable(1f) }
    LaunchedEffect(n, seed) {
        if (n > prev) {
            pop.snapTo(1.28f)
            pop.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 380f))
        } else {
            pop.snapTo(1f)
        }
        prev = n
    }
    val newest = if (n > 0) order[n - 1] else -1
    val popScale = pop.value
    Canvas(modifier.fillMaxSize()) {
        for (k in 0 until n) {
            val i = order[k]
            val c = i % cols
            val r = i / cols
            val path = cut.path(c, r, size.width, size.height)
            val s = if (i == newest) popScale else 1f
            val cx = (c + 0.5f) * size.width / cols
            val cy = (r + 0.5f) * size.height / rows
            scale(s, s, Offset(cx, cy)) {
                drawPath(path, Color(0xE61A0828))
                drawPath(path, TvColors.magenta.copy(alpha = 0.52f))
                drawPath(
                    path,
                    TvColors.gold.copy(alpha = 0.92f),
                    style = Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
        if (out) {
            drawRect(Color(0xCC0A0014).copy(alpha = 0.72f))
        }
    }
}
