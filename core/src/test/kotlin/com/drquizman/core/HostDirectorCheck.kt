package com.drquizman.core

import com.drquizman.core.host.HostDirector
import com.drquizman.core.model.*
import kotlin.random.Random

fun main() {
    val durations = mapOf("wait" to 2.93f, "hi" to 1.6f, "count" to 5.6f,
        "dance" to 6.33f, "talking" to 4.43f, "talking2" to 3.87f,
        "talking3" to 10.27f, "arguing" to 16.9f)
    fun snap(phase: Phase) = GameSnapshot(pin = "1", phase = phase, players = emptyList(),
        hostLine = "Existing speech", hostMood = HostMood.TALK)
    val director = HostDirector(Random(42))
    val question = snap(Phase.QUESTION).copy(competition = CompetitionType.FIRST)
    val sequence = mutableListOf<String>()
    repeat(1200) { tick ->
        // Live timer snapshots must not restart animation or camera.
        val f = director.advance(question.copy(timeLeftMs = 120_000L - tick * 100L), true, 2, .1f, durations)
        check(f.shot == HostDirector.Shot.FULL)
        if (sequence.lastOrNull() != f.clip) sequence += f.clip
        check(f.blend in 0f..1f && f.mouth in 0f..1f)
    }
    check(sequence.toSet() == HostDirector.TALK_BASE.toSet())
    check(sequence.size > 8)
    val before = director.advance(question, true, 2, .1f, durations)
    repeat(10) { check(director.advance(question.copy(paused = true, hostLine = "Pause"), false, 0, .1f, durations) == before) }
    val idle = director.advance(question, false, 0, .1f, durations)
    check(idle.clip == "wait" && idle.mouth < before.mouth)
    val bets = director.advance(snap(Phase.BET).copy(bettingOpen = true), false, 0, .1f, durations)
    check(bets.clip == "count" && bets.shot == HostDirector.Shot.FULL)
    val winner = snap(Phase.GAME_OVER)
    check(director.advance(winner, true, 2, .1f, durations).clip in HostDirector.TALK)
    check(director.advance(winner, false, 0, .1f, durations).clip == "dance")
    val lobby = director.advance(snap(Phase.LOBBY), true, 2, .1f, durations)
    check(lobby.clip == "hi")
    repeat(20) { director.advance(snap(Phase.LOBBY), true, 2, .1f, durations) }
    check(director.advance(snap(Phase.LOBBY), true, 2, .1f, durations).clip in HostDirector.TALK)
    check(director.advance(snap(Phase.TITLE), true, 2, .1f, durations).clip == "wait")
    check(question.phase == Phase.QUESTION && question.timeLeftMs == 0L)
    val allDurations = durations + (HostDirector.TALK_HYPE + HostDirector.TALK_SAD + HostDirector.IDLE_LOBBY)
        .associateWith { durations[it] ?: 2f }
    fun sample(s: GameSnapshot, speech: Boolean, allowed: List<String>): Set<String> {
        val d = HostDirector(Random(7))
        val seen = mutableSetOf<String>()
        var previous: HostDirector.Frame? = null
        repeat(2400) {
            val f = d.advance(s, speech, 2, .1f, allDurations)
            check(f.clip in allowed)
            if (previous != null && f.seconds < previous!!.seconds) check(f.clip != previous!!.clip)
            seen += f.clip
            previous = f
        }
        return seen
    }
    check(sample(question, false, HostDirector.IDLE) == HostDirector.IDLE.toSet())
    check(sample(question, true, HostDirector.TALK_BASE) == HostDirector.TALK_BASE.toSet())
    check(sample(snap(Phase.INTRO), true, HostDirector.TALK) == HostDirector.TALK.toSet())
    check(sample(snap(Phase.INTRO).copy(hostMood = HostMood.HYPE), true, HostDirector.TALK_HYPE) == HostDirector.TALK_HYPE.toSet())
    check(sample(snap(Phase.REVEAL).copy(hostMood = HostMood.SAD), true, HostDirector.TALK_SAD) == HostDirector.TALK_SAD.toSet())
    println("HostDirector: clips, timer stability, pause, mouth, bets, winner, greeting and title passed")
}
