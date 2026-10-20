package com.drquizman.core.i18n.de

/** Texte für den Wettbewerb „Der Osten ist eine heikle Sache“. */
object EastLinesDe {
    const val TITLE = "Der Osten ist eine heikle Sache"
    const val SLOGAN = "Je schneller, desto fetter"

    fun intro(): String =
        "Antworte so schnell du kannst! Jede Sekunde verschwinden Punkte! " +
            "Die Antworten stehen von rechts nach links — der Osten ist eine heikle Sache!"

    fun results(scored: List<Pair<String, Int>>): String {
        if (scored.isEmpty()) {
            return listOf(
                "Niemand konnte es lesen. Der Osten war wohl zu heikel!",
                "Rechts nach links ist nicht links nach rechts. Null Punkte.",
                "Alle daneben. Und die Punkte schmolzen jede Sekunde!",
            ).random()
        }
        if (scored.size == 1) {
            val (name, prize) = scored.first()
            return listOf(
                "$name hat es von rechts nach links gelesen! Plus $prize!",
                "Nur $name hat es geschafft. Nimmt $prize.",
                "$name, scharfe Augen! Plus $prize.",
            ).random()
        }
        val bits = scored
            .sortedByDescending { it.second }
            .joinToString(", ") { "${it.first} +${it.second}" }
        return listOf(
            "Geschafft: $bits.",
            "Gelesen: $bits.",
        ).random()
    }
}
