package com.drquizman.tv.ui

import com.drquizman.core.i18n.LocalizedText

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

fun joinQrBitmap(url: String, size: Int = 512): Bitmap {
    val hints = mapOf(
        EncodeHintType.MARGIN to 4,
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
    )
    val matrix = QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, size, size, hints)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val on = android.graphics.Color.BLACK
    val off = android.graphics.Color.WHITE
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        val row = y * size
        for (x in 0 until size) {
            pixels[row + x] = if (matrix.get(x, y)) on else off
        }
    }
    bmp.setPixels(pixels, 0, size, 0, 0, size, size)
    return bmp
}

@Composable
fun JoinQr(url: String, modifier: Modifier = Modifier) {
    val bmp = remember(url) { runCatching { joinQrBitmap(url) }.getOrNull() }
    if (bmp == null) return
    Image(
        bitmap = bmp.asImageBitmap(),
        contentDescription = LocalizedText.get("JoinQr.001"),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(5.dp),
        contentScale = ContentScale.Fit,
    )
}
