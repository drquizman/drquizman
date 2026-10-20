package com.drquizman.core.i18n.de

/** Texte für den Wettbewerb „Wer hat recht!?“. */
object WhoIsRightLinesDe {
    const val TITLE = "Wer hat recht!?"
    const val SLOGAN = "Sieben Richtige — und du hast recht"

    fun intro(): String =
        "Wer hat recht!? Die Antwortzeit spielt keine Rolle. Wer zuerst sieben richtige Antworten hat, " +
            "gewinnt und bekommt die größte Belohnung! Alle anderen erhalten hundert für jede richtige Antwort."

    fun race(leaderName: String?, leaderMarks: Int, atSix: List<String>, goal: Int): String {
        if (atSix.size >= 2) {
            return "${atSix.joinToString(" und ")} haben schon sechs! Wer holt zuerst Nummer sieben?"
        }
        if (atSix.size == 1) {
            return "${atSix.first()} hat schon sechs! Noch eine — und der Pot gehört ihm!"
        }
        if (leaderName != null && leaderMarks in 1 until goal) {
            return listOf(
                "$leaderName führt gerade — $leaderMarks von $goal.",
                "$leaderName sammelt weiter richtige Antworten. Schon $leaderMarks!",
                "Das Rennen bis sieben. $leaderName liegt vorn.",
            ).random()
        }
        return listOf(
            "Noch setzt sich niemand ab. Weiter!",
            "Sieben Richtige — und der Wettbewerb gehört dir. Los!",
            "Die Zeit ist egal. Nur Recht haben zählt!",
        ).random()
    }

    fun jackpot(name: String, prize: Int): String =
        listOf(
            "Sieben von sieben! $name holt $prize!",
            "Wer hat recht? $name hat recht! Der Pot: $prize.",
            "$name war zuerst bei sieben Richtigen. Plus $prize!",
        ).random()

    fun capOver(name: String?, perCorrect: Int): String =
        if (name == null) {
            "Wettbewerb vorbei. Jeder bekommt $perCorrect für jede richtige Antwort."
        } else {
            "Niemand hat sieben erreicht. $name führt, aber ohne sieben kein Jackpot — nur $perCorrect pro richtiger Antwort."
        }
}
