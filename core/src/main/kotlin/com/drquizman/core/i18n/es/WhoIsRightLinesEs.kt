package com.drquizman.core.i18n.es
/** Frases para «¿Quién tiene razón?». */
object WhoIsRightLinesEs {
 const val TITLE = "¿Quién tiene razón!?"
 const val SLOGAN = "Siete aciertos y tienes razón"
 fun intro(): String = "¿Quién tiene razón!? El tiempo no importa. El primero con siete respuestas correctas gana el premio mayor. " + "Los demás reciben cien por cada acierto."
 fun race(leaderName: String?, leaderMarks: Int, atSix: List<String>, goal: Int): String { if (atSix.size >= 2) return "¡${atSix.joinToString(" y ")} ya tienen seis! ¿Quién consigue el séptimo?"; if (atSix.size == 1) return "¡${atSix.first()} ya tiene seis! Uno más y el bote es suyo."; if (leaderName != null && leaderMarks in 1 until goal) return listOf("$leaderName lidera: $leaderMarks de $goal.", "$leaderName acumula aciertos. ¡Ya van $leaderMarks!", "Carrera a siete. $leaderName va delante.").random(); return listOf("Nadie se escapa todavía. ¡Seguimos!", "Siete aciertos y la prueba es tuya. ¡Vamos!", "El tiempo no importa. Solo importa acertar.").random() }
 fun jackpot(name: String, prize: Int): String = listOf("¡Siete de siete! $name se lleva $prize.", "¿Quién tiene razón? ¡$name! El bote es $prize.", "$name fue el primero con siete. ¡Más $prize!").random()
 fun capOver(name: String?, perCorrect: Int): String = if (name == null) "Fin de la prueba. Cada uno recibe $perCorrect por acierto." else "Nadie llegó a siete. $name lidera, pero sin siete no hay bote: solo $perCorrect por acierto."
}