package com.drquizman.core.i18n.es
/** Frases para «Oriente es un asunto delicado». */
object EastLinesEs {
 const val TITLE = "Oriente es un asunto delicado"
 const val SLOGAN = "Cuanto más rápido, más gordo"
 fun intro(): String = "¡Responde lo más rápido posible! ¡Cada segundo desaparecen puntos! " + "Las respuestas están escritas de derecha a izquierda: Oriente es un asunto delicado."
 fun results(scored: List<Pair<String, Int>>): String {
  if (scored.isEmpty()) return listOf("Nadie logró leerlo. ¡Oriente era demasiado delicado!", "De derecha a izquierda no es lo mismo. Cero puntos.", "Todos fallaron. ¡Y los puntos se derritieron cada segundo!").random()
  if (scored.size == 1) { val (name, prize) = scored.first(); return listOf("$name lo leyó de derecha a izquierda. ¡Más $prize!", "Solo $name llegó. Se lleva $prize.", "$name, ¡buena vista! Más $prize.").random() }
  val bits = scored.sortedByDescending { it.second }.joinToString(", ") { "${it.first} +${it.second}" }
  return listOf("Llegaron: $bits.", "Lo leyeron: $bits.").random()
 }
}