package com.drquizman.core.mechanic.who

import com.drquizman.core.i18n.LocalizedText

import com.drquizman.core.mechanic.MechanicHandler
import com.drquizman.core.mechanic.MechanicOutcome
import com.drquizman.core.mechanic.MechanicView
import com.drquizman.core.model.HostMood

/**
 * «Кто прав!?» — гонка до семи верных.
 *
 * Все отвечают, скорость не важна. Первый с 7 верными получает 1200.
 * Остальные: верные × 100. Если двое дошли до 7 в одном вопросе —
 * куш у того, кто раньше набрал 6.
 */
class WhoIsRightMechanic : MechanicHandler {
    private val marks = linkedMapOf<String, Int>()
    /** Номер вопроса (1-based), на котором игрок взял очередной верный. */
    private val markAt = linkedMapOf<String, MutableList<Int>>()
    private var questionNo = 0
    private var done = false

    override fun reset() {
        marks.clear()
        markAt.clear()
        questionNo = 0
        done = false
    }

    override fun onRoundStart(playerIds: List<String>) {
        reset()
        playerIds.forEach { marks[it] = 0 }
    }

    override fun onPlayerLeft(id: String, remainingIds: List<String>) {
        marks.remove(id)
        markAt.remove(id)
        if (remainingIds.isEmpty()) done = true
    }

    override fun nextTimeLimitMs(): Long = TIME_MS

    override fun canAnswer(playerId: String): Boolean = !done && playerId in marks

    override fun exclusiveAnswers(): Boolean = false

    override fun finished(): Boolean = done

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
        if (done) {
            return MechanicOutcome("", HostMood.TALK, view = view())
        }
        questionNo += 1
        results.forEach { (id, correct) ->
            if (id !in marks) marks[id] = 0
            if (correct == true) {
                val n = (marks[id] ?: 0) + 1
                marks[id] = n
                markAt.getOrPut(id) { mutableListOf() }.add(questionNo)
            }
        }
        val reached = marks.filter { it.value >= GOAL }.keys
        if (reached.isNotEmpty()) {
            return jackpot(pickWinner(reached), names)
        }
        if (questionNo >= CAP) {
            return settleWithoutJackpot(names)
        }
        return MechanicOutcome(
            hostLine = raceLine(names),
            hostMood = HostMood.HAPPY,
            view = view(),
        )
    }

    override fun questionPrefix(names: Map<String, String>): String? = null

    override fun view(): MechanicView =
        MechanicView(marks = marks.toMap(), marksGoal = GOAL)

    private fun jackpot(winnerId: String, names: Map<String, String>): MechanicOutcome {
        done = true
        val winnerName = names[winnerId] ?: LocalizedText.get("WhoIsRightMechanic.001")
        return MechanicOutcome(
            hostLine = WhoIsRightLines.jackpot(winnerName, JACKPOT),
            hostMood = HostMood.HYPE,
            scoreDeltas = payouts(winnerId),
            view = view(),
        )
    }

    private fun settleWithoutJackpot(names: Map<String, String>): MechanicOutcome {
        done = true
        val leaderId = marks.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key
        val leaderName = leaderId?.let { names[it] }
        return MechanicOutcome(
            hostLine = WhoIsRightLines.capOver(leaderName, PER_CORRECT),
            hostMood = HostMood.TALK,
            scoreDeltas = payouts(winnerId = null),
            view = view(),
        )
    }

    private fun payouts(winnerId: String?): Map<String, Int> =
        marks.mapValues { (id, n) ->
            if (id == winnerId) JACKPOT else n * PER_CORRECT
        }.filterValues { it != 0 }

    private fun pickWinner(candidates: Collection<String>): String =
        candidates.minWithOrNull(
            compareBy<String> { markQuestion(it, GOAL) }
                .thenBy { markQuestion(it, GOAL - 1) }
                .thenBy { markQuestion(it, GOAL - 2) }
                .thenBy { it },
        ) ?: candidates.first()

    private fun markQuestion(id: String, nth: Int): Int =
        markAt[id]?.getOrNull(nth - 1) ?: Int.MAX_VALUE

    private fun raceLine(names: Map<String, String>): String {
        val atSix = marks.filter { it.value == GOAL - 1 }.keys.map { names[it] ?: LocalizedText.get("WhoIsRightMechanic.002") }
        val leader = marks.maxByOrNull { it.value }?.takeIf { it.value > 0 }
        val leaderName = leader?.key?.let { names[it] }
        return WhoIsRightLines.race(leaderName, leader?.value ?: 0, atSix, GOAL)
    }

    companion object {
        const val GOAL = 7
        const val CAP = 20
        const val JACKPOT = 1200
        const val PER_CORRECT = 100
        const val TIME_MS = 20_000L
    }
}
