package com.drquizman.core.i18n.de

/** Texte für den Wettbewerb „Einsätze geschlossen“. */
object BetsLinesDe {
    const val TITLE = "Einsätze geschlossen"
    const val SLOGAN = "Das Thema ist bekannt. Setzen oder schweigen"

    fun intro(): String =
        "Setze einen Betrag und beantworte die Frage. Richtige Antwort — du gewinnst deinen Einsatz dazu. " +
            "Falsch — du verlierst denselben Betrag. Jedes Mal ein neues Thema. Nicht rechtzeitig gesetzt — du setzt die Frage aus."

    fun announceTopic(topic: String, index: Int, total: Int): String = listOf(
        "Frage $index von $total. Das Thema ist $topic!",
        "Nächstes Thema: $topic. Gut zuhören und Einsätze bereithalten!",
        "Thema von Frage $index — $topic! Wer sicher ist, setzt groß.",
        "Achtung, Thema: $topic. Nach der Ansage habt ihr fünf Sekunden zum Setzen.",
    ).random()

    fun placeBets(): String = listOf(
        "Einsätze bitte! Hundert, zweihundert, dreihundert oder vierhundert. Fünf Sekunden!",
        "Einsätze geschlossen? Noch nicht — also los! Fünf Sekunden.",
        "Ich nehme Einsätze an. Fünf Sekunden, meine Damen und Herren!",
    ).random()

    fun results(
        won: List<Pair<String, Int>>,
        lost: List<Pair<String, Int>>,
        skipped: List<String>,
    ): String {
        val bits = mutableListOf<String>()
        if (won.isEmpty() && lost.isEmpty() && skipped.isNotEmpty()) {
            return listOf(
                "Niemand hat gesetzt. Die Frage ist ohne Spieler verbrannt!",
                "Keine Einsätze — niemand antwortet. Weiter.",
            ).random()
        }
        if (won.isNotEmpty()) {
            bits += won.joinToString(", ") { "${it.first} +${it.second}" }
                .let { "Den Pot geholt: $it." }
        }
        if (lost.isNotEmpty()) {
            bits += lost.joinToString(", ") { "${it.first} −${it.second}" }
                .let { "Der Pot kassiert: $it." }
        }
        if (skipped.isNotEmpty() && skipped.size <= 3) {
            bits += "Ohne Einsatz: ${skipped.joinToString(", ")}."
        }
        if (bits.isEmpty()) {
            return if (won.isNotEmpty()) "Die Einsätze haben sich gelohnt!" else "Das Haus liegt vorn."
        }
        return bits.joinToString(" ")
    }
}
