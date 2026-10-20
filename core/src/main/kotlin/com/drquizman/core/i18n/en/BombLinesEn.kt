package com.drquizman.core.i18n.en

/** Lines for the “Bomb” contest from Quizman/host.txt and GDD. */
object BombLinesEn {
    const val TITLE = "Bomb"
    const val SLOGAN = "Pass it or explode"

    fun intro(): String =
        "A contest that is pure torture — Bomb! Whoever holds the bomb answers! " +
            "Correct answer — pass the dangerous toy on. " +
            "Fail to pass it or make too many mistakes — boooom!"

    fun yourTurn(name: String): String =
        listOf(
            "$name, you have the bomb!",
            "Careful, $name, it is ticking!",
            "$name, don’t daydream — that toy is dangerous!",
        ).random()

    fun passed(from: String, to: String): String =
        listOf(
            "$from passed it in time! Now $to has the bomb.",
            "Phew, $from made it. $to, catch!",
            "The pass worked! $to, your turn to defuse it.",
        ).random()

    fun strike(name: String, left: Int): String =
        if (left <= 1) {
            listOf(
                "Wrong, $name! One more mistake — and boom.",
                "$name, you missed! The fuse is getting shorter, think fast.",
            ).random()
        } else {
            "Wrong, $name! The bomb stays with you."
        }

    fun exploded(name: String, penalty: Int): String =
        listOf(
            "Booooom! $name didn’t make it. Minus $penalty!",
            "Explosion! $name, a little souvenir: minus $penalty.",
            "The bomb forgives no one. $name loses $penalty points.",
        ).random()

    fun survived(name: String): String =
        "$name answered correctly. The bomb is quiet for now. New question!"
}
