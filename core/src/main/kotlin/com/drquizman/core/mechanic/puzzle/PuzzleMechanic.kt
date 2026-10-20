package com.drquizman.core.mechanic.puzzle

import com.drquizman.core.i18n.LocalizedText

import com.drquizman.core.mechanic.MechanicHandler
import com.drquizman.core.mechanic.MechanicOutcome
import com.drquizman.core.mechanic.MechanicView
import com.drquizman.core.model.HostMood

/**
 * «Пазл» — 10 вопросов × 20 с. Засчитывается первый нажавший.
 *
 * Верно: каждому оставшемуся сопернику +¼ пазла.
 * Ошибка: нажавшему тоже +¼.
 * 4 четверти — вылет из конкурса, не из шоу.
 * Дошедшие: 0 кусков 800, 1 → 400, 2 → 200, 3 → 100.
 */
class PuzzleMechanic : MechanicHandler {
    private val quarters = linkedMapOf<String, Int>()
    private var questionNo = 0
    private var done = false

    override fun reset() {
        quarters.clear()
        questionNo = 0
        done = false
    }

    override fun onRoundStart(playerIds: List<String>) {
        reset()
        playerIds.forEach { quarters[it] = 0 }
    }

    override fun onPlayerLeft(id: String, remainingIds: List<String>) {
        quarters.remove(id)
        if (remainingIds.isEmpty()) done = true
    }

    override fun nextTimeLimitMs(): Long = TIME_MS

    override fun canAnswer(playerId: String): Boolean =
        !done && inContest(playerId)

    override fun exclusiveAnswers(): Boolean = true

    override fun finished(): Boolean = done

    override fun handleAnswer(
        playerId: String,
        correct: Boolean,
        playerIds: List<String>,
        names: Map<String, String>,
        elapsedMs: Long,
    ): MechanicOutcome {
        if (done || !inContest(playerId)) {
            return MechanicOutcome("", HostMood.TALK, view = view())
        }
        val justOut = mutableListOf<String>()
        if (correct) {
            inContestIds().filter { it != playerId }.forEach { id ->
                if (addQuarters(id, QUARTER_ON_HIT)) justOut += id
            }
        } else if (addQuarters(playerId, QUARTER_ON_HIT)) {
            justOut += playerId
        }
        val name = names[playerId] ?: LocalizedText.get("PuzzleMechanic.001")
        val line = if (correct) PuzzleLines.correct(name) else PuzzleLines.wrong(name)
        val mood = if (correct) HostMood.HAPPY else HostMood.SAD
        return finishQuestion(names, line, mood, justOut)
    }

    override fun handleTimeout(
        playerIds: List<String>,
        names: Map<String, String>,
    ): MechanicOutcome {
        if (done) {
            return MechanicOutcome("", HostMood.TALK, view = view())
        }
        return finishQuestion(names, PuzzleLines.timeout(), HostMood.SAD, emptyList())
    }

    override fun questionPrefix(names: Map<String, String>): String? = null

    override fun view(): MechanicView = MechanicView(marks = quarters.toMap())

    private fun finishQuestion(
        names: Map<String, String>,
        line: String,
        mood: HostMood,
        justOut: List<String>,
    ): MechanicOutcome {
        questionNo += 1
        val outLine = PuzzleLines.out(justOut.map { names[it] ?: LocalizedText.get("PuzzleMechanic.002") })
        val spoken = listOf(line, outLine).filter { it.isNotBlank() }.joinToString(" ")
        val over = questionNo >= QUESTIONS || inContestIds().isEmpty()
        if (over) {
            return settle(names, spoken)
        }
        return MechanicOutcome(
            hostLine = spoken,
            hostMood = if (justOut.isNotEmpty()) HostMood.SAD else mood,
            view = view(),
        )
    }

    private fun settle(names: Map<String, String>, lead: String): MechanicOutcome {
        done = true
        val deltas = linkedMapOf<String, Int>()
        val payouts = mutableListOf<Pair<String, Int>>()
        quarters.forEach { (id, q) ->
            if (q >= MAX_QUARTERS) return@forEach
            val prize = prize(q)
            if (prize <= 0) return@forEach
            deltas[id] = prize
            payouts += (names[id] ?: LocalizedText.get("PuzzleMechanic.003")) to prize
        }
        return MechanicOutcome(
            hostLine = PuzzleLines.settle(lead, payouts),
            hostMood = if (payouts.isEmpty()) HostMood.SAD else HostMood.HYPE,
            scoreDeltas = deltas,
            view = view(),
        )
    }

    private fun addQuarters(id: String, n: Int): Boolean {
        val before = quarters[id] ?: 0
        if (before >= MAX_QUARTERS) return false
        val after = (before + n).coerceAtMost(MAX_QUARTERS)
        quarters[id] = after
        return after >= MAX_QUARTERS
    }

    private fun inContest(id: String): Boolean = (quarters[id] ?: MAX_QUARTERS) < MAX_QUARTERS

    private fun inContestIds(): List<String> = quarters.keys.filter { inContest(it) }

    companion object {
        const val QUESTIONS = 10
        const val TIME_MS = 20_000L
        const val MAX_QUARTERS = 4
        const val QUARTER_ON_HIT = 1

        fun prize(quarters: Int): Int = when (quarters) {
            0 -> 800
            1 -> 400
            2 -> 200
            3 -> 100
            else -> 0
        }
    }
}
