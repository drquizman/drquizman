package com.drquizman.core.i18n.es

import com.drquizman.core.mechanic.MechanicCatalog
import com.drquizman.core.model.CompetitionType

/** Frases del presentador. */
object HostLinesEs {
    fun welcome(): String = listOf(
        "¡Hola a jugadores y espectadores de nuestro show médico-intelectual! Hoy veremos quién es más rápido, listo y agudo... ¿Pero por qué sigo hablando de mí? Veamos quién luchará por el gran premio.",
        "¡Hola, amigos intelectuales! Nos espera una aventura de conocimiento, rapidez mental y un poco de suerte. ¡Coged los teléfonos y entrad al juego!",
        "¡Por fin puedo daros la bienvenida otra vez, queridos Quizmaniacs! Hoy tratamos de todo en directo y con inteligencia. ¡Entrad y empezamos!",
    ).random()

    fun waiting(): String = waitingPool.random()

    private val waitingPool = listOf(
        "¿A qué esperáis? Entrad ya: ¡el buen humor está garantizado!",
        "¿Cuánto más esperamos? ¿Todos se fueron a revisión médica?",
        "Uhhh... parece que esta noche puede ser un poco aburrida.",
        "¿Dónde están mis pacientes? ¡La clínica está abierta y los mandos esperan!",
        "Mientras nadie entra, puedo jugar conmigo mismo. Pero eso ya no es un show.",
        "No os cortéis: teléfonos fuera. Hoy tratamos la inteligencia sin cita previa.",
    )

    fun lobbyChatter(count: Int, ready: Int, previous: String = ""): String {
        val extra = if (count > 0 && ready in 1 until count) {
            listOf("Listos: $ready de $count. ¡Esperando al resto!")
        } else emptyList()
        val options = waitingPool + extra
        return options.filter { it != previous }.randomOrNull() ?: options.random()
    }

    fun playerJoined(name: String, first: Boolean): String {
        if (first) return "Y el primer paciente de hoy es $name"
        return listOf(
            "Siguiente jugador: $name",
            "Y ahora recibamos a $name",
            "$name, ¿seguro que no te equivocaste de puerta? Bueno, veamos qué sabes.",
            "Ooooh, $name, ¡no esperaba verte por aquí!",
            "¡Hola, $name!",
            "$name, ¡me alegra verte!",
            "$name, ¡bienvenido!",
            "No te cortes, $name, ¡únete!",
            "$name, ¿ya nos vimos hoy o me estoy confundiendo?",
            "¡Mirad quién llegó! $name.",
            "¡Un gran aplauso para el siguiente jugador! $name",
            "$name, espero que hoy salgamos de esto sin perder los nervios...",
        ).random()
    }

    fun lobbyIdle(count: Int): String = lobbyWaiting(count, 0)

    fun lobbyWaiting(count: Int, ready: Int): String = when {
        count == 0 || ready == 0 -> waiting()
        ready < count -> "Listos: $ready de $count. ¡Esperando al resto!"
        else -> matchStart()
    }

    fun matchStart(): String =
        "¡Y ya estamos todos! Podemos empezar el tratamiento... digo, la batalla. Todos pueden ganar, pero solo uno liderará. Muy pronto sabremos quién. ¡Empezamos!"

    fun matchStartWithGrant(points: Int): String =
        "Hoy no hay fortuna inicial: cada uno recibe $points puntos para tener algo que perder. ¡Empezamos!"

    fun intro(type: CompetitionType): String =
        MechanicCatalog[type]?.intro?.invoke() ?: "¡El show continúa!"

    fun chooseTopic(name: String, lastPlace: Boolean): String = if (lastPlace) {
        listOf(
            "Para que el último puesto duela menos, $name, eliges el tema.",
            "Quien va último elige el tema de la ronda. $name, te toca.",
        ).random()
    } else {
        listOf(
            "¿A quién confiamos el tema de esta ronda? $name, ¡prueba tú!",
            "Elegir tema suele ayudar. Veamos si te funciona, $name: elige.",
        ).random()
    }

