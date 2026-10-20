package com.drquizman.tv

import android.app.Application
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.drquizman.core.model.CompetitionType
import com.drquizman.core.model.GameSnapshot
import com.drquizman.core.model.Phase

/** Random bed playlist, tick on questions, crowd on reveal/win, bang on timeout, fanfare on win. */
class ShowSounds(private val app: Application) {
    private val bedTracks = intArrayOf(
        R.raw.bg_music,
        R.raw.bg_music_2,
        R.raw.bg_music_3,
        R.raw.bg_music_4,
    )
    private var lastBed = -1
    private var bg: MediaPlayer? = null
    private var tick: MediaPlayer? = null
    private var oneShot: MediaPlayer? = null
    private var crowd: MediaPlayer? = null
    private var bedWanted = true
    private var speaking = false
    private var inQuestion = false
    private var inTitle = false
    private var appPaused = false

    private val gameAttrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private val sfxAttrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    fun startBed() {
        bedWanted = true
        ensureBed()
    }

    fun setSpeaking(on: Boolean) {
        speaking = on
        applyBedVolume()
        if (!on && inQuestion && !appPaused) {
            runCatching { if (tick != null && tick?.isPlaying == false) tick?.start() }
        }
    }

    fun onAppPause() {
        appPaused = true
        runCatching { bg?.pause() }
        runCatching { tick?.pause() }
        runCatching { oneShot?.pause() }
        runCatching { crowd?.pause() }
    }

    fun onAppResume() {
        appPaused = false
        ensureBed()
        runCatching { if (tick != null && tick?.isPlaying == false) tick?.start() }
        runCatching { if (oneShot != null && oneShot?.isPlaying == false) oneShot?.start() }
        runCatching { if (crowd != null && crowd?.isPlaying == false) crowd?.start() }
    }

    fun onSnapshot(prev: GameSnapshot?, snap: GameSnapshot) {
        val enteredQuestion = snap.phase == Phase.QUESTION && prev?.phase != Phase.QUESTION
        val leftQuestion = prev?.phase == Phase.QUESTION && snap.phase != Phase.QUESTION
        val betOpened = snap.phase == Phase.BET && snap.bettingOpen && prev?.bettingOpen != true
        val exploded = snap.bombExploded && prev?.bombExploded != true
        val passed = snap.bombHolderId != null &&
            snap.bombHolderId != prev?.bombHolderId &&
            !snap.bombExploded
        val sank = snap.competition == CompetitionType.FINAL &&
            snap.players.any { p ->
                p.eliminated && prev?.players?.find { it.id == p.id }?.eliminated != true
            }
        if (enteredQuestion && snap.competition != CompetitionType.FINAL) startTick(snap.timeLimitMs)
        if (betOpened) startTick(snap.timeLimitMs)
        if (leftQuestion) {
            stopTick()
            // Reveal overwrites timeLeftMs with the speech hold, so timeout is
            // the previous question snapshot still sitting at ~0.
            val timedOut = (prev?.timeLeftMs ?: Long.MAX_VALUE) <= 250L
            when {
                exploded -> play(R.raw.sfx_bomb_explode, 1f)
                snap.phase == Phase.REVEAL && timedOut -> play(R.raw.sfx_timeout, 1f)
            }
        } else if (exploded) {
            play(R.raw.sfx_bomb_explode, 1f)
        }
        if (passed) play(R.raw.sfx_bomb_pass, 1f)
        if (sank) play(R.raw.sfx_bang, 0.9f)
        if (snap.paused && prev?.paused != true) runCatching { tick?.pause() }
        if (!snap.paused && prev?.paused == true &&
            (snap.phase == Phase.QUESTION || (snap.phase == Phase.BET && snap.bettingOpen))
        ) {
            runCatching { if (tick?.isPlaying == false) tick?.start() }
        }
        if ((snap.phase == Phase.QUESTION || (snap.phase == Phase.BET && snap.bettingOpen)) &&
            !snap.paused && tick == null && !enteredQuestion && !betOpened
        ) {
            startTick(snap.timeLeftMs)
        }
        if (snap.phase == Phase.TITLE && prev?.phase != Phase.TITLE) {
            play(titleSting(snap.competition), 1f)
        }
        if (snap.phase == Phase.REVEAL && prev?.phase != Phase.REVEAL) {
            playCrowdForReveal(snap)
        }
        if (snap.phase == Phase.GAME_OVER && prev?.phase != Phase.GAME_OVER) {
            stopTick()
            play(R.raw.sfx_fanfare, 0.8f)
            playCrowd(R.raw.sfx_crowd_cheer, 0.58f)
        }
        inQuestion = snap.phase == Phase.QUESTION || (snap.phase == Phase.BET && snap.bettingOpen)
        inTitle = snap.phase == Phase.TITLE
        bedWanted = snap.phase != Phase.GAME_OVER
        ensureBed()
    }

