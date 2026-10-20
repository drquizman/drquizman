package com.drquizman.core.i18n.he

/** טקסטים לתחרות "מי צודק!?" מתוך Quizman/host.txt ו-GDD. */
object WhoIsRightLinesHe {
    const val TITLE = "מי צודק!?"
    const val SLOGAN = "שבע נכונות — ואתה צודק"

    fun intro(): String =
        "מי צודק!? זמן התשובה לא חשוב. הראשון שנותן שבע תשובות נכונות " +
            "מנצח ומקבל הכי הרבה נקודות! כל השאר — מאה על כל תשובה נכונה."

    fun race(leaderName: String?, leaderMarks: Int, atSix: List<String>, goal: Int): String {
        if (atSix.size >= 2) {
            return "כבר שישה ל־${atSix.joinToString(" ו")}! מי ייקח ראשון את השביעי?"
        }
        if (atSix.size == 1) {
            return "ל־${atSix.first()} כבר יש שישה! עוד אחד — והקופה שלו!"
        }
        if (leaderName != null && leaderMarks in 1 until goal) {
            return listOf(
                "כרגע מוביל $leaderName — $leaderMarks מתוך $goal.",
                "$leaderName צובר תשובות נכונות. כבר $leaderMarks!",
                "המרוץ לשבעה. $leaderName מוביל.",
            ).random()
        }
        return listOf(
            "כרגע אף אחד לא בורח קדימה. ממשיכים!",
            "שבע נכונות — והתחרות שלכם. קדימה!",
            "הזמן לא חשוב. רק הצדק חשוב!",
        ).random()
    }

    fun jackpot(name: String, prize: Int): String =
        listOf(
            "שבע מתוך שבע! $name לוקח $prize!",
            "מי צודק? $name צודק! הקופה — $prize.",
            "$name היה הראשון עם שבע נכונות. פלוס $prize!",
        ).random()

    fun capOver(name: String?, perCorrect: Int): String =
        if (name == null) {
            "סוגרים את התחרות. כל אחד מקבל $perCorrect על כל תשובה נכונה."
        } else {
            "אף אחד לא הגיע לשבע. $name מוביל, אבל בלי שבע אין קופה — רק $perCorrect על כל תשובה נכונה."
        }
}
