package com.drquizman.core

object Palette {
    const val StudioDeep = 0xFF14002B
    const val StudioMid = 0xFF3B0764
    const val Magenta = 0xFFFF2D7A
    const val Gold = 0xFFFFD24A
    const val Cyan = 0xFF3DFFF3
    const val Cream = 0xFFFFF6E8
    const val Ink = 0xFF1A0828
    const val AccentRed = 0xFFE31C3D

    val player = longArrayOf(
        0xFF2E6BFF,
        0xFFFF8A1F, // оранжевый
        0xFF2ECC5A, // зелёный
        0xFFFFE14A, // жёлтый
        0xFFFF5AD5, // розовый (5-й игрок)
        0xFF3DFFF3, // циан (6-й)
    )

    val answers = longArrayOf(Gold, Gold, Gold, Gold)
}
