package com.drquizman.core.i18n.en

/** Lines for the “Bets Are Closed” contest from Quizman/host.txt and GDD. */
object BetsLinesEn {
    const val TITLE = "Bets Are Closed"
    const val SLOGAN = "The topic is known. Bet or stay quiet"

    fun intro(): String =
        "Place a bet and answer the question. Correct answer — you gain your bet amount. " +
            "Wrong answer — you lose the same amount. A new topic every time. No bet in time — you sit the question out."

    fun announceTopic(topic: String, index: Int, total: Int): String = listOf(
        "Question $index of $total. The topic is $topic!",
        "Next topic: $topic. Listen carefully and get your bets ready!",
        "Topic for question $index — $topic! If you are confident, bet big.",
        "Attention, the topic is $topic. After the announcement — five seconds to bet.",
    ).random()

    fun placeBets(): String = listOf(
        "Place your bets! One hundred, two hundred, three hundred or four hundred. Five seconds!",
        "Bets closed? Not yet — so move! Five seconds.",
        "Taking bets. Five seconds, ladies and gentlemen!",
    ).random()

    fun results(
        won: List<Pair<String, Int>>,
        lost: List<Pair<String, Int>>,
        skipped: List<String>,
    ): String {
        val bits = mutableListOf<String>()
        if (won.isEmpty() && lost.isEmpty() && skipped.isNotEmpty()) {
            return listOf(
                "Nobody bet. The question burned without players!",
                "No bets — nobody answers. Moving on.",
            ).random()
        }
        if (won.isNotEmpty()) {
            bits += won.joinToString(", ") { "${it.first} +${it.second}" }
                .let { "Took the pot: $it." }
        }
        if (lost.isNotEmpty()) {
            bits += lost.joinToString(", ") { "${it.first} −${it.second}" }
                .let { "The pot took: $it." }
        }
        if (skipped.isNotEmpty() && skipped.size <= 3) {
            bits += "No bet: ${skipped.joinToString(", ")}."
        }
        if (bits.isEmpty()) {
            return if (won.isNotEmpty()) "The bets paid off!" else "The house is ahead."
        }
        return bits.joinToString(" ")
    }
}
