package com.drquizman.core.i18n.he

/** טקסטים לתחרות "פצצה" מתוך Quizman/host.txt ו-GDD. */
object BombLinesHe {
    const val TITLE = "פצצה"
    const val SLOGAN = "העבר או תתפוצץ"

    fun intro(): String =
        "תחרות שהיא עינוי — פצצה! מי שמחזיק בפצצה עונה! " +
            "תשובה נכונה — מעבירים את הצעצוע המסוכן הלאה. " +
            "לא הספקת להעביר או טעית כמה פעמים — בווווום!"

    fun yourTurn(name: String): String =
        listOf(
            "$name, הפצצה אצלך!",
            "זהירות, $name, היא מתקתקת!",
            "$name, אל תחלום — זה צעצוע מסוכן!",
        ).random()

    fun passed(from: String, to: String): String =
        listOf(
            "$from העביר בזמן! עכשיו הפצצה אצל $to.",
            "פיו, $from הספיק. $to, תפוס!",
            "המסירה עברה! $to, תורך לנטרל.",
        ).random()

    fun strike(name: String, left: Int): String =
        if (left <= 1) {
            listOf(
                "לא נכון, $name! עוד טעות אחת — ובום.",
                "$name, פספסת! הפתיל מתקצר, תחשוב מהר.",
            ).random()
        } else {
            "לא נכון, $name! הפצצה נשארת אצלך."
        }

    fun exploded(name: String, penalty: Int): String =
        listOf(
            "בווווום! $name לא הספיק. מינוס $penalty!",
            "פיצוץ! $name, מזכרת קטנה: מינוס $penalty.",
            "הפצצה לא סולחת. $name מאבד $penalty נקודות.",
        ).random()

    fun survived(name: String): String =
        "$name ענה נכון. הפצצה שקטה בינתיים. שאלה חדשה!"
}
