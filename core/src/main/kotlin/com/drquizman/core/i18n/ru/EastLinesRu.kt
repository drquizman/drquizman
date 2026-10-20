package com.drquizman.core.i18n.ru

/** Реплики конкурса «Восток — дело тонкое» из Quizman/host.txt и GDD. */
object EastLinesRu {
    const val TITLE = "ВОСТОК — ДЕЛО ТОНКОЕ"
    const val SLOGAN = "ЧЕМ БЫСТРЕЕ — ТЕМ ЖИРНЕЕ"

    fun intro(): String =
        "Отвечай как можно быстрее! Каждую секунду вычитаются очки! " +
            "Ответы написаны справа налево — восток, дело тонкое!"

    fun results(scored: List<Pair<String, Int>>): String {
        if (scored.isEmpty()) {
            return listOf(
                "Никто не прочитал. Восток оказался слишком тонким делом!",
                "Справа налево — это вам не слева направо. Ноль баллов.",
                "Все мимо. А ставки таяли с каждой секундой!",
            ).random()
        }
        if (scored.size == 1) {
            val (name, prize) = scored.first()
            return listOf(
                "$name прочитал справа налево! Плюс $prize!",
                "Только $name успел. Забирает $prize.",
                "$name, глаз намётан! Плюс $prize.",
            ).random()
        }
        val bits = scored
            .sortedByDescending { it.second }
            .joinToString(", ") { "${it.first} +${it.second}" }
        return listOf(
            "Успели: $bits.",
            "Прочитали: $bits.",
        ).random()
    }
}

