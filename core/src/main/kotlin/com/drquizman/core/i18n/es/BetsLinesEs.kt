package com.drquizman.core.i18n.es
/** Frases para el concurso «Apuestas cerradas». */
object BetsLinesEs {
 const val TITLE = "Apuestas cerradas"
 const val SLOGAN = "El tema se conoce. Apuesta o calla"
 fun intro(): String = "Apuesta y responde. Si aciertas, ganas lo apostado. " + "Si fallas, pierdes lo mismo. Cada vez hay un tema nuevo. Si no apuestas a tiempo, no juegas esa pregunta."
 fun announceTopic(topic: String, index: Int, total: Int): String = listOf(
  "Pregunta $index de $total. ¡Tema: $topic!", "Siguiente tema: $topic. ¡Escuchad y preparad las apuestas!", "Tema de la pregunta $index: $topic. Si estás seguro, apuesta fuerte.", "Atención, tema: $topic. Tras el anuncio, cinco segundos para apostar.").random()
 fun placeBets(): String = listOf("¡Apuestas! Cien, doscientos, trescientos o cuatrocientos. ¡Cinco segundos!", "¿Apuestas cerradas? Aún no. ¡Cinco segundos!", "Acepto apuestas. Cinco segundos, señoras y señores.").random()
 fun results(won: List<Pair<String, Int>>, lost: List<Pair<String, Int>>, skipped: List<String>): String {
  val bits = mutableListOf<String>()
  if (won.isEmpty() && lost.isEmpty() && skipped.isNotEmpty()) return listOf("Nadie apostó. ¡La pregunta se quedó sin jugadores!", "Sin apuestas, nadie responde. Seguimos.").random()
  if (won.isNotEmpty()) bits += won.joinToString(", ") { "${it.first} +${it.second}" }.let { "Ganaron: $it." }
  if (lost.isNotEmpty()) bits += lost.joinToString(", ") { "${it.first} −${it.second}" }.let { "La banca ganó: $it." }
  if (skipped.isNotEmpty() && skipped.size <= 3) bits += "Sin apuesta: ${skipped.joinToString(", ")}."
  if (bits.isEmpty()) return if (won.isNotEmpty()) "¡Las apuestas salieron bien!" else "La banca va ganando."
  return bits.joinToString(" ")
 }
}