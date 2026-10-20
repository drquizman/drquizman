package com.drquizman.tv.ui.studio

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.drquizman.tv.ui.TvColors
import kotlin.math.sin
import kotlin.random.Random

private data class SwampBubble(
    val x: Float,
    val y: Float,
    val r: Float,
    val phase: Float,
    val speed: Float,
)

@Composable
fun SwampFloor(intensity: Float, modifier: Modifier = Modifier) {
    if (intensity <= 0.01f) return
    val t = rememberInfiniteTransition(label = "swamp")
    val wave by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
        label = "wave",
    )
    val bubbles = remember {
        List(18) {
            SwampBubble(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                r = 4f + Random.nextFloat() * 10f,
                phase = Random.nextFloat(),
                speed = 0.35f + Random.nextFloat() * 0.7f,
            )
        }
    }
    val a = intensity.coerceIn(0f, 1f)
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height((118 + a * 70).dp),
        ) {
            val h = size.height
            val rise = sin(wave * 6.2831855f).toFloat() * 8f
            drawRect(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color(0x662A4A18).copy(alpha = 0.35f * a),
                        Color(0xCC1A2E0C).copy(alpha = 0.72f * a),
                        Color(0xEE0C1808).copy(alpha = 0.9f * a),
                    ),
                ),
                topLeft = Offset(0f, rise * 0.4f),
                size = Size(size.width, h),
            )
            val surfaceY = h * 0.22f + rise
            drawLine(
                Color(0xFF7DFF4A).copy(alpha = 0.18f * a),
                Offset(0f, surfaceY),
                Offset(size.width, surfaceY),
                strokeWidth = 3f,
            )
            bubbles.forEach { b ->
                val lift = ((wave * b.speed + b.phase) % 1f)
                val cx = b.x * size.width
                val cy = h - lift * h * 0.85f
                val alpha = (0.15f + 0.45f * (1f - lift)) * a
                drawCircle(Color(0xFFB6FF6A).copy(alpha = alpha), radius = b.r, center = Offset(cx, cy))
                drawCircle(
                    Color.White.copy(alpha = alpha * 0.45f),
                    radius = b.r * 0.35f,
                    center = Offset(cx - b.r * 0.25f, cy - b.r * 0.3f),
                )
            }
        }
    }
}

@Composable
fun TimeFlask(
    fill: Float,
    color: Color,
    frozen: Boolean,
    danger: Boolean,
    modifier: Modifier = Modifier,
) {
    val p by animateFloatAsState(fill.coerceIn(0f, 1.12f), tween(160), label = "flask-fill")
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (w < 2f || h < 2f) return@Canvas
        val inset = w * 0.12f
        val tube = CornerRadius(w * 0.42f, w * 0.42f)
        val liquidColor = when {
            danger && !frozen -> TvColors.red
            frozen -> color.copy(alpha = 0.85f)
            else -> color
        }
        drawRoundRect(
            Color(0x3314002B),
            topLeft = Offset(inset * 0.4f, inset),
            size = Size(w - inset * 0.8f, h - inset),
            cornerRadius = tube,
        )
        val innerL = inset
        val innerT = inset * 1.4f
        val innerW = w - inset * 2f
        val innerH = h - inset * 2.2f
        val liquidH = innerH * p.coerceAtMost(1f)
        if (liquidH > 1.5f) {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(liquidColor.copy(alpha = 0.55f), liquidColor.copy(alpha = 0.95f)),
                ),
                topLeft = Offset(innerL, innerT + innerH - liquidH),
                size = Size(innerW, liquidH),
                cornerRadius = CornerRadius(w * 0.32f, w * 0.32f),
            )
        }
        drawRoundRect(
            Color.White.copy(alpha = 0.22f),
            topLeft = Offset(innerL + innerW * 0.18f, innerT),
            size = Size(innerW * 0.22f, innerH),
            cornerRadius = CornerRadius(w * 0.12f),
        )
        drawRoundRect(
            Color.White.copy(alpha = if (frozen) 0.85f else 0.55f),
            topLeft = Offset(inset * 0.4f, inset),
            size = Size(w - inset * 0.8f, h - inset),
            cornerRadius = tube,
            style = Stroke(width = if (frozen) w * 0.12f else w * 0.08f),
        )
        if (frozen) {
            drawRoundRect(
                Color.White.copy(alpha = 0.22f),
                topLeft = Offset(inset * 0.4f, inset),
                size = Size(w - inset * 0.8f, h - inset),
                cornerRadius = tube,
            )
            val cx = w * 0.5f
            val cy = h * 0.38f
            drawLine(Color.White.copy(0.8f), Offset(cx - 5f, cy), Offset(cx + 5f, cy), 2.2f)
            drawLine(Color.White.copy(0.8f), Offset(cx, cy - 5f), Offset(cx, cy + 5f), 2.2f)
        }
        drawRoundRect(
            TvColors.gold,
            topLeft = Offset(w * 0.18f, 0f),
            size = Size(w * 0.64f, inset * 1.35f),
            cornerRadius = CornerRadius(4f, 4f),
        )
    }
}
