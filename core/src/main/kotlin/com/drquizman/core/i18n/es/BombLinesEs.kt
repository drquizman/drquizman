package com.drquizman.core.i18n.es
/** Frases para el concurso «Bomba». */
object BombLinesEs {
 const val TITLE = "Bomba"
 const val SLOGAN = "Pásala o explota"
 fun intro(): String = "Un concurso que es pura tortura: ¡Bomba! Quien tiene la bomba responde. " + "Aciertas: la pasas. Fallas demasiado o no llegas a pasarla: ¡boooom!"
 fun yourTurn(name: String): String = listOf("$name, ¡tienes la bomba!", "Cuidado, $name, ¡está haciendo tic-tac!", "$name, no te duermas: ese juguete es peligroso.").random()
 fun passed(from: String, to: String): String = listOf("$from la pasó a tiempo. Ahora la tiene $to.", "Uf, $from llegó. $to, ¡atrapa!", "¡Pase completado! $to, te toca desactivarla.").random()
 fun strike(name: String, left: Int): String = if (left <= 1) listOf("¡Mal, $name! Un error más y boom.", "$name, fallaste. La mecha se acorta, piensa rápido.").random() else "¡Mal, $name! La bomba sigue contigo."
 fun exploded(name: String, penalty: Int): String = listOf("¡Booooom! $name no llegó. ¡Menos $penalty!", "¡Explosión! $name, un recuerdo: menos $penalty.", "La bomba no perdona. $name pierde $penalty puntos.").random()
 fun survived(name: String): String = "$name acertó. La bomba está tranquila por ahora. ¡Nueva pregunta!"
}