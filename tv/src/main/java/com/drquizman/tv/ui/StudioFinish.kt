package com.drquizman.tv.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Fine inset highlight, shared by the studio's physical display panels. */
fun Modifier.studioFinish(radius: Dp = 18.dp): Modifier = drawWithContent {
    drawContent()
    val inset = 3.dp.toPx()
    if (size.width > inset * 2 && size.height > inset * 2) {
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFFFFEAC6).copy(0.52f), Color.White.copy(0.04f), Color(0xFFB27A39).copy(0.35f))),
            topLeft = Offset(inset, inset),
            size = Size(size.width - inset * 2, size.height - inset * 2),
            cornerRadius = CornerRadius((radius.toPx() - inset).coerceAtLeast(0f)),
            style = Stroke(0.7.dp.toPx()),
        )
    }
}
