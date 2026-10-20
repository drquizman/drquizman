package com.drquizman.core.i18n.de

/** Texte für den Wettbewerb „Puzzle“. */
object PuzzleLinesDe {
    const val TITLE = "Puzzle"
    const val SLOGAN = "Errate es zuerst — oder werde bedeckt"

    fun intro(): String =
        "Puzzle — wer zuerst richtig rät, gewinnt. Die Teile öffnen sich automatisch. " +
            "Richtig geraten — ein Viertel des Puzzles bedeckt jeden Gegner. Falsch — ein Viertel bedeckt dich. " +
            "Vier Viertel — und du bist aus dem Wettbewerb."

    fun correct(name: String): String = listOf(
        "$name hat es! Jeder Gegner bekommt ein Puzzleteil.",
        "$name war zuerst. Jeder Gegner bekommt ein Viertel.",
        "$name hat das Bild erkannt. Die anderen werden langsam bedeckt.",
        "Richtig, $name! Die Puzzles der anderen sind gerade gewachsen.",
    ).random()

    fun wrong(name: String): String = listOf(
        "$name liegt falsch — ein Viertel Puzzle auf dich!",
        "Nein! $name bedeckt sich selbst mit einem weiteren Teil.",
        "$name war zu schnell. Das Bild auf ihm wird größer.",
        "$name liegt falsch. Ein Viertel Puzzle — drauf.",
    ).random()

    fun timeout(): String = listOf(
        "Niemand hat gedrückt. Die Puzzles bleiben wie sie sind. Weiter.",
        "Stille. Die Teile haben sich geöffnet, aber niemand traut sich.",
        "Zeit abgelaufen, keine Antwort. Niemand bekommt etwas.",
    ).random()

    fun out(names: List<String>): String {
        if (names.isEmpty()) return ""
        if (names.size == 1) {
            val n = names.first()
            return listOf(
                "$n ist unter dem vierten Viertel verschwunden!",
                "Das Puzzle hat $n komplett bedeckt. Raus aus diesem Wettbewerb.",
                "$n ist aus dem Puzzle ausgeschieden.",
            ).random()
        }
        val joined = names.joinToString(" und ")
        return "Das Puzzle hat alle auf einmal bedeckt: $joined."
    }

    fun settle(lead: String, payouts: List<Pair<String, Int>>): String {
        val head = lead.trim()
        val prefix = if (head.isEmpty()) "" else "$head "
        if (payouts.isEmpty()) {
            return prefix + listOf(
                "Das Puzzle hat alle bedeckt. Der Pot bleibt im Haus.",
                "Niemand kam bis zum Ende. Keine Punkte fürs Puzzle.",
            ).random()
        }
        val bits = payouts.joinToString(", ") { "${it.first} +${it.second}" }
        return prefix + listOf(
            "Wer ohne zusätzliche Bilder geblieben ist, holt den Pot: $bits.",
            "Puzzle-Ergebnis: $bits.",
            "Ein sauberes Bild ist mehr wert. $bits.",
        ).random()
    }
}