    fun stopFx() {
        stopTick()
        stopCrowd()
        oneShot?.setOnCompletionListener(null)
        runCatching { oneShot?.stop() }
        oneShot?.release()
        oneShot = null
    }

    fun stopAll() {
        stopFx()
        bedWanted = false
        bg?.setOnCompletionListener(null)
        runCatching { bg?.stop() }
        bg?.release()
        bg = null
    }

    private fun ensureBed() {
        if (!bedWanted || appPaused) {
            runCatching { if (bg?.isPlaying == true) bg?.pause() }
            return
        }
        if (bg == null) {
            bg = player(nextBedTrack(), gameAttrs)?.apply {
                isLooping = false
                setOnCompletionListener {
                    it.setOnCompletionListener(null)
                    it.release()
                    if (bg === it) bg = null
                    if (bedWanted && !appPaused) ensureBed()
                }
            }
        }
        applyBedVolume()
        runCatching { if (bg?.isPlaying == false) bg?.start() }
    }

    private fun nextBedTrack(): Int {
        val pool = bedTracks.filter { it != lastBed }
        val pick = pool.random()
        lastBed = pick
        return pick
    }

    private fun applyBedVolume() {
        val v = when {
            speaking && inQuestion -> 0.06f
            inTitle -> 0.05f
            inQuestion -> 0.10f
            speaking -> 0.08f
            else -> 0.22f
        }
        bg?.setVolume(v, v)
    }

    private fun titleSting(type: CompetitionType?): Int = when (type) {
        CompetitionType.BOMB -> R.raw.sfx_bomb_pass
        CompetitionType.FINAL -> R.raw.sfx_bang
        else -> R.raw.sfx_title_sting
    }

    private fun startTick(limitMs: Long) {
        stopTick()
        if (limitMs <= 0L) return
        val player = player(R.raw.sfx_tick, sfxAttrs) ?: return
        player.setVolume(0.78f, 0.78f)
        val dur = player.duration.toLong()
        // The file is a ~30s countdown. We start near the end so urgency matches
        // the clock. MediaPlayer's MP3 duration/seek is sloppy and would otherwise
        // run out of ticks while timeLeftMs is still going; extra headroom plus a
        // tail loop keep the sound alive until stopTick().
        val slackMs = 3_000L
        if (dur > limitMs + slackMs) {
            player.seekTo((dur - limitMs - slackMs).toInt())
            player.setOnCompletionListener { ended ->
                if (!inQuestion || appPaused || tick !== ended) return@setOnCompletionListener
                val tail = 4_000L.coerceAtMost(dur.coerceAtLeast(1L)).toInt()
                ended.seekTo((dur - tail).toInt().coerceAtLeast(0))
                runCatching { ended.start() }
            }
        } else {
            player.isLooping = true
        }
        player.start()
        tick = player
    }

    private fun stopTick() {
        tick?.setOnCompletionListener(null)
        runCatching { tick?.stop() }
        tick?.release()
        tick = null
    }

    private fun playCrowdForReveal(snap: GameSnapshot) {
        if (snap.competition == CompetitionType.BOMB || snap.bombExploded) return
        val active = snap.players.filter { !it.eliminated }
        if (active.isEmpty()) return
        val hits = active.count { it.lastCorrect == true }
        when {
            hits == active.size && active.size >= 2 -> playCrowd(R.raw.sfx_crowd_applause, 0.48f)
            hits == 0 -> playCrowd(R.raw.sfx_crowd_boo, 0.42f)
        }
    }

    private fun playCrowd(res: Int, volume: Float) {
        stopCrowd()
        val player = player(res, sfxAttrs) ?: return
        player.setVolume(volume, volume)
        player.setOnCompletionListener {
            it.release()
            if (crowd === it) crowd = null
        }
        player.start()
        crowd = player
    }

    private fun stopCrowd() {
        crowd?.setOnCompletionListener(null)
        runCatching { crowd?.stop() }
        crowd?.release()
        crowd = null
    }

    private fun play(res: Int, volume: Float) {
        oneShot?.setOnCompletionListener(null)
        runCatching { oneShot?.stop() }
        oneShot?.release()
        val player = player(res, sfxAttrs) ?: return
        player.setVolume(volume, volume)
        player.setOnCompletionListener {
            it.release()
            if (oneShot === it) oneShot = null
        }
        player.start()
        oneShot = player
    }

    private fun player(res: Int, attrs: AudioAttributes): MediaPlayer? = runCatching {
        MediaPlayer().apply {
            setAudioAttributes(attrs)
            app.resources.openRawResourceFd(res).use { fd ->
                setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                prepare()
            }
        }
    }.getOrNull()
}
