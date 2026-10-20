package com.drquizman.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class Phase {
    LOBBY,
    TITLE,
    INTRO,
    TOPIC,
    BET,
    QUESTION,
    REVEAL,
    GAME_OVER,
}

@Serializable
enum class CompetitionType {
    FIRST,
    TRUE_FALSE,
    SPEED,
    BOMB,
    WHO_IS_RIGHT,
    EAST,
    BETS,
    PUZZLE,
    FINAL,
}

@Serializable
enum class HostMood {
    IDLE,
    TALK,
    HAPPY,
    SAD,
    HYPE,
}

@Serializable
data class PlayerState(
    val id: String,
    val name: String,
    val colorIndex: Int,
    val avatarIndex: Int = 0,
    val score: Int,
    val ready: Boolean,
    val answered: Boolean = false,
    val lastCorrect: Boolean? = null,
    val eliminated: Boolean = false,
    val finalTimeMs: Long = 0,
    val finalFrozen: Boolean = false,
    /** Верные в текущем конкурсе, если у механики есть шкала (например, гонка до 7). */
    val contestMarks: Int = 0,
    /** Ставка на текущий вопрос. 0 — не ставил. */
    val bet: Int = 0,
    /** Пульт на связи. false — вкладка закрылась, кресло ещё держим. */
    val connected: Boolean = true,
)

/** Вход или возврат в кресло: снимок игрока и секрет только для пульта. */
data class PlayerSeat(
    val player: PlayerState,
    val resumeToken: String,
)

@Serializable
data class QuestionView(
    val id: Int,
    val text: String,
    val answers: List<String>,
    val trueFalse: Boolean,
    val correctIndex: Int? = null,
    val topic: String = "",
)

@Serializable
data class GameSnapshot(
    val pin: String,
    val hostAddress: String = "",
    val wifiName: String = "",
    val phase: Phase,
    val players: List<PlayerState>,
    val competition: CompetitionType? = null,
    val competitionTitle: String = "",
    val competitionSlogan: String = "",
    val questionIndex: Int = 0,
    val questionTotal: Int = 0,
    val hostLine: String,
    val hostMood: HostMood,
    val question: QuestionView? = null,
    val timeLeftMs: Long = 0,
    val timeLimitMs: Long = 0,
    val paused: Boolean = false,
    val canSkipIntro: Boolean = false,
    val winnerId: String? = null,
    val winnerName: String? = null,
    val maxPlayers: Int = 6,
    val topicChooserId: String? = null,
    val topics: List<String> = emptyList(),
    val chosenTopic: String = "",
    val showTimer: Boolean = true,
    val bombHolderId: String? = null,
    val bombStrikes: Int = 0,
    val bombExploded: Boolean = false,
    /** Верх колбы в финале: стартовое время лидера, чтобы шкалы были сравнимы. */
    val finalTimeCapMs: Long = 0,
    /** Цель шкалы на плашке (7 в «Кто прав»). 0 — шкалы нет. */
    val contestMarksGoal: Int = 0,
    /** Ставка за верный ответ прямо сейчас (восток). 0 — не показываем. */
    val livePrize: Int = 0,
    /** Пока тикает вопрос, ответы на экране справа налево. */
    val reverseAnswers: Boolean = false,
    /** Пока тикает вопрос, ответы закрыты кусками пазла. */
    val puzzleAnswers: Boolean = false,
    /** Окно ставок открыто (после объявления темы). */
    val bettingOpen: Boolean = false,
    val betAmounts: List<Int> = emptyList(),
)

data class Question(
    val id: Int,
    val text: String,
    val answers: List<String>,
    val correctIndex: Int,
    val topic: String = "",
    val trueFalse: Boolean = false,
) {
    /** Перемешивает варианты ответа и обновляет [correctIndex] для текущего показа вопроса. */
    fun withShuffledAnswers(random: kotlin.random.Random = kotlin.random.Random.Default): Question {
        if (answers.size <= 1) return this
        val order = answers.indices.toMutableList().also { it.shuffle(random) }
        return copy(
            answers = order.map { answers[it] },
            correctIndex = order.indexOf(correctIndex),
        )
    }
}
