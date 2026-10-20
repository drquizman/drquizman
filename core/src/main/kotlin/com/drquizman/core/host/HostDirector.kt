package com.drquizman.core.host

import com.drquizman.core.model.GameSnapshot
import com.drquizman.core.model.Phase
import com.drquizman.core.model.HostMood
import kotlin.math.abs
import kotlin.math.exp
import kotlin.random.Random

/** Presentation only: never advances game phases or changes scores / game timers. */
class HostDirector(private val random: Random = Random.Default) {
    enum class Shot { FULL, MEDIUM, CLOSE }
    data class Frame(val clip: String, val seconds: Float, val previousClip: String?,
        val previousSeconds: Float, val blend: Float, val shot: Shot, val mouth: Float, val blink: Float)
    private var event = ""
    private var lastSnapshot: GameSnapshot? = null
    private var cachedKey = "menu"
    private var hasLaugh = false
    private var lastPhase: Phase? = null
    private var playerCount = 0
    private var eventAge = 0f
    private var clock = 0f
    private var speechSeen = false
    private var afterSpeech = 0f
    private var greeting = false
    private var clip = "wait"
    private var time = 0f
    private var previous: String? = null
    private var previousTime = 0f
    private var blendTime = FADE
    private val bags = mutableMapOf<List<String>, MutableList<String>>()
    private var poolSource: List<String>? = null
    private var poolDurations: Map<String, Float>? = null
    private var availablePool = listOf("wait")
    private var mouth = 0f
    private var lastFrame = Frame("wait", 0f, null, 0f, 1f, Shot.FULL, 0f, 0f)

    fun advance(snapshot: GameSnapshot?, speaking: Boolean, viseme: Int, deltaSeconds: Float,
                durations: Map<String, Float>): Frame {
        if (snapshot?.paused == true) return lastFrame
        val dt = deltaSeconds.coerceIn(0f, .1f)
        val phase = snapshot?.phase
        // Snapshots are immutable. Avoid formatting event strings on every render frame.
        if (snapshot !== lastSnapshot) {
            lastSnapshot = snapshot
            cachedKey = snapshot?.let { listOf(it.phase, it.competition, it.question?.id, it.questionIndex,
                it.hostLine, it.bettingOpen, it.winnerId, it.players.size).joinToString("|") } ?: "menu"
            hasLaugh = snapshot?.hostLine?.lowercase()?.let { "хаха" in it || "haha" in it || "חחח" in it } == true
        }
        val key = cachedKey
        if (key != event) {
            greeting = phase == Phase.LOBBY && (lastPhase != Phase.LOBBY || snapshot!!.players.size > playerCount)
            event = key; eventAge = 0f; speechSeen = false; afterSpeech = 0f
            lastPhase = phase; playerCount = snapshot?.players?.size ?: 0
            if (greeting) switchTo("hi")
        }
        eventAge += dt; clock += dt; time += dt; previousTime += dt; blendTime += dt
        if (speaking) speechSeen = true
        afterSpeech = if (speechSeen && !speaking) afterSpeech + dt else 0f
        val celebrating = phase == Phase.GAME_OVER && !speaking && (speechSeen || eventAge > 3f)
        val sourcePool = when {
            speaking && phase == Phase.QUESTION -> TALK_BASE
            speaking && snapshot?.hostMood == HostMood.SAD && phase == Phase.REVEAL -> TALK_SAD
            speaking && snapshot?.hostMood == HostMood.HYPE && (phase == Phase.INTRO || phase == Phase.GAME_OVER) -> TALK_HYPE
            speaking && (snapshot?.hostMood == HostMood.HAPPY || snapshot?.hostMood == HostMood.HYPE) -> TALK_HAPPY
            speaking -> TALK
            phase == null || phase == Phase.LOBBY -> IDLE_LOBBY
            else -> IDLE
        }
        if (poolSource !== sourcePool || poolDurations !== durations) {
            poolSource = sourcePool; poolDurations = durations
            availablePool = sourcePool.filter { it in durations }.ifEmpty { listOf("wait") }
        }
        val pool = availablePool
        fun pooled() = if (clip in pool) clip else nextFrom(pool)
        val desired = when {
            phase == Phase.TITLE -> "wait"
            greeting && eventAge < (durations["hi"] ?: 1.6f) -> "hi"
            phase == Phase.BET && snapshot?.bettingOpen == true -> "count"
            celebrating && hasLaugh && (if (speechSeen) afterSpeech else eventAge - 3f) < 1.6f -> "laughing"
            celebrating -> "dance"
            else -> pooled()
        }
        if (desired != clip) switchTo(desired)
        val duration = (durations[clip] ?: 3f).coerceAtLeast(.1f)
        if (time >= duration - FADE && clip != "hi" && clip != "count") {
            if (clip in pool && phase != Phase.TITLE && !celebrating) switchTo(nextFrom(pool))
            else switchTo(clip)
        }
        if (clip == "hi" || clip == "count") time = time.coerceAtMost(duration)
        val wantedMouth = if (!speaking) 0f else when (viseme) {
            2 -> 1f; 3 -> .8f; 6 -> .65f; 1 -> .38f; 5 -> .08f; else -> 0f
        }
        mouth += (wantedMouth - mouth) * (1f - exp(-dt * 55f))
        val shot = when (phase) {
            Phase.INTRO -> if (snapshot?.competition == null) Shot.CLOSE else Shot.MEDIUM
            Phase.GAME_OVER -> if (celebrating) Shot.FULL else Shot.MEDIUM
            Phase.BET -> if (snapshot?.bettingOpen == true) Shot.FULL else Shot.MEDIUM
            else -> Shot.FULL
        }
        val blink = (1f - abs((clock % 4.8f) - 3.35f) / .12f).coerceIn(0f, 1f)
        val alpha = (blendTime / FADE).coerceIn(0f, 1f).let { it * it * (3 - 2 * it) }
        return Frame(clip, time, previous, previousTime, alpha, shot, mouth, blink).also { lastFrame = it }
    }
    private fun switchTo(next: String) {
        previous = clip; previousTime = time; clip = next; time = 0f; blendTime = 0f
    }
    private fun nextFrom(pool: List<String>): String {
        if (pool.size == 1) return pool.first()
        val bag = bags.getOrPut(pool) { mutableListOf() }
        if (bag.isEmpty()) bag.addAll(pool.shuffled(random))
        var index = bag.indexOfFirst { it != clip }
        if (index < 0) {
            bag.clear(); bag.addAll(pool.shuffled(random))
            index = bag.indexOfFirst { it != clip }
        }
        return bag.removeAt(index)
    }
    companion object {
        const val FADE = .38f
        val TALK_BASE = listOf("talking", "talking2", "talking3", "arguing")
        val TALK = TALK_BASE + listOf("acknowledging", "head_nod_yes", "lengthy_head_nod")
        val TALK_HAPPY = TALK + listOf("happy_hand_gesture", "hard_head_nod")
        val TALK_HYPE = TALK_HAPPY + "being_cocky"
        val TALK_SAD = TALK_BASE + listOf("shaking_head_no", "thoughtful_head_shake", "relieved_sigh", "annoyed_head_shake")
        val IDLE = listOf("wait", "weight_shift")
        val IDLE_LOBBY = IDLE + listOf("look_away_gesture", "relieved_sigh")
    }
}
