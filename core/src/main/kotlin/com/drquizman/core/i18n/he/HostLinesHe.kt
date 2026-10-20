package com.drquizman.core.i18n.he

import com.drquizman.core.mechanic.MechanicCatalog
import com.drquizman.core.model.CompetitionType

/** משפטי המנחה. טקסטי התחרויות נלקחים מקטלוג המכניקות. */
object HostLinesHe {
    fun welcome(): String = listOf(
        "שלום למשתתפים ולצופים האהובים והמכובדים של המופע הרפואי־אינטלקטואלי שלנו! היום נגלה מי מאיתנו הכי מהיר, הכי חכם, הכי חד... אבל למה אני כל הזמן מדבר על עצמי!? בואו נראה מי יילחם היום על הפרס הגדול של החידון.",
        "שלום, חבריי האינטלקטואלים! התכנסנו כדי לצלול להרפתקה אדירה על גלי הידע, התושייה, ואצל חלקכם גם קצת מזל! קחו את הטלפונים והצטרפו למשחק!",
        "סוף סוף אני שוב יכול לברך אתכם, קוויזמנים יקרים שלי! היום אירוע היסטורי — טיפול בכל מה שאפשר בשידור אינטלקטואלי חי! הצטרפו ונתחיל!",
    ).random()

    fun waiting(): String = waitingPool.random()

    private val waitingPool = listOf(
        "למה אתם מחכים? תתחברו כבר — מצב רוח טוב מובטח!",
        "כמה זמן עוד נחכה לכולם? כולם הלכו לבדיקה רפואית?",
        "אאאה... נראה שהערב הולך להיות קצת משעמם.",
        "איפה אתם, המטופלים שלי? הקליניקה פתוחה, השלטים מחכים!",
        "כל עוד אף אחד לא מתחבר, אני יכול לשחק עם עצמי. אבל זה כבר לא מופע — זו קבלת קהל.",
        "אל תתביישו, טלפונים ביד — היום מטפלים באינטליגנציה, בלי תור מראש!",
    )

    fun lobbyChatter(count: Int, ready: Int, previous: String = ""): String {
        val extra = if (count > 0 && ready in 1 until count) {
            listOf("מוכנים $ready מתוך $count. מחכים לשאר!")
        } else {
            emptyList()
        }
        val options = waitingPool + extra
        return options.filter { it != previous }.randomOrNull() ?: options.random()
    }

    fun playerJoined(name: String, first: Boolean): String {
        if (first) return "ובכן, הראשון שהגיע היום לטיפול הוא $name"
        return listOf(
            "השחקן הבא — $name",
            "ועכשיו נקבל בברכה את $name",
            "$name, אתה בטוח שלא טעית בדלת? טוב, נראה אותך בפעולה.",
            "אוווו, $name, ממש לא ציפיתי לראות אותך כאן!",
            "שלום, $name!",
            "$name, טוב לראות אותך!",
            "$name, ברוך הבא!",
            "אל תתבייש, $name, תצטרף!",
            "$name, כבר נפגשנו היום? או שאני מתבלבל?",
            "תחזיקו לי את הקרחת — תראו מי הגיע! $name!",
            "נקבל את השחקן הבא במחיאות כפיים סוערות! $name",
            "$name, אני מקווה שהיום נעבור את זה בלי עצבים...",
        ).random()
    }

    fun lobbyIdle(count: Int): String = lobbyWaiting(count, 0)

    fun lobbyWaiting(count: Int, ready: Int): String = when {
        count == 0 || ready == 0 -> waiting()
        ready < count -> "מוכנים $ready מתוך $count. מחכים לשאר!"
        else -> matchStart()
    }

    fun matchStart(): String =
        "ווווובכן, כולם כאן ואנחנו מוכנים להתחיל בטיפול... כלומר בקרב! קרב שבו כולם יהיו מנצחים, אבל מוביל יהיה כמובן רק אחד! ואת שמו נגלה ממש בקרוב. בינתיים — מתחילים!"

    fun matchStartWithGrant(points: Int): String =
        "אין היום הון התחלתי — כל אחד מקבל מיד $points נקודות, כדי שיהיה מה להפסיד. מתחילים!"

    fun intro(type: CompetitionType): String =
        MechanicCatalog[type]?.intro?.invoke() ?: "ממשיכים במופע!"

    fun chooseTopic(name: String, lastPlace: Boolean): String = if (lastPlace) {
        listOf(
            "כדי שלא יהיה לך כל כך מבאס להיות במקום האחרון, $name, אתה בוחר את הנושא.",
            "מי שמאחור בוחר את נושא הסיבוב. $name, זה אתה.",
        ).random()
    } else {
        listOf(
            "למי נפקיד את בחירת הנושא בסיבוב הזה!? $name, נסה אתה!",
            "בדרך כלל בחירת הנושא עוזרת להשיג תוצאה טובה יותר. נראה איך ילך לך, $name — בחר נושא.",
        ).random()
    }

