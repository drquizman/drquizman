package com.drquizman.core.i18n.de

/** Texte für den Wettbewerb „Bombe“. */
object BombLinesDe {
    const val TITLE = "Bombe"
    const val SLOGAN = "Weitergeben oder explodieren"

    fun intro(): String =
        "Ein Wettbewerb wie Folter — Bombe! Wer die Bombe hält, antwortet! " +
            "Richtig — das gefährliche Spielzeug weitergeben. " +
            "Nicht rechtzeitig weitergegeben oder zu oft falsch — boooom!"

    fun yourTurn(name: String): String =
        listOf(
            "$name, du hast die Bombe!",
            "Vorsicht, $name, sie tickt!",
            "$name, nicht träumen — das Spielzeug ist gefährlich!",
        ).random()

    fun passed(from: String, to: String): String =
        listOf(
            "$from hat rechtzeitig weitergegeben! Jetzt hat $to die Bombe.",
            "Puh, $from hat es geschafft. $to, fang!",
            "Übergabe geschafft! $to, jetzt bist du mit Entschärfen dran.",
        ).random()

    fun strike(name: String, left: Int): String =
        if (left <= 1) {
            listOf(
                "Falsch, $name! Noch ein Fehler — und boom.",
                "$name, daneben! Die Lunte wird kürzer, denk schnell.",
            ).random()
        } else {
            "Falsch, $name! Die Bombe bleibt bei dir."
        }

    fun exploded(name: String, penalty: Int): String =
        listOf(
            "Booooom! $name hat es nicht geschafft. Minus $penalty!",
            "Explosion! $name, ein kleines Andenken: minus $penalty.",
            "Die Bombe verzeiht niemandem. $name verliert $penalty Punkte.",
        ).random()

    fun survived(name: String): String =
        "$name hat richtig geantwortet. Die Bombe ist vorerst ruhig. Neue Frage!"
}