    fun topicPicked(topic: String): String = "Tema de la ronda: $topic. ¡Vamos!"

    fun titles(type: CompetitionType): String = MechanicCatalog[type]?.title.orEmpty()

    fun allCorrect(): String = listOf(
        "¡Qué listos sois todos! A primera vista no lo parecía.",
        "¡Unanimidad y totalmente correcto!",
        "¿Fuisteis todos al mismo colegio?",
    ).random()

    fun oneCorrect(name: String): String = listOf(
        "¡A veces conviene no ser como todos!",
        "¡Engaño perfecto! Todos los rivales atrás.",
        "$name, ¿eres el más listo de aquí?",
    ).random()

    fun allWrong(): String = listOf(
        "Hmm... ¿quizá os iría mejor en tira y afloja?",
        "¿Hay que seguir a todos incluso cuando todos fallan?",
        "¿Empezamos a esforzarnos?",
    ).random()

    fun mixedResults(): String = listOf(
        "¡No está mal para empezar!",
        "Me parece que alguien ni se esforzó.",
        "¿Os gustó? ¡Seguimos!",
    ).random()

    fun afterFirstThird(): String = listOf(
        "Se acabó el calentamiento. Pasamos al tratamiento serio.",
        "¡No está mal para empezar!",
        "Me parece que alguien ni se esforzó.",
        "¿Os gustó? ¡Seguimos!",
    ).random()

    fun lastStretch(): String = listOf(
        "¡Recta final!",
        "¿Quién está último? ¡Vamos, hay que remontar!",
        "¡Un poco más, solo un poco!",
        "¡Yyyy sprint final!",
    ).random()

    fun questionFlavor(index: Int, total: Int): String? = when {
        total >= 3 && index == (total / 3).coerceAtLeast(1) -> afterFirstThird()
        total >= 3 && index == total - 2 -> lastStretch()
        else -> null
    }

    fun roundOver(): String = listOf(
        "¡Ronda terminada! Veamos la clasificación.",
        "Por fin podemos resumir resultados provisionales.",
        "¿Queréis saber quién lidera? Yo sí.",
        "¿Solo me parece a mí que algunos empezaron a dormirse?",
        "¡Pasó volando! ¿Quién destacó esta vez?",
    ).random()

    fun standings(ranked: List<String>): String {
        if (ranked.isEmpty()) return roundOver()
        val bits = mutableListOf(roundOver())
        if (ranked.size >= 2) bits += listOf(
            "En último lugar está ${ranked.last()}.",
            "En penúltimo lugar está ${ranked.last()}.",
        ).random()
        val mid = ranked.drop(1).dropLast(1)
        if (mid.isNotEmpty() && ranked.size >= 4) bits += "En mitad de la tabla: ${mid.joinToString(", ")}."
        if (ranked.size >= 3) bits += "En segundo lugar está ${ranked[1]}."
        bits += listOf(
            "En primer lugar: ${ranked.first()}",
            "Lidera nuestro quiz: ${ranked.first()}",
            "Al frente de la tabla: ${ranked.first()}",
        ).random()
        return bits.joinToString(" ")
    }

    fun winner(name: String): String = listOf(
        "¡Y como ya habéis entendido, $name gana el juego de hoy! Premio: miradas de admiración de amigos y rivales.",
        "¡Atención! ¡Tenemos ganador! $name, ¿por qué no celebras? ¡Felicidades!",
        "¡Partida increíble! ¡Tensión máxima! $name, ¿creías que esta victoria saldría gratis? Ja, ja.",
        "$name, estás en la cima del Olimpo. ¿Aprieta la corona? Ah, cierto: no tenemos corona.",
        "$name, ¡ni sueñes con que te entregue el gran premio! ¡Es mío!",
        "¡Suenan las trompetas, el público enloquece! Ya nos acostumbramos... ¿verdad, $name?",
    ).random()

    fun backToLobby(winner: String?): String =
        if (winner.isNullOrBlank()) "Otra ronda. Pulsad «Listo» en los mandos."
        else "$winner ganó. Quien quiera otra partida, que pulse «Listo»."
}
