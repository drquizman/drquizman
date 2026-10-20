package com.drquizman.core.engine

import com.drquizman.core.i18n.LocalizedText

import com.drquizman.core.host.HostLines
import com.drquizman.core.mechanic.MechanicCatalog
import com.drquizman.core.mechanic.MechanicHandler
import com.drquizman.core.mechanic.MechanicOutcome
import com.drquizman.core.mechanic.MechanicView
import com.drquizman.core.mechanic.bets.BetsMechanic
import com.drquizman.core.model.CompetitionType
import com.drquizman.core.model.GameSnapshot
import com.drquizman.core.model.HostMood
import com.drquizman.core.model.Phase
import com.drquizman.core.model.PlayerSeat
import com.drquizman.core.model.PlayerState
import com.drquizman.core.model.Question
import com.drquizman.core.model.QuestionView
import com.drquizman.core.questions.QuestionBank
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.max

class GameEngine(
    private val bank: QuestionBank,
    private val scope: CoroutineScope,
    private val onSnapshot: (GameSnapshot) -> Unit,
) {
    var pin: String = (1000..9999).random().toString()
        private set
    var hostAddress: String = ""
    var wifiName: String = ""

    private val players = linkedMapOf<String, PlayerInternal>()
    private var phase = Phase.LOBBY
    private var hostLine = HostLines.welcome()
    private var hostMood = HostMood.TALK
    private var competitions = listOf<CompetitionType>()
    private var plannedRounds: List<CompetitionType> = emptyList()
    private var showPlan: ShowPlan = ShowPlan.Standard
    private var competitionIndex = -1
    private var questionsLeft = 0
    private var questionTotal = 0
    private var questionIndexInComp = 0
    private var currentQuestion: Question? = null
    private var timeLeftMs = 0L
    private var timeLimitMs = 0L
    private var paused = false
    private var canSkipIntro = false
    private var winnerId: String? = null
    private var correctOrder = mutableListOf<String>()
    private var timerJob: Job? = null
    private var tickerJob: Job? = null
    private var lobbyChatJob: Job? = null
    private var waitingForSpeech = false
    private var holdEnd: (() -> Unit)? = null
    private var chosenTopic = ""
    private var topicChooserId: String? = null
    private var offeredTopics = listOf<String>()
    private val usedTopics = mutableSetOf<String>()
    private lateinit var mcQueue: ArrayDeque<Question>
    private lateinit var tfQueue: ArrayDeque<Question>
    private var mechanic: MechanicHandler? = null
    private var mechanicView = MechanicView()
    private var pendingOutcome: MechanicOutcome? = null
    private var finalTimeCapMs = 0L
    private var finalQuestionElapsedMs = 0L
    private var bettingOpen = false

    fun snapshot(): GameSnapshot {
        val q = currentQuestion
        val reveal = phase == Phase.REVEAL || phase == Phase.GAME_OVER
        return GameSnapshot(
            pin = pin,
            hostAddress = hostAddress,
            wifiName = wifiName,
            phase = phase,
            players = players.values.map {
                it.toState(reveal, mechanicView.marks[it.id] ?: 0, mechanicView.bets[it.id] ?: 0)
            },
            competition = currentCompetition(),
            competitionTitle = currentCompetition()?.let { HostLines.titles(it) }.orEmpty(),
            competitionSlogan = currentCompetition()?.let { MechanicCatalog[it]?.slogan }.orEmpty(),
            questionIndex = questionIndexInComp,
            questionTotal = questionTotal,
            hostLine = hostLine,
            hostMood = hostMood,
            question = q?.let {
                QuestionView(
                    id = it.id,
                    text = it.text,
                    answers = it.answers,
                    trueFalse = it.trueFalse,
                    correctIndex = if (reveal) it.correctIndex else null,
                    topic = it.topic,
                )
            },
            timeLeftMs = timeLeftMs,
            timeLimitMs = timeLimitMs,
            paused = paused,
            canSkipIntro = canSkipIntro &&
                (phase == Phase.TITLE || phase == Phase.INTRO || phase == Phase.TOPIC ||
                    (phase == Phase.BET && !bettingOpen)),
            winnerId = winnerId,
            winnerName = winnerId?.let { players[it]?.name },
            topicChooserId = if (phase == Phase.TOPIC) topicChooserId else null,
            topics = if (phase == Phase.TOPIC) offeredTopics else emptyList(),
            chosenTopic = chosenTopic,
            showTimer = showPublicTimer(),
            bombHolderId = if (phase == Phase.QUESTION || phase == Phase.REVEAL) mechanicView.holderId else null,
            bombStrikes = mechanicView.strikes,
            bombExploded = mechanicView.exploded && phase == Phase.REVEAL,
            finalTimeCapMs = if (currentCompetition() == CompetitionType.FINAL) finalTimeCapMs else 0L,
            contestMarksGoal = mechanicView.marksGoal,
            livePrize = if (phase == Phase.QUESTION) {
                mechanic?.livePrize((timeLimitMs - timeLeftMs).coerceAtLeast(0)) ?: 0
            } else {
                0
            },
            reverseAnswers = MechanicCatalog[currentCompetition()]?.reverseAnswers == true &&
                phase == Phase.QUESTION,
            puzzleAnswers = MechanicCatalog[currentCompetition()]?.puzzleAnswers == true &&
                phase == Phase.QUESTION,
            bettingOpen = phase == Phase.BET && bettingOpen,
            betAmounts = if (phase == Phase.BET) BetsMechanic.AMOUNTS else emptyList(),
        )
    }

    private fun publish() = onSnapshot(snapshot())

    fun addPlayer(name: String): Result<PlayerSeat> {
        if (phase != Phase.LOBBY) return Result.failure(IllegalStateException(LocalizedText.get("GameEngine.001")))
        if (players.size >= MAX_PLAYERS) return Result.failure(IllegalStateException(LocalizedText.get("GameEngine.002", (MAX_PLAYERS))))
        val color = (0 until MAX_PLAYERS).first { idx -> players.values.none { it.colorIndex == idx } }
        val avatar = (0 until AVATAR_COUNT).first { idx -> players.values.none { it.avatarIndex == idx } }
        val id = UUID.randomUUID().toString()
        val token = UUID.randomUUID().toString()
        val p = PlayerInternal(
            id,
            name.ifBlank { LocalizedText.get("GameEngine.003", (players.size + 1)) }.take(16),
            color,
            avatar,
            resumeToken = token,
        )
        players[id] = p
        hostLine = HostLines.playerJoined(p.name, first = players.size == 1)
        hostMood = HostMood.HAPPY
        publish()
        pokeLobbyChatter()
        return Result.success(PlayerSeat(p.toState(), token))
    }

    fun resumePlayer(token: String): Result<PlayerSeat> {
        val key = token.trim()
        if (key.isEmpty()) return Result.failure(IllegalStateException(LocalizedText.get("GameEngine.004")))
        val p = players.values.find { it.resumeToken == key }
            ?: return Result.failure(IllegalStateException(LocalizedText.get("GameEngine.005")))
        p.dropJob?.cancel()
        p.dropJob = null
        p.connected = true
        publish()
        return Result.success(PlayerSeat(p.toState(), p.resumeToken))
    }

    fun markDisconnected(id: String) {
        val p = players[id] ?: return
        if (!p.connected) return
        p.connected = false
        publish()
        val wait = if (phase == Phase.LOBBY) DISCONNECT_LOBBY_MS else DISCONNECT_PLAY_MS
        scheduleDrop(p, wait)
    }

    fun removePlayer(id: String) {
        val gone = players.remove(id) ?: return
        gone.dropJob?.cancel()
        gone.dropJob = null
        if (phase == Phase.LOBBY) {
            hostLine = HostLines.lobbyIdle(players.size)
            hostMood = HostMood.IDLE
            if (players.isNotEmpty() && players.values.all { it.ready }) {
                beginMatch()
                return
            }
            pokeLobbyChatter()
            publish()
            return
        }
        if (players.isEmpty() && phase != Phase.LOBBY) {
            resetToLobby(LocalizedText.get("GameEngine.006"))
            return
        }
        if (phase == Phase.TOPIC && id == topicChooserId) {
            val fallback = offeredTopics.randomOrNull()
            if (fallback != null) applyTopic(fallback) else beginQuestions()
            return
        }
        mechanic?.onPlayerLeft(id, activePlayers().map { it.id })
        mechanicView = mechanic?.view() ?: MechanicView()
        if (phase == Phase.BET && bettingOpen) {
            tryCloseBets()
            return
        }
        publish()
    }

    fun setAvatar(id: String, index: Int) {
        if (phase != Phase.LOBBY) return
        if (index !in 0 until AVATAR_COUNT) return
        if (players.values.any { it.id != id && it.avatarIndex == index }) return
        val p = players[id] ?: return
        p.avatarIndex = index
        publish()
    }

    fun setReady(id: String, ready: Boolean) {
        val p = players[id] ?: return
        if (phase != Phase.LOBBY) return
        p.ready = ready
        val readyCount = players.values.count { it.ready }
        hostLine = HostLines.lobbyWaiting(players.size, readyCount)
        hostMood = HostMood.TALK
        if (players.isNotEmpty() && players.values.all { it.ready }) {
            beginMatch()
        } else {
            publish()
            pokeLobbyChatter()
        }
    }

    fun setPlan(plan: ShowPlan) {
        showPlan = plan
    }

    private fun beginMatch() {
        if (phase != Phase.LOBBY) return
        if (players.isEmpty()) return
        lobbyChatJob?.cancel()
        lobbyChatJob = null
        mcQueue = bank.shuffledMc()
        tfQueue = bank.shuffledTf()
        plannedRounds = showPlan.deal()
        competitions = plannedRounds.ifEmpty { MatchPlan.standard() }
        competitionIndex = -1
        winnerId = null
        paused = false
        chosenTopic = ""
        usedTopics.clear()
        topicChooserId = null
        offeredTopics = emptyList()
        phase = Phase.INTRO
        canSkipIntro = true
        currentQuestion = null
        if (CompetitionType.FIRST !in competitions) {
            players.values.forEach { it.score = STARTING_GRANT }
            hostLine = HostLines.matchStartWithGrant(STARTING_GRANT)
        } else {
            hostLine = HostLines.matchStart()
        }
        hostMood = HostMood.HYPE
        players.values.filter { !it.connected }.forEach { scheduleDrop(it, DISCONNECT_PLAY_MS) }
        holdForSpeech(INTRO_MS) {
            canSkipIntro = false
            startNextCompetition()
        }
    }

    fun skipIntro() {
        if (phase == Phase.BET && !bettingOpen && canSkipIntro) {
            canSkipIntro = false
            waitingForSpeech = false
            timerJob?.cancel()
            openBetting()
            return
        }
        if (phase == Phase.TOPIC && canSkipIntro) {
            canSkipIntro = false
            waitingForSpeech = false
            val fallback = offeredTopics.randomOrNull() ?: return
            applyTopic(fallback)
            return
        }
        if (phase == Phase.TITLE) {
            canSkipIntro = false
            waitingForSpeech = false
            timerJob?.cancel()
            afterCompetitionIntro()
            return
        }
        if (phase != Phase.INTRO || !canSkipIntro) return
        canSkipIntro = false
        waitingForSpeech = false
        timerJob?.cancel()
        when {
            competitionIndex < 0 -> startNextCompetition()
            mechanic?.finished() == true -> startNextCompetition()
            questionsLeft <= 0 && currentCompetition() != CompetitionType.FINAL -> startNextCompetition()
            else -> afterCompetitionIntro()
        }
    }

    /** TTS договорил: не держим экран по запасу, только короткая пауза. */
    fun onSpeechFinished() {
        if (phase == Phase.LOBBY) {
            startLobbyChatter(afterSpeech = true)
            return
        }
        if (!waitingForSpeech) return
        if (paused) return
        when (phase) {
            Phase.INTRO, Phase.REVEAL, Phase.GAME_OVER -> Unit
            Phase.BET -> if (bettingOpen) return else Unit
            else -> return
        }
        val end = holdEnd ?: return
        if (timeLeftMs <= AFTER_SPEECH_MS) return
        runTimer(AFTER_SPEECH_MS, end)
    }

    fun togglePauseFromRemote() {
        if (phase == Phase.LOBBY || phase == Phase.GAME_OVER) return
        paused = !paused
        hostLine = if (paused) LocalizedText.get("GameEngine.007") else LocalizedText.get("GameEngine.008")
        hostMood = HostMood.TALK
        publish()
    }

    fun submitAnswer(playerId: String, questionId: Int, index: Int) {
        if (paused || phase != Phase.QUESTION) return
        val p = players[playerId] ?: return
        val q = currentQuestion ?: return
        if (p.eliminated || p.answered) return
        if (q.id != questionId) return
        if (index !in q.answers.indices) return
        val handler = mechanic
        if (handler != null && !handler.canAnswer(playerId)) return

        val correct = index == q.correctIndex
        p.answered = true
        p.lastCorrect = correct
        p.answerElapsedMs = (timeLimitMs - timeLeftMs).coerceAtLeast(0)
        if (correct) correctOrder.add(playerId)

        if (handler != null && handler.exclusiveAnswers()) {
            val outcome = handler.handleAnswer(
                playerId,
                correct,
                activeIds(),
                playerNames(),
                p.answerElapsedMs,
            )
            applyMechanicOutcome(outcome)
            reveal("mechanic")
            return
        }

        val comp = currentCompetition()
        if (comp == CompetitionType.FINAL) {
            p.finalFrozen = true
            when {
                !correct -> p.finalTimeMs = max(0, p.finalTimeMs - WRONG_PENALTY_MS)
                correctOrder.size == 1 ->
                    p.finalTimeMs = (p.finalTimeMs + FIRST_BONUS_MS).coerceAtMost(MAX_FINAL_MS)
            }
            if (p.finalTimeMs <= 0) p.eliminated = true
            checkFinalWinner()
            if (phase == Phase.GAME_OVER) return
        }

        val active = activePlayers()
        if (active.isNotEmpty() && active.none { waitingForAnswer(it) }) {
            reveal("all")
        } else {
            publish()
        }
    }

    fun placeBet(playerId: String, amount: Int) {
        if (paused || phase != Phase.BET || !bettingOpen) return
        val p = players[playerId] ?: return
        if (p.eliminated) return
        val handler = mechanic ?: return
        if (!handler.placeBet(playerId, amount)) return
        mechanicView = handler.view()
        publish()
        tryCloseBets()
    }

    private fun startNextCompetition() {
        cancelTimers()
        competitionIndex += 1
        if (competitionIndex >= competitions.size) {
            finishByScore()
            return
        }
        val type = competitions[competitionIndex]
        val spec = MechanicCatalog[type]
        mechanic = MechanicCatalog.createHandler(type)
        mechanicView = MechanicView()
        pendingOutcome = null
        bettingOpen = false
        mechanic?.onRoundStart(activeIds())
        mechanicView = mechanic?.view() ?: MechanicView()
        questionTotal = spec?.questionCount ?: QUESTIONS_PER_ROUND
        questionsLeft = questionTotal
        questionIndexInComp = 0
        chosenTopic = ""
        topicChooserId = null
        offeredTopics = emptyList()
        if (type == CompetitionType.FINAL) {
            players.values.forEach {
                it.finalTimeMs = finalTimeForScore(it.score)
                it.finalFrozen = false
                it.eliminated = it.finalTimeMs <= 0
            }
            finalTimeCapMs = players.values.maxOfOrNull { it.finalTimeMs } ?: MIN_FINAL_MS
        } else {
            finalTimeCapMs = 0L
        }
        phase = Phase.TITLE
        canSkipIntro = false
        waitingForSpeech = false
        holdEnd = null
        hostLine = HostLines.titles(type)
        hostMood = HostMood.HYPE
        currentQuestion = null
        timeLimitMs = TITLE_MS
        timeLeftMs = TITLE_MS
        publish()
        runTimer(TITLE_MS) {
            startCompetitionSpeech()
        }
    }

    private fun startCompetitionSpeech() {
        val type = currentCompetition() ?: return startNextCompetition()
        phase = Phase.INTRO
        canSkipIntro = true
        hostLine = HostLines.intro(type)
        hostMood = HostMood.HYPE
        currentQuestion = null
        holdForSpeech(INTRO_MS) {
            canSkipIntro = false
            afterCompetitionIntro()
        }
    }

    private fun afterCompetitionIntro() {
        val type = currentCompetition()
        if (type != null && type.needsTopicPick && chosenTopic.isEmpty()) {
            startTopicPick()
        } else {
            beginQuestions()
        }
    }

    fun pickTopic(playerId: String, topic: String) {
        if (paused || phase != Phase.TOPIC) return
        if (playerId != topicChooserId) return
        if (topic !in offeredTopics) return
        applyTopic(topic)
    }

    private fun startTopicPick() {
        cancelTimers()
        currentQuestion = null
        val pool = players.values.toList()
        if (pool.isEmpty()) {
            beginQuestions()
            return
        }
        val min = pool.minOf { it.score }
        val last = pool.filter { it.score == min }
        val lastPlace = last.size < pool.size
        val chooser = last.random()
        topicChooserId = chooser.id
        offeredTopics = offerTopics()
        if (offeredTopics.size <= 1) {
            applyTopic(offeredTopics.firstOrNull().orEmpty())
            return
        }
        phase = Phase.TOPIC
        canSkipIntro = true
        hostLine = HostLines.chooseTopic(chooser.name, lastPlace)
        hostMood = HostMood.TALK
        timeLimitMs = TOPIC_PICK_MS
        timeLeftMs = TOPIC_PICK_MS
        publish()
        runTimer(TOPIC_PICK_MS) {
            if (phase == Phase.TOPIC) applyTopic(offeredTopics.random())
        }
    }

    private fun offerTopics(): List<String> {
        val required = MechanicCatalog[currentCompetition()]?.questionCount ?: QUESTIONS_PER_ROUND
        val all = if (currentCompetition() == CompetitionType.EAST) {
            bank.topicsForEastWithAtLeast(required)
        } else {
            bank.topicsWithAtLeast(required)
        }
        val fresh = all.filter { it !in usedTopics }
        return (fresh.ifEmpty { all }).shuffled().take(TOPIC_CHOICES)
    }

    private fun applyTopic(topic: String) {
        cancelTimers()
        if (topic.isNotBlank()) {
            chosenTopic = topic
            usedTopics += topic
            mcQueue = if (currentCompetition() == CompetitionType.EAST) {
                bank.shuffledMcForEast(topic)
            } else {
                bank.shuffledMc(topic)
            }
        }
        topicChooserId = null
        offeredTopics = emptyList()
        phase = Phase.INTRO
        canSkipIntro = true
        val type = currentCompetition()
        hostLine = when {
            chosenTopic.isNotBlank() -> HostLines.topicPicked(chosenTopic)
            type != null -> HostLines.intro(type)
            else -> {
                beginQuestions()
                return
            }
        }
        hostMood = HostMood.HYPE
        holdForSpeech(TOPIC_CONFIRM_MS, 8_000L) {
            canSkipIntro = false
            beginQuestions()
        }
    }

    private fun announceStandingsThenNext() {
        cancelTimers()
        currentQuestion = null
        phase = Phase.INTRO
        canSkipIntro = true
        val ranked = players.values.sortedByDescending { it.score }.map { it.name }
        hostLine = HostLines.standings(ranked)
        hostMood = HostMood.TALK
        holdForSpeech(STANDINGS_MS, 28_000L) {
            canSkipIntro = false
            startNextCompetition()
        }
    }

    private fun beginQuestions() {
        canSkipIntro = false
        waitingForSpeech = false
        startNextQuestion()
    }

    private fun startNextQuestion() {
        cancelTimers()
        val type = currentCompetition() ?: return finishByScore()
        if (mechanic?.finished() == true) {
            announceStandingsThenNext()
            return
        }
        if (type != CompetitionType.FINAL && questionsLeft <= 0) {
            announceStandingsThenNext()
            return
        }
        if (type == CompetitionType.FINAL && activePlayers().size <= 1) {
            finishFinal()
            return
        }

        val spec = MechanicCatalog[type]
        if (spec?.perQuestionTopic == true) {
            startPerQuestionTopic()
            return
        }
        presentQuestion(bumpIndex = true)
    }

    private fun startPerQuestionTopic() {
        val type = currentCompetition() ?: return startNextCompetition()
        val topic = pickRandomTopic()
        if (topic.isNotBlank()) {
            chosenTopic = topic
            usedTopics += topic
            mcQueue = bank.shuffledMc(topic)
        } else {
            chosenTopic = ""
            mcQueue = bank.shuffledMc()
        }
        mechanic?.onBetWindowStart()
        mechanicView = mechanic?.view() ?: MechanicView()
        players.values.forEach {
            it.answered = false
            it.lastCorrect = null
            it.answerElapsedMs = 0
        }
        correctOrder.clear()
        currentQuestion = null
        questionIndexInComp += 1
        if (type != CompetitionType.FINAL) questionsLeft -= 1
        bettingOpen = false
        phase = Phase.BET
        val announce = mechanic?.betAnnounce(
            chosenTopic.ifBlank { LocalizedText.get("GameEngine.009") },
            questionIndexInComp,
            questionTotal,
        )
        if (!announce.isNullOrBlank()) {
            canSkipIntro = true
            hostLine = announce
            hostMood = HostMood.TALK
            holdForSpeech(TOPIC_CONFIRM_MS, 12_000L) {
                canSkipIntro = false
                openBetting()
            }
        } else {
            hostLine = ""
            hostMood = HostMood.TALK
            openBetting()
        }
    }

    private fun pickRandomTopic(): String {
        val all = bank.topicsWithAtLeast(1)
        val fresh = all.filter { it !in usedTopics }
        return (fresh.ifEmpty { all }).randomOrNull().orEmpty()
    }

    private fun openBetting() {
        if (phase != Phase.BET) return
        if (mechanic?.wantsBets() != true) {
            presentQuestion(bumpIndex = false)
            return
        }
        bettingOpen = true
        canSkipIntro = false
        waitingForSpeech = false
        holdEnd = null
        hostMood = HostMood.HYPE
        val limit = mechanic?.betWindowMs()?.takeIf { it > 0 } ?: BET_WINDOW_MS
        timeLimitMs = limit
        timeLeftMs = limit
        publish()
        runTimer(limit) {
            if (phase == Phase.BET) presentQuestion(bumpIndex = false)
        }
    }

    private fun tryCloseBets() {
        if (phase != Phase.BET || !bettingOpen) {
            publish()
            return
        }
        if (mechanic?.allBetsIn(activeIds()) == true) {
            presentQuestion(bumpIndex = false)
        } else {
            publish()
        }
    }

    private fun presentQuestion(bumpIndex: Boolean) {
        cancelTimers()
        val type = currentCompetition() ?: return finishByScore()
        bettingOpen = false
        waitingForSpeech = false
        holdEnd = null
        canSkipIntro = false

        val q = nextQuestion(type)?.withShuffledAnswers() ?: return startNextCompetition()
        currentQuestion = q
        if (bumpIndex) {
            questionIndexInComp += 1
            if (type != CompetitionType.FINAL) questionsLeft -= 1
            players.values.forEach {
                it.answered = false
                it.lastCorrect = null
                it.finalFrozen = false
                it.answerElapsedMs = 0
            }
            correctOrder.clear()
        }
        phase = Phase.QUESTION
        val flavor = if (type != CompetitionType.FINAL && mechanic?.wantsBets() != true) {
            HostLines.questionFlavor(questionIndexInComp, questionTotal)
        } else null
        val prefix = mechanic?.questionPrefix(playerNames())
        hostLine = listOfNotNull(flavor, prefix, q.text).joinToString(" ")
        hostMood = HostMood.TALK
        mechanicView = mechanic?.view() ?: MechanicView()

        if (type == CompetitionType.FINAL) {
            finalQuestionElapsedMs = 0L
            timeLimitMs = activePlayers().maxOfOrNull { it.finalTimeMs } ?: 0
            timeLeftMs = timeLimitMs
            publish()
            startFinalTicker()
        } else {
            timeLimitMs = mechanic?.nextTimeLimitMs() ?: when (type) {
                CompetitionType.TRUE_FALSE -> TRUE_FALSE_MS
                else -> STANDARD_MS
            }
            mechanicView = mechanic?.view() ?: mechanicView
            timeLeftMs = timeLimitMs
            publish()
            if (activePlayers().none { waitingForAnswer(it) }) {
                runTimer(AFTER_SPEECH_MS) { reveal("all") }
            } else {
                runTimer(timeLimitMs) { reveal("timeout") }
            }
        }
    }

    private fun reveal(reason: String) {
        if (phase != Phase.QUESTION) return
        cancelTimers()
        phase = Phase.REVEAL
        val handler = mechanic
        val outcome = when {
            handler != null && !handler.exclusiveAnswers() -> {
                val results = players.values.associate { it.id to it.lastCorrect }
                val elapsed = players.values.associate { it.id to it.answerElapsedMs }
                handler.resolveQuestion(results, playerNames(), elapsed).also { applyMechanicOutcome(it) }
            }
            handler != null && handler.exclusiveAnswers() -> {
                pendingOutcome ?: handler.handleTimeout(activeIds(), playerNames())
                    .also { applyMechanicOutcome(it) }
            }
            else -> pendingOutcome
        }
        pendingOutcome = null
        if (outcome != null) {
            hostLine = outcome.hostLine
            hostMood = outcome.hostMood
            mechanicView = outcome.view
        } else {
            applyScores()
            val active = activePlayers()
            val correctPlayers = active.filter { it.lastCorrect == true }
            val anyCorrect = correctPlayers.isNotEmpty()
            hostMood = when {
                reason == "timeout" && !anyCorrect -> HostMood.SAD
                anyCorrect -> HostMood.HAPPY
                else -> HostMood.SAD
            }
            hostLine = when {
                active.isNotEmpty() && correctPlayers.size == active.size -> HostLines.allCorrect()
                correctPlayers.size == 1 -> HostLines.oneCorrect(correctPlayers.first().name)
                correctPlayers.isEmpty() -> HostLines.allWrong()
                else -> HostLines.mixedResults()
            }
        }
        val revealHold = if (hostLine.isBlank()) SILENT_REVEAL_MS else REVEAL_MS
        holdForSpeech(revealHold, 10_000L) {
            if (phase == Phase.REVEAL) startNextQuestion()
        }
    }

    private fun applyScores() {
        when (currentCompetition()) {
            CompetitionType.FIRST -> players.values.filter { it.lastCorrect == true }.forEach { it.score += 200 }
            CompetitionType.TRUE_FALSE -> players.values.forEach { p ->
                when (p.lastCorrect) {
                    true -> p.score += 150
                    false -> p.score = max(0, p.score - 150)
                    null -> Unit
                }
            }
            CompetitionType.SPEED -> {
                val awards = intArrayOf(400, 300, 200, 100)
                correctOrder.take(4).forEachIndexed { i, id ->
                    players[id]?.let { it.score += awards[i] }
                }
            }
            CompetitionType.BOMB, CompetitionType.WHO_IS_RIGHT, CompetitionType.EAST,
            CompetitionType.BETS, CompetitionType.PUZZLE, CompetitionType.FINAL, null -> Unit
        }
    }

    private fun startFinalTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (phase == Phase.QUESTION && currentCompetition() == CompetitionType.FINAL) {
                delay(TICK_MS)
                if (paused) continue
                finalQuestionElapsedMs += TICK_MS
                var changed = false
                val drain = finalDrainMs()
                activePlayers().filter { !it.answered && !it.finalFrozen }.forEach { p ->
                    p.finalTimeMs = max(0, p.finalTimeMs - drain)
                    if (p.finalTimeMs <= 0) {
                        p.eliminated = true
                        changed = true
                    }
                }
                if (finalQuestionElapsedMs >= FINAL_ANSWER_TIMEOUT_MS) {
                    activePlayers().filter { !it.answered && !it.finalFrozen }.forEach { p ->
                        p.answered = true
                        p.lastCorrect = false
                        p.finalFrozen = true
                        p.answerElapsedMs = FINAL_ANSWER_TIMEOUT_MS
                        p.finalTimeMs = max(0, p.finalTimeMs - WRONG_PENALTY_MS)
                        if (p.finalTimeMs <= 0) {
                            p.eliminated = true
                            changed = true
                        }
                    }
                }
                timeLeftMs = activePlayers().filter { !it.finalFrozen }.maxOfOrNull { it.finalTimeMs } ?: 0
                if (changed) checkFinalWinner()
                if (phase != Phase.QUESTION) break
                val living = activePlayers()
                if (living.isNotEmpty() && living.none { waitingForAnswer(it) }) {
                    reveal("all")
                    break
                }
                publish()
            }
        }
    }

    private fun checkFinalWinner() {
        val alive = players.values.filter { !it.eliminated }
        if (alive.size == 1) {
            winnerId = alive.first().id
            endGame(HostLines.winner(alive.first().name))
        } else if (alive.isEmpty()) {
            finishByScore()
        }
    }

    private fun finishFinal() {
        val alive = players.values.filter { !it.eliminated }
        val champ = alive.maxByOrNull { it.finalTimeMs } ?: players.values.maxByOrNull { it.score }
        if (champ != null) {
            winnerId = champ.id
            endGame(HostLines.winner(champ.name))
        } else {
            finishByScore()
        }
    }

    private fun finishByScore() {
        val champ = players.values.maxByOrNull { it.score }
        winnerId = champ?.id
        endGame(champ?.let { HostLines.winner(it.name) } ?: LocalizedText.get("GameEngine.010"))
    }

    private fun endGame(line: String) {
        cancelTimers()
        phase = Phase.GAME_OVER
        hostLine = line
        hostMood = HostMood.HAPPY
        canSkipIntro = false
        holdForSpeech(WINNER_MS) { returnToLobby() }
    }

    fun skipWinner() {
        if (phase != Phase.GAME_OVER) return
        waitingForSpeech = false
        returnToLobby()
    }

    private fun returnToLobby() {
        val name = winnerId?.let { players[it]?.name }
        resetToLobby(HostLines.backToLobby(name))
    }

    fun shutdown() {
        cancelTimers()
        players.values.forEach {
            it.dropJob?.cancel()
            it.dropJob = null
        }
    }

    fun resetToLobby(line: String = HostLines.welcome()) {
        cancelTimers()
        phase = Phase.LOBBY
        competitions = emptyList()
        competitionIndex = -1
        currentQuestion = null
        winnerId = null
        paused = false
        canSkipIntro = false
        waitingForSpeech = false
        holdEnd = null
        chosenTopic = ""
        topicChooserId = null
        offeredTopics = emptyList()
        usedTopics.clear()
        mechanic = null
        mechanicView = MechanicView()
        pendingOutcome = null
        finalTimeCapMs = 0L
        finalQuestionElapsedMs = 0L
        bettingOpen = false
        players.values.forEach {
            it.score = 0
            it.ready = false
            it.answered = false
            it.lastCorrect = null
            it.eliminated = false
            it.finalTimeMs = 0
            it.finalFrozen = false
            it.answerElapsedMs = 0
        }
        hostLine = line
        hostMood = HostMood.TALK
        publish()
        pokeLobbyChatter()
    }

    private fun pokeLobbyChatter() = startLobbyChatter(afterSpeech = false)

    private fun startLobbyChatter(afterSpeech: Boolean) {
        lobbyChatJob?.cancel()
        if (phase != Phase.LOBBY) return
        lobbyChatJob = scope.launch {
            if (!afterSpeech) {
                delay(speechHoldMs(hostLine, 2_000L, 22_000L))
            }
            while (phase == Phase.LOBBY) {
                delay((LOBBY_CHAT_MIN_MS..LOBBY_CHAT_MAX_MS).random())
                if (phase != Phase.LOBBY) return@launch
                hostLine = HostLines.lobbyChatter(
                    players.size,
                    players.values.count { it.ready },
                    hostLine,
                )
                hostMood = HostMood.TALK
                publish()
            }
        }
    }

    private fun nextQuestion(type: CompetitionType): Question? {
        if (type == CompetitionType.TRUE_FALSE) {
            if (tfQueue.isEmpty()) tfQueue = bank.shuffledTf()
            return tfQueue.removeFirstOrNull()
        }
        if (mcQueue.isEmpty()) {
            mcQueue = when {
                type == CompetitionType.EAST -> bank.shuffledMcForEast(chosenTopic)
                chosenTopic.isNotBlank() -> bank.shuffledMc(chosenTopic)
                else -> bank.shuffledMc()
            }
        }
        return mcQueue.removeFirstOrNull()
    }

    private fun holdForSpeech(minMs: Long, maxMs: Long = 30_000L, onEnd: () -> Unit) {
        waitingForSpeech = true
        val fallback = speechHoldMs(hostLine, minMs, maxMs)
        holdEnd = {
            waitingForSpeech = false
            holdEnd = null
            onEnd()
        }
        val end = holdEnd!!
        timeLimitMs = fallback
        timeLeftMs = fallback
        publish()
        runTimer(fallback, end)
    }

    private fun runTimer(durationMs: Long, onEnd: () -> Unit) {
        timerJob?.cancel()
        timeLeftMs = durationMs
        timerJob = scope.launch {
            var left = durationMs
            while (left > 0) {
                delay(TICK_MS)
                if (paused) continue
                left -= TICK_MS
                timeLeftMs = max(0, left)
                publish()
            }
            if (!paused) onEnd()
        }
    }

    private fun cancelTimers() {
        timerJob?.cancel()
        tickerJob?.cancel()
        lobbyChatJob?.cancel()
        timerJob = null
        tickerJob = null
        lobbyChatJob = null
    }

    private fun currentCompetition(): CompetitionType? =
        competitions.getOrNull(competitionIndex)

    private val CompetitionType.needsTopicPick: Boolean
        get() = MechanicCatalog[this]?.needsTopicPick == true

    private fun showPublicTimer(): Boolean {
        if (phase == Phase.BET && bettingOpen) return true
        if (phase != Phase.QUESTION && phase != Phase.REVEAL) return false
        return MechanicCatalog[currentCompetition()]?.showPublicTimer == true
    }

    private fun applyMechanicOutcome(outcome: MechanicOutcome) {
        pendingOutcome = outcome
        mechanicView = outcome.view
        outcome.scoreDeltas.forEach { (id, delta) ->
            val p = players[id] ?: return@forEach
            p.score = max(0, p.score + delta)
        }
    }

    /** Каждые 2 вопроса финала время идёт на 25% быстрее, максимум в 3 раза. */
    private fun finalDrainMs(): Long {
        val completedQuestions = (questionIndexInComp - 1).coerceAtLeast(0)
        val steps = completedQuestions / FINAL_SPEEDUP_EVERY
        val percent = (100 + steps * FINAL_SPEEDUP_PCT).coerceAtMost(FINAL_MAX_SPEED_PCT)
        return TICK_MS * percent / 100
    }

    private fun waitingForAnswer(p: PlayerInternal): Boolean {
        if (p.answered || p.eliminated) return false
        val handler = mechanic
        return handler == null || handler.canAnswer(p.id)
    }

    private fun activeIds() = activePlayers().map { it.id }

    private fun playerNames() = players.values.associate { it.id to it.name }

    private fun activePlayers() = players.values.filter { !it.eliminated }

    private fun scheduleDrop(p: PlayerInternal, waitMs: Long) {
        p.dropJob?.cancel()
        val id = p.id
        p.dropJob = scope.launch {
            delay(waitMs)
            val cur = players[id] ?: return@launch
            if (!cur.connected) removePlayer(id)
        }
    }

    private class PlayerInternal(
        val id: String,
        var name: String,
        val colorIndex: Int,
        var avatarIndex: Int,
        val resumeToken: String,
        var score: Int = 0,
        var ready: Boolean = false,
        var answered: Boolean = false,
        var lastCorrect: Boolean? = null,
        var eliminated: Boolean = false,
        var finalTimeMs: Long = 0,
        var finalFrozen: Boolean = false,
        var answerElapsedMs: Long = 0,
        var connected: Boolean = true,
        var dropJob: Job? = null,
    ) {
        fun toState(showResult: Boolean = false, contestMarks: Int = 0, bet: Int = 0) = PlayerState(
            id, name, colorIndex, avatarIndex, score, ready, answered,
            lastCorrect = if (showResult) lastCorrect else null,
            eliminated, finalTimeMs, finalFrozen, contestMarks, bet,
            connected = connected,
        )
    }

    companion object {
        const val MAX_PLAYERS = 6
        const val AVATAR_COUNT = 10
        const val QUESTIONS_PER_ROUND = 8
        const val STARTING_GRANT = 1000
        const val STANDARD_MS = 20_000L
        const val TRUE_FALSE_MS = 15_000L
        const val INTRO_MS = 8_000L
        const val TITLE_MS = 3_000L
        const val LOBBY_CHAT_MIN_MS = 20_000L
        const val LOBBY_CHAT_MAX_MS = 30_000L
        const val REVEAL_MS = 3_500L
        const val SILENT_REVEAL_MS = 2_000L
        const val STANDINGS_MS = 6_000L
        const val WINNER_MS = 10_000L
        const val AFTER_SPEECH_MS = 700L
        const val TICK_MS = 100L
        /** Финал: 100 очков = 1 с, но запас всегда остаётся в пределах 10–60 с. */
        const val MS_PER_POINT = 10L
        const val MIN_FINAL_MS = 10_000L
        const val MAX_FINAL_MS = 60_000L
        const val WRONG_PENALTY_MS = 5_000L
        const val FIRST_BONUS_MS = 5_000L
        const val FINAL_ANSWER_TIMEOUT_MS = 20_000L
        const val FINAL_SPEEDUP_EVERY = 2
        const val FINAL_SPEEDUP_PCT = 25
        const val FINAL_MAX_SPEED_PCT = 300
        const val TOPIC_PICK_MS = 25_000L
        const val TOPIC_CONFIRM_MS = 3_000L
        const val TOPIC_CHOICES = 4
        const val BET_WINDOW_MS = 5_000L
        const val DISCONNECT_LOBBY_MS = 10_000L
        const val DISCONNECT_PLAY_MS = 30_000L

        fun finalTimeForScore(score: Int): Long =
            (score.coerceAtLeast(0).toLong() * MS_PER_POINT).coerceIn(MIN_FINAL_MS, MAX_FINAL_MS)

        fun speechHoldMs(text: String, minMs: Long, maxMs: Long = 45_000L): Long {
            // ~11 символов/с при темпе 0.95 — держим экран, пока TTS договаривает.
            return (text.length * 70L + 1_200L).coerceIn(minMs, maxMs)
        }
    }
}
