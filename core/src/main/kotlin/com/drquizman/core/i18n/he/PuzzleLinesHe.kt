package com.drquizman.core.i18n.he

/** טקסטים לתחרות "פאזל" מתוך Quizman/host.txt ו-GDD. */
object PuzzleLinesHe {
    const val TITLE = "פאזל"
    const val SLOGAN = "נחש ראשון — או שתיסגר"

    fun intro(): String =
        "פאזל — הראשון שמנחש נכון מנצח. החלקים נפתחים לבד. " +
            "ניחשת — רבע פאזל מכסה כל אחד מהיריבים. טעית — רבע מכסה אותך. " +
            "ארבעה רבעים — ונעלמת מהתחרות."

    fun correct(name: String): String = listOf(
        "$name ניחש! לכל האחרים — חתיכת פאזל.",
        "$name היה הראשון. כל יריב מקבל רבע.",
        "$name השלים את התמונה. האחרים מתחילים להיסגר.",
        "נכון, $name! הפאזלים של האחרים גדלו.",
    ).random()

    fun wrong(name: String): String = listOf(
        "$name פספס — רבע פאזל לעצמו!",
        "לא! $name סוגר על עצמו חתיכה.",
        "$name מיהר מדי. התמונה עליו גדלה.",
        "טעות של $name. רבע פאזל — עליו.",
    ).random()

    fun timeout(): String = listOf(
        "אף אחד לא לחץ. הפאזלים נשארים במקום, ממשיכים.",
        "שקט. החלקים נפתחו, אבל אין אמיצים.",
        "הזמן נגמר בלי תשובה. אף אחד לא מקבל כלום.",
    ).random()

    fun out(names: List<String>): String {
        if (names.isEmpty()) return ""
        if (names.size == 1) {
            val n = names.first()
            return listOf(
                "$n נעלם מתחת לרבע הרביעי!",
                "הפאזל סגר את $n. בתחרות הזאת הוא כבר בחוץ.",
                "$n מודח מהפאזל.",
            ).random()
        }
        val joined = names.joinToString(" ו")
        return "הפאזל סגר בבת אחת: $joined."
    }

    fun settle(lead: String, payouts: List<Pair<String, Int>>): String {
        val head = lead.trim()
        val prefix = if (head.isEmpty()) "" else "$head "
        if (payouts.isEmpty()) {
            return prefix + listOf(
                "הפאזל סגר את כולם. הקופה נשארת בבית.",
                "אף אחד לא הגיע לסיום. אין נקודות על הפאזל.",
            ).random()
        }
        val bits = payouts.joinToString(", ") { "${it.first} +${it.second}" }
        return prefix + listOf(
            "מי שנשאר בלי תמונות מיותרות לוקח את הקופה: $bits.",
            "תוצאת הפאזל: $bits.",
            "תמונות נקיות שוות יותר. $bits.",
        ).random()
    }
}
