package com.drquizman.core.i18n.es
/** Frases para el concurso «Puzle». */
object PuzzleLinesEs {
 const val TITLE = "Puzle"
 const val SLOGAN = "Adivina primero o queda cubierto"
 fun intro(): String = "Puzle: gana quien adivine primero. Las piezas se abren solas. " + "Aciertas: un cuarto del puzle cubre a cada rival. Fallas: te cubre a ti. Cuatro cuartos y estás fuera."
 fun correct(name: String): String = listOf("¡$name acertó! Cada rival recibe una pieza.", "$name fue primero. Un cuarto para cada rival.", "$name completó la imagen. Los demás empiezan a cubrirse.", "¡Correcto, $name! Crecieron los puzles rivales.").random()
 fun wrong(name: String): String = listOf("$name falló: ¡un cuarto para sí!", "¡No! $name se cubre con otra pieza.", "$name se precipitó. Su imagen crece.", "$name falla. Un cuarto del puzle para él.").random()
 fun timeout(): String = listOf("Nadie pulsó. Los puzles quedan igual. Seguimos.", "Silencio. Las piezas se abrieron, pero nadie respondió.", "Se acabó el tiempo sin respuesta. Nadie gana nada.").random()
 fun out(names: List<String>): String { if (names.isEmpty()) return ""; if (names.size == 1) { val n=names.first(); return listOf("¡$n desapareció bajo el cuarto cuarto!", "El puzle cubrió por completo a $n. Está fuera.", "$n queda eliminado del puzle.").random() }; val joined=names.joinToString(" y "); return "El puzle los cubrió a todos de golpe: $joined." }
 fun settle(lead: String, payouts: List<Pair<String, Int>>): String { val head=lead.trim(); val prefix=if (head.isEmpty()) "" else "$head "; if (payouts.isEmpty()) return prefix+listOf("El puzle cubrió a todos. El bote se queda en casa.", "Nadie llegó al final. Sin puntos en el puzle.").random(); val bits=payouts.joinToString(", ") { "${it.first} +${it.second}" }; return prefix+listOf("Los que quedaron limpios se llevan el bote: $bits.", "Resultado del puzle: $bits.", "Una imagen limpia vale más. $bits.").random() }
}