package com.drquizman.core.i18n.ru

/** Реплики конкурса «Бомба» из Quizman/host.txt и GDD. */
object BombLinesRu {
    const val TITLE = "БОМБА"
    const val SLOGAN = "ПЕРЕДАЙ ИЛИ ВЗОРВИСЬ"

    fun intro(): String =
        "Состязание — истязание — БОМБА! Отвечает тот, у кого бомба! " +
            "Правильный ответ — передаём опасную игрушку дальше. " +
            "Не успел передать или ответил несколько раз неправильно — бабааааххх!"

    fun yourTurn(name: String): String =
        listOf(
            "$name, бомба у тебя!",
            "Осторожно, $name, она тикает!",
            "$name, не зевай — игрушка опасная!",
        ).random()

    fun passed(from: String, to: String): String =
        listOf(
            "$from вовремя передал! Теперь бомба у $to.",
            "Уф, $from успел. $to, лови!",
            "Передача есть! $to, твоя очередь обезвреживать.",
        ).random()

    fun strike(name: String, left: Int): String =
        if (left <= 1) {
            listOf(
                "Не то, $name! Ещё одна ошибка — и бабах.",
                "$name, мимо! Фитиль короче, думай быстрее.",
            ).random()
        } else {
            "Неправильно, $name! Бомба остаётся у тебя."
        }

    fun exploded(name: String, penalty: Int): String =
        listOf(
            "Бабааааххх! $name не успел. Минус $penalty!",
            "Взрыв! $name, это тебе на память: минус $penalty.",
            "Бомба не прощает. $name теряет $penalty баллов.",
        ).random()

    fun survived(name: String): String =
        "$name ответил верно. Бомба пока молчит. Новый вопрос!"
}

