package com.drquizman.core.mechanic.east

import com.drquizman.core.i18n.LocalizedText

import com.drquizman.core.mechanic.MechanicHandler
import com.drquizman.core.mechanic.MechanicOutcome
import com.drquizman.core.mechanic.MechanicView
import com.drquizman.core.model.HostMood

/**
 * «Восток — дело тонкое» — 10 вопросов × 20 с.
 *
 * Ответы на экране справа налево. Верный ответ: 400 минус 20 за каждую секунду
 * (округление до 0,1 с). Ошибка и молчание — 0.
 * Ведущий комментирует не каждый вопрос, а примерно раз в 2–3.
 */
class EastMechanic : MechanicHandler {
    private var questionNo = 0
    private var nextCommentAt = 0

    override fun reset() {
        questionNo = 0
        nextCommentAt = (2..3).random()
    }

    override fun onRoundStart(playerIds: List<String>) = reset()

    override fun onPlayerLeft(id: String, remainingIds: List<String>) = Unit

    override fun nextTimeLimitMs(): Long = TIME_MS

    override fun canAnswer(playerId: String): Boolean = true

    override fun exclusiveAnswers(): Boolean = false

    override fun handleAnswer(
        playerId: String,
        correct: Boolean,
        playerIds: List<String>,
        names: Map<String, String>,
        elapsedMs: Long,
    ): MechanicOutcome = MechanicOutcome("", HostMood.TALK, view = view())

    override fun handleTimeout(
        playerIds: List<String>,
        names: Map<String, String>,
    ): MechanicOutcome = resolveQuestion(playerIds.associateWith { null }, names, emptyMap())

    override fun resolveQuestion(
        results: Map<String, Boolean?>,
        names: Map<String, String>,
        elapsedMs: Map<String, Long>,
    ): MechanicOutcome {
        val deltas = linkedMapOf<String, Int>()
        val scored = mutableListOf<Pair<String, Int>>()
        results.forEach { (id, correct) ->
            if (correct != true) return@forEach
            val prize = prize(elapsedMs[id] ?: TIME_MS)
            if (prize <= 0) return@forEach
            deltas[id] = prize
            scored += (names[id] ?: LocalizedText.get("EastMechanic.001")) to prize
        }
        questionNo += 1
        val comment = if (questionNo >= nextCommentAt) {
            nextCommentAt = questionNo + (2..3).random()
            EastLines.results(scored)
        } else {
            ""
        }
        return MechanicOutcome(
            hostLine = comment,
            hostMood = if (scored.isNotEmpty()) HostMood.HAPPY else HostMood.SAD,
            scoreDeltas = deltas,
            view = view(),
        )
    }

    override fun questionPrefix(names: Map<String, String>): String? = null

    override fun view(): MechanicView = MechanicView()

    override fun livePrize(elapsedMs: Long): Int = prize(elapsedMs)

    companion object {
        const val QUESTIONS = 10
        const val TIME_MS = 20_000L
        const val START = 400
        const val PER_SECOND = 20

        /** 400 − 20×секунды, шаг 0,1 с. */
        fun prize(elapsedMs: Long): Int {
            val tenths = ((elapsedMs.coerceAtLeast(0) + 50) / 100).toInt()
            return (START - tenths * PER_SECOND / 10).coerceAtLeast(0)
        }
    }
}
