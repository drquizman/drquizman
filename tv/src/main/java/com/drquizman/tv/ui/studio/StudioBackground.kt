package com.drquizman.tv.ui.studio

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.drquizman.tv.R

/** Shared painted studio with subtle live lighting. */
@Composable
fun StudioBackground(modifier: Modifier = Modifier, swamp: Float = 0f) {
    val light by rememberInfiniteTransition(label = "studio-light").animateFloat(
        0.02f, 0.08f, infiniteRepeatable(tween(4200), RepeatMode.Reverse), label = "warm-light",
    )
    Box(modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.studio_warm), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Color(0xFF100914).copy(alpha = 0.15f))
            drawRect(Brush.radialGradient(listOf(Color(0xFFFFCA83).copy(light), Color.Transparent),
                center = Offset(size.width * 0.25f, size.height * 0.25f), radius = size.width * 0.65f))
            if (swamp > 0f) drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF182A14).copy(swamp * 0.55f))))
        }
    }
}
