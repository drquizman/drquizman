package com.drquizman.core.mechanic.bets

import com.drquizman.core.i18n.LocalizedText

import com.drquizman.core.mechanic.MechanicHandler
import com.drquizman.core.mechanic.MechanicOutcome
import com.drquizman.core.mechanic.MechanicView
import com.drquizman.core.model.HostMood

/**
 * «Ставки сделаны» — 5 вопросов × 20 с.
 *
 * Тема случайная на каждый вопрос, на экране всегда. Ведущий объявляет тему
 * и комментирует итог в первый раз, потом примерно раз в 2–3 вопроса.
 * 5 с на ставку 100 / 200 / 300 / 400. Кто не успел — вопрос пропускает.
 * Верно +ставка, ошибка или молчание после ставки −ставка.
 */
class BetsMechanic : MechanicHandler {
    private val bets = linkedMapOf<String, Int>()
    private var questionNo = 0
    private var nextTalkAt = 1
    private var talkThis = false

    override fun reset() {
        bets.clear()
        questionNo = 0
        nextTalkAt = 1
        talkThis = false
    }

    override fun onRoundStart(playerIds: List<String>) = reset()

    override fun onPlayerLeft(id: String, remainingIds: List<String>) {
        bets.remove(id)
    }

    override fun nextTimeLimitMs(): Long = TIME_MS

    override fun canAnswer(playerId: String): Boolean = playerId in bets

    override fun exclusiveAnswers(): Boolean = false

    override fun wantsBets(): Boolean = true

    override fun onBetWindowStart() {
        bets.clear()
        questionNo += 1
        talkThis = questionNo >= nextTalkAt
    }

    override fun betAnnounce(topic: String, index: Int, total: Int): String? =
        if (talkThis) BetsLines.announceTopic(topic.ifBlank { LocalizedText.get("BetsMechanic.001") }, index, total) else null

    override fun placeBet(playerId: String, amount: Int): Boolean {
        if (amount !in AMOUNTS) return false
        if (playerId in bets) return false
        bets[playerId] = amount
        return true
    }

    override fun allBetsIn(playerIds: List<String>): Boolean =
        playerIds.isNotEmpty() && playerIds.all { it in bets }

    override fun betWindowMs(): Long = BET_MS

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
        val won = mutableListOf<Pair<String, Int>>()
        val lost = mutableListOf<Pair<String, Int>>()
        val skipped = mutableListOf<String>()
        results.forEach { (id, correct) ->
            val name = names[id] ?: LocalizedText.get("BetsMechanic.002")
            val bet = bets[id]
            if (bet == null) {
                skipped += name
                return@forEach
            }
            when (correct) {
                true -> {
                    deltas[id] = bet
                    won += name to bet
                }
                false, null -> {
                    deltas[id] = -bet
                    lost += name to bet
                }
            }
        }
        val comment = if (talkThis) {
            nextTalkAt = questionNo + (2..3).random()
            BetsLines.results(won, lost, skipped)
        } else {
            ""
        }
        return MechanicOutcome(
            hostLine = comment,
            hostMood = when {
                won.isNotEmpty() && lost.isEmpty() -> HostMood.HAPPY
                won.isEmpty() && (lost.isNotEmpty() || skipped.isNotEmpty()) -> HostMood.SAD
                else -> HostMood.TALK
            },
            scoreDeltas = deltas,
            view = view(),
        )
    }

    override fun questionPrefix(names: Map<String, String>): String? = null

    override fun view(): MechanicView = MechanicView(bets = HashMap(bets))

    companion object {
        const val QUESTIONS = 5
        const val TIME_MS = 20_000L
        const val BET_MS = 5_000L
        val AMOUNTS = listOf(100, 200, 300, 400)
    }
}
