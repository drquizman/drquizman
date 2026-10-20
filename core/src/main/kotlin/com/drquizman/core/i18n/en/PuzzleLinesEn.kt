package com.drquizman.core.i18n.en

/** Lines for the “Puzzle” contest from Quizman/host.txt and GDD. */
object PuzzleLinesEn {
    const val TITLE = "Puzzle"
    const val SLOGAN = "Guess first — or get covered"

    fun intro(): String =
        "Puzzle — the first correct guess wins. The pieces reveal themselves automatically. " +
            "Guess correctly — a quarter of the puzzle covers every rival. Wrong — a quarter covers you. " +
            "Four quarters — and you are out of the contest."

    fun correct(name: String): String = listOf(
        "$name got it! Every rival gets a puzzle piece.",
        "$name was first. Every opponent gets a quarter.",
        "$name completed the picture. The others start getting covered.",
        "Correct, $name! Everyone else’s puzzle just grew.",
    ).random()

    fun wrong(name: String): String = listOf(
        "$name missed — one quarter of the puzzle goes on them!",
        "No! $name covers themself with another piece.",
        "$name rushed it. Their picture gets bigger.",
        "$name is wrong. One quarter of the puzzle — on them.",
    ).random()

    fun timeout(): String = listOf(
        "Nobody pressed anything. The puzzles stay where they are. Moving on.",
        "Silence. The pieces opened up, but nobody dared answer.",
        "Time ran out with no answer. Nobody gets anything.",
    ).random()

    fun out(names: List<String>): String {
        if (names.isEmpty()) return ""
        if (names.size == 1) {
            val n = names.first()
            return listOf(
                "$n disappeared under the fourth quarter!",
                "The puzzle covered $n completely. They are out of this contest.",
                "$n is eliminated from the puzzle.",
            ).random()
        }
        val joined = names.joinToString(" and ")
        return "The puzzle covered them all at once: $joined."
    }

    fun settle(lead: String, payouts: List<Pair<String, Int>>): String {
        val head = lead.trim()
        val prefix = if (head.isEmpty()) "" else "$head "
        if (payouts.isEmpty()) {
            return prefix + listOf(
                "The puzzle covered everyone. The pot stays with the house.",
                "Nobody made it to the end. No points for the puzzle.",
            ).random()
        }
        val bits = payouts.joinToString(", ") { "${it.first} +${it.second}" }
        return prefix + listOf(
            "Those who stayed clear of extra pictures take the pot: $bits.",
            "Puzzle result: $bits.",
            "A clean picture is worth more. $bits.",
        ).random()
    }
}
