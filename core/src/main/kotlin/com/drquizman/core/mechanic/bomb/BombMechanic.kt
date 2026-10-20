package com.drquizman.core.mechanic.bomb

import com.drquizman.core.i18n.LocalizedText

import com.drquizman.core.mechanic.MechanicHandler
import com.drquizman.core.mechanic.MechanicOutcome
import com.drquizman.core.mechanic.MechanicView
import com.drquizman.core.model.HostMood
import kotlin.random.Random

/**
 * «Бомба» — отдельная механика по GDD.
 *
 * Отвечает только держатель. Верный ответ передаёт бомбу по кругу.
 * Два промаха подряд или сгорание скрытого фитиля — взрыв и штраф.
 * Подключается через [com.drquizman.core.mechanic.MechanicCatalog].
 */
class BombMechanic(
    private val random: Random = Random.Default,
) : MechanicHandler {
    private var holderId: String? = null
    private var strikes: Int = 0
    private var exploded: Boolean = false

    override fun reset() {
        holderId = null
        strikes = 0
        exploded = false
    }

    override fun onRoundStart(playerIds: List<String>) {
        reset()
        holderId = playerIds.randomOrNull(random)
    }

    override fun onPlayerLeft(id: String, remainingIds: List<String>) {
        if (holderId != id) return
        strikes = 0
        exploded = false
        holderId = remainingIds.randomOrNull(random)
    }

    override fun nextTimeLimitMs(): Long {
        exploded = false
        return random.nextLong(FUSE_MIN_MS, FUSE_MAX_MS + 1)
    }

    override fun canAnswer(playerId: String): Boolean = playerId == holderId

    override fun handleAnswer(
        playerId: String,
        correct: Boolean,
        playerIds: List<String>,
        names: Map<String, String>,
        elapsedMs: Long,
    ): MechanicOutcome {
        exploded = false
        val holder = holderId ?: playerId
        val holderName = names[holder] ?: LocalizedText.get("BombMechanic.001")
        if (correct) {
            strikes = 0
            if (playerIds.size <= 1) {
                return MechanicOutcome(
                    hostLine = BombLines.survived(holderName),
                    hostMood = HostMood.HAPPY,
                    view = view(),
                )
            }
            val next = nextHolder(holder, playerIds)
            holderId = next
            return MechanicOutcome(
                hostLine = BombLines.passed(holderName, names[next] ?: LocalizedText.get("BombMechanic.002")),
                hostMood = HostMood.HAPPY,
                view = view(),
            )
        }
        strikes += 1
        if (strikes >= WRONGS_TO_EXPLODE) return explode(playerIds, names)
        return MechanicOutcome(
            hostLine = BombLines.strike(holderName, WRONGS_TO_EXPLODE - strikes),
            hostMood = HostMood.SAD,
            view = view(),
        )
    }

    override fun handleTimeout(
        playerIds: List<String>,
        names: Map<String, String>,
    ): MechanicOutcome = explode(playerIds, names)

    override fun questionPrefix(names: Map<String, String>): String? {
        val id = holderId ?: return null
        val name = names[id] ?: return null
        return BombLines.yourTurn(name)
    }

    override fun view(): MechanicView =
        MechanicView(holderId = holderId, strikes = strikes, exploded = exploded)

    private fun explode(playerIds: List<String>, names: Map<String, String>): MechanicOutcome {
        val victim = holderId
        val victimName = victim?.let { names[it] } ?: LocalizedText.get("BombMechanic.003")
        exploded = true
        strikes = 0
        val next = victim?.let { nextHolder(it, playerIds) } ?: playerIds.randomOrNull(random)
        holderId = next
        return MechanicOutcome(
            hostLine = BombLines.exploded(victimName, PENALTY),
            hostMood = HostMood.SAD,
            scoreDeltas = if (victim != null) mapOf(victim to -PENALTY) else emptyMap(),
            exploded = true,
            view = MechanicView(holderId = victim, strikes = 0, exploded = true),
        )
    }

    private fun nextHolder(current: String, playerIds: List<String>): String {
        if (playerIds.isEmpty()) return current
        val i = playerIds.indexOf(current)
        if (i < 0) return playerIds.random(random)
        return playerIds[(i + 1) % playerIds.size]
    }

    companion object {
        const val PENALTY = 250
        const val WRONGS_TO_EXPLODE = 2
        const val FUSE_MIN_MS = 7_000L
        const val FUSE_MAX_MS = 16_000L
        const val QUESTIONS = 8
    }
}
