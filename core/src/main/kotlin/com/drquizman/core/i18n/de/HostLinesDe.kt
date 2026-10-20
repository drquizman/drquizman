package com.drquizman.core.i18n.de

import com.drquizman.core.mechanic.MechanicCatalog
import com.drquizman.core.model.CompetitionType

/** Texte des Moderators. Wettbewerbtexte kommen aus dem Mechanik-Katalog. */
object HostLinesDe {
    fun welcome(): String = listOf(
        "Hallo an unsere geliebten und hochgeschätzten Spieler und Zuschauer der medizinisch-intellektuellen Show! Heute finden wir heraus, wer von uns am schnellsten, klügsten und schärfsten ist... Aber warum rede ich ständig von mir!? Sehen wir lieber, wer heute um den großen Quizpreis kämpft.",
        "Hallo, meine intellektuellen Freunde! Wir stürzen uns heute in ein großes Abenteuer auf den Wellen von Wissen, Schlagfertigkeit und bei manchen auch etwas Glück! Handys raus und rein ins Spiel!",
        "Endlich darf ich euch wieder begrüßen, meine lieben Quizmaniacs! Heute ist ein historischer Tag — Behandlung gegen praktisch alles, live und intellektuell! Macht mit, wir fangen an!",
    ).random()

    fun waiting(): String = waitingPool.random()

    private val waitingPool = listOf(
        "Worauf wartet ihr? Kommt rein — gute Laune ist garantiert!",
        "Wie lange warten wir noch auf alle? Sind die etwa alle beim Gesundheitscheck?",
        "Uhhh... sieht so aus, als könnte der Abend etwas langweilig werden.",
        "Wo seid ihr, meine Patienten? Die Klinik ist offen und die Fernbedienungen warten!",
        "Solange niemand beitritt, kann ich mit mir selbst spielen. Aber das ist keine Show mehr — das ist Sprechstunde.",
        "Keine Scheu, Handys raus — heute behandeln wir Intelligenz, ganz ohne Termin!",
    )

    fun lobbyChatter(count: Int, ready: Int, previous: String = ""): String {
        val extra = if (count > 0 && ready in 1 until count) {
            listOf("Bereit: $ready von $count. Wir warten auf den Rest!")
        } else {
            emptyList()
        }
        val options = waitingPool + extra
        return options.filter { it != previous }.randomOrNull() ?: options.random()
    }

    fun playerJoined(name: String, first: Boolean): String {
        if (first) return "Und der erste Patient, der heute zur Behandlung erscheint, ist $name"
        return listOf(
            "Nächster Spieler — $name",
            "Und jetzt begrüßen wir $name",
            "$name, sicher, dass du nicht in den falschen Raum gekommen bist? Na gut, sehen wir, was du kannst.",
            "Ooooh, $name, mit dir habe ich hier wirklich nicht gerechnet!",
            "Hallo, $name!",
            "$name, schön dich zu sehen!",
            "$name, willkommen!",
            "Nicht schüchtern sein, $name, komm dazu!",
            "$name, haben wir uns heute schon gesehen? Oder verwechsle ich da etwas?",
            "Haltet meine Glatze fest — schaut mal, wer da ist! $name!",
            "Begrüßen wir den nächsten Spieler mit donnerndem Applaus! $name",
            "$name, ich hoffe, wir kommen heute durch, ohne dass jemand die Nerven verliert...",
        ).random()
    }

    fun lobbyIdle(count: Int): String = lobbyWaiting(count, 0)

    fun lobbyWaiting(count: Int, ready: Int): String = when {
        count == 0 || ready == 0 -> waiting()
        ready < count -> "Bereit: $ready von $count. Wir warten auf den Rest!"
        else -> matchStart()
    }

    fun matchStart(): String =
        "Uuuund alle sind da, wir können mit der Behandlung beginnen... ich meine natürlich mit dem Kampf! Ein Kampf, bei dem alle Gewinner sein werden, aber natürlich nur einer ganz vorne stehen kann! Den Namen erfahren wir sehr bald. Und jetzt — los!"

    fun matchStartWithGrant(points: Int): String =
        "Heute gibt es kein Startvermögen — jeder bekommt sofort $points Punkte, damit es wenigstens etwas zu verlieren gibt. Los geht’s!"

    fun intro(type: CompetitionType): String =
        MechanicCatalog[type]?.intro?.invoke() ?: "Die Show geht weiter!"

    fun chooseTopic(name: String, lastPlace: Boolean): String = if (lastPlace) {
        listOf(
            "Damit sich der letzte Platz nicht ganz so schlimm anfühlt, darfst du, $name, das Thema wählen.",
            "Wer hinten liegt, wählt das Rundenthema. $name, das bist du.",
        ).random()
    } else {
        listOf(
            "Wem vertrauen wir diesmal die Themenwahl an!? $name, versuch du es!",
            "Normalerweise hilft die Themenwahl zu einem besseren Ergebnis. Mal sehen, wie es bei dir läuft, $name — wähle ein Thema.",
        ).random()
    }

