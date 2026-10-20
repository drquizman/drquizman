package com.drquizman.core.i18n.en

/** Lines for the “Who Is Right!?” contest from Quizman/host.txt and GDD. */
object WhoIsRightLinesEn {
    const val TITLE = "Who Is Right!?"
    const val SLOGAN = "Seven correct — and you’re right"

    fun intro(): String =
        "Who Is Right!? Answer time does not matter. The first player to get seven correct answers " +
            "wins and gets the biggest reward! Everyone else gets one hundred for each correct answer."

    fun race(leaderName: String?, leaderMarks: Int, atSix: List<String>, goal: Int): String {
        if (atSix.size >= 2) {
            return "${atSix.joinToString(" and ")} already have six! Who gets the seventh first?"
        }
        if (atSix.size == 1) {
            return "${atSix.first()} already has six! One more — and the pot is theirs!"
        }
        if (leaderName != null && leaderMarks in 1 until goal) {
            return listOf(
                "$leaderName leads for now — $leaderMarks of $goal.",
                "$leaderName keeps collecting correct answers. Already $leaderMarks!",
                "The race to seven. $leaderName is in front.",
            ).random()
        }
        return listOf(
            "Nobody is running away with it yet. Keep going!",
            "Seven correct — and the contest is yours. Go!",
            "Time does not matter. Only being right matters!",
        ).random()
    }

    fun jackpot(name: String, prize: Int): String =
        listOf(
            "Seven out of seven! $name takes $prize!",
            "Who is right? $name is right! The pot is $prize.",
            "$name was the first to seven correct. Plus $prize!",
        ).random()

    fun capOver(name: String?, perCorrect: Int): String =
        if (name == null) {
            "Contest over. Everyone gets $perCorrect for each correct answer."
        } else {
            "Nobody reached seven. $name leads, but no seven means no jackpot — only $perCorrect for each correct answer."
        }
}
