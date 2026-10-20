package com.drquizman.core.i18n.he

/** טקסטים לתחרות "המזרח הוא עניין עדין" מתוך Quizman/host.txt ו-GDD. */
object EastLinesHe {
    const val TITLE = "המזרח הוא עניין עדין"
    const val SLOGAN = "ככל שמהר יותר — כך שמן יותר"

    fun intro(): String =
        "ענה כמה שיותר מהר! בכל שנייה יורדות נקודות! " +
            "התשובות כתובות מימין לשמאל — המזרח הוא עניין עדין!"

    fun results(scored: List<Pair<String, Int>>): String {
        if (scored.isEmpty()) {
            return listOf(
                "אף אחד לא הצליח לקרוא. המזרח התברר כעדין מדי!",
                "מימין לשמאל — זה לא כמו משמאל לימין. אפס נקודות.",
                "כולם פספסו. והנקודות נמסו בכל שנייה!",
            ).random()
        }
        if (scored.size == 1) {
            val (name, prize) = scored.first()
            return listOf(
                "$name קרא מימין לשמאל! פלוס $prize!",
                "רק $name הספיק. לוקח $prize.",
                "$name, יש לך עין חדה! פלוס $prize.",
            ).random()
        }
        val bits = scored
            .sortedByDescending { it.second }
            .joinToString(", ") { "${it.first} +${it.second}" }
        return listOf(
            "הספיקו: $bits.",
            "קראו: $bits.",
        ).random()
    }
}
