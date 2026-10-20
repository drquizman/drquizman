package com.drquizman.core.i18n.en

/** Lines for the “The East Is a Delicate Matter” contest from Quizman/host.txt and GDD. */
object EastLinesEn {
    const val TITLE = "The East Is a Delicate Matter"
    const val SLOGAN = "The faster, the fatter"

    fun intro(): String =
        "Answer as fast as you can! Points disappear every second! " +
            "The answers are written right to left — the East is a delicate matter!"

    fun results(scored: List<Pair<String, Int>>): String {
        if (scored.isEmpty()) {
            return listOf(
                "Nobody managed to read it. The East turned out to be too delicate!",
                "Right to left is not the same as left to right. Zero points.",
                "Everyone missed. And the points melted away every second!",
            ).random()
        }
        if (scored.size == 1) {
            val (name, prize) = scored.first()
            return listOf(
                "$name read it right to left! Plus $prize!",
                "Only $name made it. Takes $prize.",
                "$name, sharp eyes! Plus $prize.",
            ).random()
        }
        val bits = scored
            .sortedByDescending { it.second }
            .joinToString(", ") { "${it.first} +${it.second}" }
        return listOf(
            "Made it: $bits.",
            "Read it: $bits.",
        ).random()
    }
}
