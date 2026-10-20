package com.drquizman.core.mechanic

import com.drquizman.core.model.CompetitionType
import com.drquizman.core.model.HostMood

/** Слот в составе шоу: старт и финал всегда на местах, середину набираем из каталога. */
enum class MechanicSlot { START, MID, END }

/**
 * Описание механики. Сама логика раунда — в [createHandler], если он задан.
 * Встроенные конкурсы (старт, верю/не верю, скорость, финал) живут в движке
 * и имеют `createHandler = null`.
 */
data class MechanicSpec(
    val type: CompetitionType,
    val slot: MechanicSlot,
    val title: String,
    val slogan: String = "",
    val intro: () -> String,
    val needsTopicPick: Boolean = false,
    val questionCount: Int = 8,
    val showPublicTimer: Boolean = true,
    val enabledByDefault: Boolean = true,
    val required: Boolean = false,
    /** Ответы на экране читаются справа налево, пока тикает вопрос. */
    val reverseAnswers: Boolean = false,
    /** Ответы на экране открываются кусками пазла, пока тикает вопрос. */
    val puzzleAnswers: Boolean = false,
    /** Случайная тема на каждый вопрос, без выбора игроками. */
    val perQuestionTopic: Boolean = false,
    val createHandler: (() -> MechanicHandler)? = null,
)

/** Состояние, которое механика отдаёт на экран и пульт. */
data class MechanicView(
    val holderId: String? = null,
    val strikes: Int = 0,
    val exploded: Boolean = false,
    val marks: Map<String, Int> = emptyMap(),
    val marksGoal: Int = 0,
    val bets: Map<String, Int> = emptyMap(),
)

data class MechanicOutcome(
    val hostLine: String,
    val hostMood: HostMood,
    val scoreDeltas: Map<String, Int> = emptyMap(),
    val exploded: Boolean = false,
    val view: MechanicView = MechanicView(),
)

/**
 * Живой обработчик раунда. Создаётся на конкурс, сбрасывается после него.
 * Движок спрашивает, кто может отвечать, сколько тикает фитиль и что делать с ответом.
 */
interface MechanicHandler {
    fun reset()
    fun onRoundStart(playerIds: List<String>)
    fun onPlayerLeft(id: String, remainingIds: List<String>)
    fun nextTimeLimitMs(): Long
    fun canAnswer(playerId: String): Boolean
    /** Бомба: ответ держателя сразу закрывает вопрос. «Кто прав»: ждут все. */
    fun exclusiveAnswers(): Boolean = true
    fun finished(): Boolean = false
    fun handleAnswer(
        playerId: String,
        correct: Boolean,
        playerIds: List<String>,
        names: Map<String, String>,
        elapsedMs: Long = 0L,
    ): MechanicOutcome
    fun handleTimeout(playerIds: List<String>, names: Map<String, String>): MechanicOutcome
    /** Итог вопроса, когда отвечают все. [results]: верно / неверно / молчание. */
    fun resolveQuestion(
        results: Map<String, Boolean?>,
        names: Map<String, String>,
        elapsedMs: Map<String, Long> = emptyMap(),
    ): MechanicOutcome = MechanicOutcome("", HostMood.TALK, view = view())
    fun questionPrefix(names: Map<String, String>): String?
    fun view(): MechanicView
    /** Текущая ставка за мгновенный верный ответ; 0 — шкалы нет. */
    fun livePrize(elapsedMs: Long): Int = 0
    fun wantsBets(): Boolean = false
    fun onBetWindowStart() {}
    fun placeBet(playerId: String, amount: Int): Boolean = false
    fun allBetsIn(playerIds: List<String>): Boolean = false
    fun betWindowMs(): Long = 0L
    /** Реплика перед ставками; null — тему только на экране, без голоса. */
    fun betAnnounce(topic: String, index: Int, total: Int): String? = null
}
