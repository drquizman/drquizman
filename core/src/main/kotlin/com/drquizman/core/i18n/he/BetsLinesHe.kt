package com.drquizman.core.i18n.he

/** טקסטים לתחרות "ההימורים נסגרו" מתוך Quizman/host.txt ו-GDD. */
object BetsLinesHe {
    const val TITLE = "ההימורים נסגרו"
    const val SLOGAN = "הנושא ידוע. הימר או שתוק"

    fun intro(): String =
        "המר וענה על השאלה. תשובה נכונה — אתה מקבל את סכום ההימור שלך. " +
            "טעות — אתה מפסיד אותו סכום. בכל פעם נושא חדש. לא הספקת להמר — השאלה בלעדיך."

    fun announceTopic(topic: String, index: Int, total: Int): String = listOf(
        "שאלה $index מתוך $total. הנושא — $topic!",
        "הנושא הבא: $topic. הקשיבו היטב והכינו את ההימורים!",
        "נושא השאלה $index — $topic! מי שבטוח, מהמר בגדול.",
        "שימו לב, הנושא: $topic. אחרי ההכרזה — חמש שניות להימור.",
    ).random()

    fun placeBets(): String = listOf(
        "הימורים! מאה, מאתיים, שלוש מאות או ארבע מאות. חמש שניות!",
        "ההימורים נסגרו? עוד לא — אז קדימה! חמש שניות.",
        "מקבל הימורים. חמש שניות, רבותיי!",
    ).random()

    fun results(
        won: List<Pair<String, Int>>,
        lost: List<Pair<String, Int>>,
        skipped: List<String>,
    ): String {
        val bits = mutableListOf<String>()
        if (won.isEmpty() && lost.isEmpty() && skipped.isNotEmpty()) {
            return listOf(
                "אף אחד לא הימר. השאלה נשרפה בלי שחקנים!",
                "אין הימורים — ואין מי שיענה. ממשיכים.",
            ).random()
        }
        if (won.isNotEmpty()) {
            bits += won.joinToString(", ") { "${it.first} +${it.second}" }
                .let { "לקחו את הקופה: $it." }
        }
        if (lost.isNotEmpty()) {
            bits += lost.joinToString(", ") { "${it.first} −${it.second}" }
                .let { "הקופה לקחה: $it." }
        }
        if (skipped.isNotEmpty() && skipped.size <= 3) {
            bits += "בלי הימור: ${skipped.joinToString(", ")}."
        }
        if (bits.isEmpty()) {
            return if (won.isNotEmpty()) "ההימורים הצליחו!" else "הקופה ברווח."
        }
        return bits.joinToString(" ")
    }
}
