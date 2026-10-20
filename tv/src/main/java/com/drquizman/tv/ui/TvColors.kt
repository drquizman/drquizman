package com.drquizman.tv.ui

import androidx.compose.ui.graphics.Color
import com.drquizman.core.Palette

fun Long.toCompose() = Color(this)

object TvColors {
    val deep = Color(0xFF160D19)
    val mid = Color(0xFF362032)
    val magenta = Color(0xFFD8A16C)
    val gold = Color(0xFFE9BD78)
    val cyan = Palette.Cyan.toCompose()
    val cream = Color(0xFFFFF0D8)
    val ink = Color(0xFF281525)
    val red = Palette.AccentRed.toCompose()
    val night = Color(0xFF0D0810)
    val panel = Color(0xFF211421)
    val panelRaised = Color(0xFF482D40)
    val goldSoft = Color(0xFFFFE3AD)
    val line = Color(0xBBDEAD6F)
    fun player(i: Int) = Palette.player[i.mod(Palette.player.size)].toCompose()
    fun answer(@Suppress("UNUSED_PARAMETER") i: Int) = gold
}