    fun topicPicked(topic: String): String =
        "Rundenthema — $topic! Los geht’s!"

    fun titles(type: CompetitionType): String =
        MechanicCatalog[type]?.title.orEmpty()

    fun allCorrect(): String = listOf(
        "Schaut euch an, wie klug ihr alle seid! Auf den ersten Blick hätte man es kaum gedacht.",
        "Einstimmig — und komplett richtig!",
        "Wart ihr alle auf derselben Schule?",
    ).random()

    fun oneCorrect(name: String): String = listOf(
        "Manchmal lohnt es sich, nicht wie alle anderen zu sein!",
        "Perfekte Täuschung! Alle Gegner abgehängt! Toucchhdown!",
        "$name, bist du hier etwa der Schlauste?",
    ).random()

    fun allWrong(): String = listOf(
        "Hmm... vielleicht wärt ihr beim Tauziehen besser?",
        "Muss man wirklich mit der Masse gehen, selbst wenn alle falsch liegen?",
        "Vielleicht sollten wir langsam anfangen, uns anzustrengen?",
    ).random()

    fun mixedResults(): String = listOf(
        "Gar kein schlechter Start!",
        "Ich glaube, jemand hat sich nicht einmal angestrengt!",
        "Gefällt’s euch? Weiter!",
    ).random()

    fun afterFirstThird(): String = listOf(
        "Aufwärmen vorbei. Jetzt geht’s zur Wassertherapie.",
        "Gar kein schlechter Start!",
        "Ich glaube, jemand hat sich nicht einmal angestrengt!",
        "Gefällt’s euch? Weiter!",
    ).random()

    fun lastStretch(): String = listOf(
        "Endspurt!",
        "Wer sitzt da ganz hinten auf dem letzten Platz? Los, aufholen!",
        "Nur noch ein bisschen, wirklich nur ein kleines Stück!",
        "Uuuund der letzte Sprint!",
    ).random()

    fun questionFlavor(index: Int, total: Int): String? = when {
        total >= 3 && index == (total / 3).coerceAtLeast(1) -> afterFirstThird()
        total >= 3 && index == total - 2 -> lastStretch()
        else -> null
    }

    fun roundOver(): String = listOf(
        "Die Runde ist vorbei! Alle waren großartig! Schauen wir auf die Tabelle.",
        "Endlich können wir ein Zwischenergebnis ziehen.",
        "Wollt ihr wissen, wer nach dieser Runde führt? Ich auf jeden Fall!",
        "Kommt es nur mir so vor, oder sind die Spieler mitten im Spiel eingeschlafen?",
        "Man könnte sagen, das ging in einem Atemzug! Wer hat diesmal geglänzt?",
    ).random()

    fun standings(ranked: List<String>): String {
        if (ranked.isEmpty()) return roundOver()
        val bits = mutableListOf(roundOver())
        if (ranked.size >= 2) {
            bits += listOf(
                "Auf dem letzten Platz ist ${ranked.last()}.",
                "Vorletzter ist ${ranked.last()}.",
            ).random()
        }
        val mid = ranked.drop(1).dropLast(1)
        if (mid.isNotEmpty() && ranked.size >= 4) {
            bits += "In der Tabellenmitte: ${mid.joinToString(", ")}."
        }
        if (ranked.size >= 3) {
            bits += "Auf Platz zwei — ${ranked[1]}."
        }
        bits += listOf(
            "Auf Platz eins — ${ranked.first()}",
            "Unser Quiz führt — ${ranked.first()}",
            "Ganz oben in der Tabelle — ${ranked.first()}",
        ).random()
        return bits.joinToString(" ")
    }

    fun winner(name: String): String = listOf(
        "Uuuund wie ihr schon gemerkt habt, $name ist heute der Sieger!!! Als Preis bekommt der Gewinner bewundernde Blicke von Freunden und Rivalen!",
        "Achtung, Achtung! Eilmeldung! Wir haben einen Quizsieger! $name, warum freust du dich nicht? Jaaaa! Glückwunsch!",
        "Was für ein Spiel! Unglaubliche Spannung auf unserer virtuellen Bühne! Ein unfassbarer Sieger! $name, glaubst du, dieser Sieg bleibt ungestraft!? Ha ha ha.",
        "$name, da stehst du nun auf dem Gipfel des Olymp! Drückt die Krone? Ach richtig, wir haben ja gar keine Krone! Ha ha ha.",
        "$name, denk bloß nicht, dass ich dir den Hauptpreis gebe! Niemals, der ist meiner, meiner und nur meiner!!!!",
        "Die Trompeten schmettern! Die Menge rastet aus! Daran haben wir uns inzwischen gewöhnt... oder, $name?",
    ).random()

    fun backToLobby(winner: String?): String =
        if (winner.isNullOrBlank()) {
            "Noch eine Runde. Drückt „Bereit“ auf euren Fernbedienungen."
        } else {
            "$winner hat gewonnen! Wer für ein neues Spiel bereit ist, drückt „Bereit“."
        }
}
