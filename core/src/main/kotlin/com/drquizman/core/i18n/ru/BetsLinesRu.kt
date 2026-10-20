package com.drquizman.core.i18n.ru

/** Реплики конкурса «Ставки сделаны» из Quizman/host.txt и GDD. */
object BetsLinesRu {
    const val TITLE = "СТАВКИ СДЕЛАНЫ"
    const val SLOGAN = "ТЕМА ИЗВЕСТНА. СТАВЬ ИЛИ МОЛЧИ"

    fun intro(): String =
        "Сделай ставку и ответь на вопрос. Получаешь свою ставку при правильном ответе. " +
            "Ошибка — столько же теряешь. Тема каждый раз новая. Не успел поставить — вопрос без тебя."

    fun announceTopic(topic: String, index: Int, total: Int): String = listOf(
        "Вопрос $index из $total. Тема — $topic!",
        "Следующая тема: $topic. Слушайте внимательно и готовьте ставки!",
        "Тема вопроса $index — $topic! Кто уверен, тот ставит крупно.",
        "Внимание, тема: $topic. После объявления — пять секунд на ставку.",
    ).random()

    fun placeBets(): String = listOf(
        "Ставки! Сто, двести, триста или четыреста. Пять секунд!",
        "Ставки сделаны? Ещё нет — так ставьте! Пять секунд.",
        "Принимаю ставки. Пять секунд, господа!",
    ).random()

    fun results(
        won: List<Pair<String, Int>>,
        lost: List<Pair<String, Int>>,
        skipped: List<String>,
    ): String {
        val bits = mutableListOf<String>()
        if (won.isEmpty() && lost.isEmpty() && skipped.isNotEmpty()) {
            return listOf(
                "Никто не поставил. Вопрос сгорел без игроков!",
                "Ставок нет — и отвечать некому. Идём дальше.",
            ).random()
        }
        if (won.isNotEmpty()) {
            bits += won.joinToString(", ") { "${it.first} +${it.second}" }
                .let { "Забрали банк: $it." }
        }
        if (lost.isNotEmpty()) {
            bits += lost.joinToString(", ") { "${it.first} −${it.second}" }
                .let { "Касса забрала: $it." }
        }
        if (skipped.isNotEmpty() && skipped.size <= 3) {
            bits += "Без ставки: ${skipped.joinToString(", ")}."
        }
        if (bits.isEmpty()) {
            return if (won.isNotEmpty()) "Ставки сыграли!" else "Касса в плюсе."
        }
        return bits.joinToString(" ")
    }
}