    fun topicPicked(topic: String): String =
        "נושא הסיבוב — $topic! יוצאים לדרך!"

    fun titles(type: CompetitionType): String =
        MechanicCatalog[type]?.title.orEmpty()

    fun allCorrect(): String = listOf(
        "איזה חכמים כולכם! לא היית מנחש ממבט ראשון.",
        "פה אחד — ונכון לחלוטין!",
        "כולכם למדתם באותו בית ספר?",
    ).random()

    fun oneCorrect(name: String): String = listOf(
        "לפעמים עדיף לא להיות כמו כולם!",
        "הטעיה מושלמת! כל היריבים מאחור! וווווטאצ'דאון!",
        "$name, מה, אתה הכי חכם פה?",
    ).random()

    fun allWrong(): String = listOf(
        "הממ... אולי במשיכת חבל הייתם מצליחים יותר?",
        "חייבים להיות כמו כולם גם כשכולם טועים?",
        "אולי נתחיל להתאמץ?",
    ).random()

    fun mixedResults(): String = listOf(
        "פתיחה לא רעה!",
        "נדמה לי שמישהו אפילו לא התאמץ!",
        "אהבתם? ממשיכים!",
    ).random()

    fun afterFirstThird(): String = listOf(
        "החימום נגמר. עוברים לטיפולי מים.",
        "פתיחה לא רעה!",
        "נדמה לי שמישהו אפילו לא התאמץ!",
        "אהבתם? ממשיכים!",
    ).random()

    fun lastStretch(): String = listOf(
        "ישורת אחרונה!",
        "מי שם במקום האחרון? היציע האחורי, קדימה להדביק!",
        "עוד קצת, ממש עוד טיפה!",
        "וווווהספרינט האחרון!",
    ).random()

    fun questionFlavor(index: Int, total: Int): String? = when {
        total >= 3 && index == (total / 3).coerceAtLeast(1) -> afterFirstThird()
        total >= 3 && index == total - 2 -> lastStretch()
        else -> null
    }

    fun roundOver(): String = listOf(
        "הסיבוב הסתיים! כולם היו מצוינים! בואו נראה את טבלת הדירוג.",
        "סוף סוף אפשר לסכם תוצאות ביניים.",
        "מעניין אתכם מי מוביל אחרי הסיבוב הזה? אותי — מאוד!",
        "רק לי נדמה שהשחקנים התחילו להירדם תוך כדי המשחק?",
        "אפשר לומר שעברנו את זה בנשימה אחת! מי הצטיין הפעם?",
    ).random()

    fun standings(ranked: List<String>): String {
        if (ranked.isEmpty()) return roundOver()
        val bits = mutableListOf(roundOver())
        if (ranked.size >= 2) {
            bits += listOf(
                "במקום האחרון נמצא ${ranked.last()}.",
                "במקום השני מהסוף נמצא ${ranked.last()}.",
            ).random()
        }
        val mid = ranked.drop(1).dropLast(1)
        if (mid.isNotEmpty() && ranked.size >= 4) {
            bits += "באמצע הטבלה נמצאים: ${mid.joinToString(", ")}."
        }
        if (ranked.size >= 3) {
            bits += "במקום השני נמצא ${ranked[1]}."
        }
        bits += listOf(
            "במקום הראשון — ${ranked.first()}",
            "מוביל החידון שלנו — ${ranked.first()}",
            "בראש הטבלה — ${ranked.first()}",
        ).random()
        return bits.joinToString(" ")
    }

    fun winner(name: String): String = listOf(
        "ווווווכמו שכבר הבנתם, $name הוא המנצח של המשחק היום!!! כפרס, המנצח מקבל מבטים מלאי הערצה מהחברים ומהיריבים!",
        "שימו לב, שימו לב! הודעה דחופה! יש לנו מנצח בחידון! $name, למה אתה לא שמח? יששש! מזל טוב!",
        "משחק מדהים! מתח מטורף על הבמה הווירטואלית שלנו! מנצח בלתי ייאמן! $name, אתה חושב שהניצחון שלך יעבור בלי עונש!? חה חה חה.",
        "$name, הנה אתה עומד על פסגת האולימפוס! הכתר לא לוחץ? אה, נכון, אין אצלנו כתר! חה חה חה.",
        "$name, אל תחשוב אפילו שאמסור לך את הפרס הגדול! בחיים לא, הוא שלי, שלי ורק שלי!!!!",
        "החצוצרות מריעות! הקהל משתולל! כבר התרגלנו לזה... נכון, $name?",
    ).random()

    fun backToLobby(winner: String?): String =
        if (winner.isNullOrBlank()) {
            "עוד סיבוב. לחצו «מוכן» בשלטים."
        } else {
            "$winner ניצח! מי שמוכן למשחק חדש — לחצו «מוכן»."
        }
}
