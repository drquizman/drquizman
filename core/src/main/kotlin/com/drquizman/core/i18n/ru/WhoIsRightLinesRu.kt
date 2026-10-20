package com.drquizman.core.i18n.ru

/** Реплики конкурса «Кто прав!?» из Quizman/host.txt и GDD. */
object WhoIsRightLinesRu {
    const val TITLE = "КТО ПРАВ!?"
    const val SLOGAN = "СЕМЬ ВЕРНЫХ — И ТЫ ПРАВ"

    fun intro(): String =
        "Кто прав!? Время ответа не играет роли. Первый, кто даст семь правильных ответов, " +
            "выиграл и получает больше всех очков! Остальные — по сотне за каждый верный."

    fun race(leaderName: String?, leaderMarks: Int, atSix: List<String>, goal: Int): String {
        if (atSix.size >= 2) {
            return "На шести сразу ${atSix.joinToString(" и ")}! Кто первый заберёт седьмой?"
        }
        if (atSix.size == 1) {
            return "У ${atSix.first()} уже шесть! Ещё один — и куш его!"
        }
        if (leaderName != null && leaderMarks in 1 until goal) {
            return listOf(
                "Пока впереди $leaderName — $leaderMarks из $goal.",
                "$leaderName набирает верные. Уже $leaderMarks!",
                "Гонка до семи. Лидирует $leaderName.",
            ).random()
        }
        return listOf(
            "Пока ни у кого нет отрыва. Продолжаем!",
            "Семь верных — и конкурс ваш. Дальше!",
            "Время не важно. Важна только правота!",
        ).random()
    }

    fun jackpot(name: String, prize: Int): String =
        listOf(
            "Семь из семи! $name забирает $prize!",
            "Кто прав? $name прав! Куш — $prize.",
            "$name первым набрал семь верных. Плюс $prize!",
        ).random()

    fun capOver(name: String?, perCorrect: Int): String =
        if (name == null) {
            "Конкурс закрываем. Каждый забирает по $perCorrect за верный ответ."
        } else {
            "Семь так никто и не набрал. Впереди $name, но куш без семи не отдаём — только по $perCorrect за верный."
        }
}

