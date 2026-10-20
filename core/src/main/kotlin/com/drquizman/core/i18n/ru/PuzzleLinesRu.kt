package com.drquizman.core.i18n.ru

/** Реплики конкурса «Пазл» из Quizman/host.txt и GDD. */
object PuzzleLinesRu {
    const val TITLE = "ПАЗЛ"
    const val SLOGAN = "УГАДАЙ ПЕРВЫМ — ИЛИ ЗАКРОЕТ"

    fun intro(): String =
        "Пазл — побеждает первый угадавший ответ. Куски открываются сами. " +
            "Угадал — четверть пазла закрывает каждого из остальных. Ошибся — четверть закрывает тебя. " +
            "Четыре четверти — и ты исчезаешь из конкурса."

    fun correct(name: String): String = listOf(
        "$name угадал! Остальным — по куску пазла.",
        "Первым был $name. Соперникам прилетает по четверти.",
        "$name собрал картинку. Остальных потихоньку закрывает.",
        "Верно, $name! Чужие пазлы подросли.",
    ).random()

    fun wrong(name: String): String = listOf(
        "$name промахнулся — себе четверть пазла!",
        "Мимо! $name закрывает кусок на себе.",
        "$name поспешил. Картинка на нём подросла.",
        "Ошибка $name. Четверть пазла — на него.",
    ).random()

    fun timeout(): String = listOf(
        "Никто не нажал. Пазлы на местах, идём дальше.",
        "Тишина. Куски открылись, а смельчаков нет.",
        "Время вышло без ответа. Никому ничего.",
    ).random()

    fun out(names: List<String>): String {
        if (names.isEmpty()) return ""
        if (names.size == 1) {
            val n = names.first()
            return listOf(
                "$n исчез под четвёртой четвертью!",
                "Пазл закрыл $n. В этом конкурсе его больше нет.",
                "$n выбывает из пазла.",
            ).random()
        }
        val joined = names.joinToString(" и ")
        return "Пазл закрыл сразу: $joined."
    }

    fun settle(lead: String, payouts: List<Pair<String, Int>>): String {
        val head = lead.trim()
        val prefix = if (head.isEmpty()) "" else "$head "
        if (payouts.isEmpty()) {
            return prefix + listOf(
                "Пазл закрыл всех. Банк остаётся в кассе.",
                "До финиша никто не дошёл. Очков за пазл нет.",
            ).random()
        }
        val bits = payouts.joinToString(", ") { "${it.first} +${it.second}" }
        return prefix + listOf(
            "Кто остался без лишних картинок, забирает банк: $bits.",
            "Итог пазла: $bits.",
            "Чистые картинки дороже. $bits.",
        ).random()
    }
}

