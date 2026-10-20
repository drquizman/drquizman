package com.drquizman.tv.speech

import com.drquizman.core.i18n.LocalizedText

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.drquizman.core.model.HostMood
import com.drquizman.core.i18n.LocaleManager
import java.util.Locale

/** Мужской TTS + минимальный липсинк: открытый/закрытый рот по гласным и слогам. */
class HostSpeech(
    private val app: Application,
    private val onLip: (speaking: Boolean, viseme: Int) -> Unit,
    private val onComplete: () -> Unit = {},
    private val onOptionsChanged: () -> Unit = {},
    private val onStatusChanged: (Status) -> Unit = {},
) {
    enum class Status { LOADING, READY, UNAVAILABLE }
    data class EngineOption(val packageName: String, val label: String)
    data class VoiceOption(
        val name: String,
        val label: String,
        val isNetwork: Boolean,
        val isDefault: Boolean,
        val isRecommended: Boolean,
    )

    private val prefs = app.getSharedPreferences("speech", 0)
    private var tts: TextToSpeech? = null
    private var ready = false
    private var initializing = false
    private var generation = 0
    private var pendingSpeech: Triple<String, HostMood, String>? = null
    private val initTimeout = Runnable {
        if (initializing) failInitialization()
    }

    private var cachedVoices: Set<Voice> = emptySet()

    private fun queryVoices(engine: TextToSpeech): Set<Voice> =
        runCatching { engine.voices.orEmpty() }.getOrElse {
            Log.e("HostSpeech", "Unable to query voices", it)
            emptySet()
        }

    private fun availableVoices(engine: TextToSpeech): Set<Voice> =
        cachedVoices.ifEmpty { queryVoices(engine).also { cachedVoices = it } }
    private var maleVoice = false
    private var defaultVoiceName: String? = null
    private var currentText = ""
    private var activeUtterance: String? = null
    private var speaking = false
    private var viseme = 0
    private var rangeDriven = false
    private var cycleI = 0
    private var rangeVisemes = intArrayOf()
    private var rangeI = 0
    private val main = Handler(Looper.getMainLooper())

    private val pulse = object : Runnable {
        override fun run() {
            if (!speaking) return
            if (!rangeDriven) {
                cycleI = (cycleI + 1) % talkCycle.size
                setViseme(talkCycle[cycleI])
                val delay = if (viseme in openVisemes) 95L else 55L
                main.postDelayed(this, delay)
            } else {
                if (rangeI < rangeVisemes.size) {
                    setViseme(rangeVisemes[rangeI++])
                    main.postDelayed(this, 70L)
                } else {
                    // A range ending is not speech ending: TTS may report a whole
                    // phrase once, or omit later ranges. Keep the fallback pulse
                    // alive until onDone/onStop/onError cancels it.
                    rangeDriven = false
                    setViseme(5)
                    main.postDelayed(this, 55L)
                }
            }
        }
    }

    fun start() {
        if (tts != null) return
        onStatusChanged(Status.LOADING)
        val installed = engines().map { it.packageName }
        val preferred = prefs.getString(KEY_ENGINE, null)?.takeIf { it in installed }
            ?: installed.firstOrNull { it == "com.google.android.tts" }
        createEngine(preferred)
    }

    fun engines(): List<EngineOption> = runCatching {
        val intent = Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE)
        app.packageManager.queryIntentServices(intent, 0)
            .mapNotNull { resolve ->
                val service = resolve.serviceInfo ?: return@mapNotNull null
                EngineOption(service.packageName, service.loadLabel(app.packageManager).toString())
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }.getOrDefault(emptyList())

    fun voices(): List<VoiceOption> {
        val engine = tts ?: return emptyList()
        if (!ready) return emptyList()
        val all = availableVoices(engine)
        val defaultName = defaultVoiceName
        val recommendedName = pickMaleVoice(all)?.name
        val googleEngine = (prefs.getString(KEY_ENGINE, null) ?: "").contains("google", ignoreCase = true)
        var maleNumber = 0
        var femaleNumber = 0
        var otherNumber = 0
        return all
            .filter { matchesLanguage(it) }
            .sortedWith(compareBy<Voice>({ !looksMale(it.name) }, { it.isNetworkConnectionRequired }, { it.name }))
            .map {
                val male = looksMale(it.name)
                val female = looksFemale(it.name)
                val displayName = if (googleEngine) {
                    when {
                        male -> LocalizedText.get("HostSpeech.001", (++maleNumber))
                        female -> LocalizedText.get("HostSpeech.002", (++femaleNumber))
                        else -> LocalizedText.get("HostSpeech.003", (++otherNumber))
                    }
                } else {
                    prettyVoiceLabel(it.name)
                }
                VoiceOption(
                    name = it.name,
                    label = displayName,
                    isNetwork = it.isNetworkConnectionRequired,
                    isDefault = it.name == defaultName,
                    isRecommended = it.name == recommendedName,
                )
            }
    }

    fun selectedEngine(): String? = prefs.getString(KEY_ENGINE, null)
    fun selectedVoice(): String? = prefs.getString(KEY_VOICE, null)

    fun selectEngine(packageName: String) {
        if (packageName == selectedEngine() && (ready || initializing)) return
        prefs.edit().putString(KEY_ENGINE, packageName).remove(KEY_VOICE).apply()
        shutdown()
        createEngine(packageName)
    }

    fun selectVoice(voiceName: String) {
        val engine = tts ?: return
        val voice = availableVoices(engine).firstOrNull { it.name == voiceName && matchesLanguage(it) } ?: return
        runCatching { tts?.voice = voice }.onFailure { return }
        maleVoice = looksMale(voice.name)
        prefs.edit().putString(KEY_VOICE, voice.name).apply()
        onOptionsChanged()
    }

    fun setLanguage(locale: Locale) {
        LocaleManager.set(locale)
        prefs.edit().remove(KEY_VOICE).apply()
        stop()
        onStatusChanged(Status.LOADING)
        if (tts == null) start()
        else if (ready) runCatching { configure() }.onFailure {
            Log.e("HostSpeech", "Unable to change language", it)
            failInitialization()
        }
    }

    fun previewVoice(voiceName: String) {
        val engine = tts ?: return
        if (!ready) return
        val voice = availableVoices(engine).firstOrNull { it.name == voiceName && matchesLanguage(it) } ?: return
        runCatching {
            engine.voice = voice
            engine.setSpeechRate(0.95f)
            engine.setPitch(if (looksMale(voice.name)) 0.98f else 0.82f)
            val preview = PREVIEW_TEXT
            currentText = preview
            activeUtterance = PREVIEW_ID
            engine.speak(preview, TextToSpeech.QUEUE_FLUSH, Bundle(), PREVIEW_ID)
        }
    }

    fun speak(text: String, mood: HostMood, utteranceId: String) {
        if (initializing) {
            pendingSpeech = Triple(text, mood, utteranceId)
            return
        }
        val engine = tts
        activeUtterance = utteranceId
        if (engine == null || !ready || text.isBlank() || availableVoices(engine).none { matchesLanguage(it) }) {
            main.post { finishSpeech(completed = true, utteranceId) }
            return
        }
        prefs.getString(KEY_VOICE, null)?.let { saved ->
            runCatching { availableVoices(engine).firstOrNull { it.name == saved && matchesLanguage(it) }?.let { engine.voice = it; maleVoice = looksMale(it.name) } }
        }
        currentText = text
        activeUtterance = utteranceId
        rangeDriven = false
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f)
        }
        val result = runCatching {
            applyMood(mood)
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        }.getOrElse {
            Log.e("HostSpeech", "Unable to speak", it)
            TextToSpeech.ERROR
        }
        if (result == TextToSpeech.ERROR) {
            main.post { finishSpeech(completed = true, utteranceId) }
        }
    }

    fun stop() {
        pendingSpeech = null
        runCatching { tts?.stop() }
        finishSpeech(completed = false, activeUtterance)
        activeUtterance = null
    }

    fun shutdown() {
        generation++
        initializing = false
        pendingSpeech = null
        main.removeCallbacks(initTimeout)
        ready = false
        main.removeCallbacks(pulse)
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
        cachedVoices = emptySet()
        finishSpeech(completed = false, activeUtterance)
        activeUtterance = null
    }

    private fun createEngine(preferred: String?) {
        val ticket = ++generation
        initializing = true
        ready = false
        main.removeCallbacks(initTimeout)
        main.postDelayed(initTimeout, 15000L)
        val listener = TextToSpeech.OnInitListener { status ->
            main.post {
            if (ticket != generation) return@post
            if (status == TextToSpeech.SUCCESS) {
                val actual = preferred ?: runCatching { tts?.defaultEngine }.getOrNull()
                prefs.edit().putString(KEY_ENGINE, actual).apply()
                runCatching { configure() }.onFailure {
                    Log.e("HostSpeech", "Unable to configure TTS", it)
                    failInitialization()
                }
            } else if (preferred != null) {
                runCatching { tts?.shutdown() }
                tts = null
                prefs.edit().remove(KEY_ENGINE).apply()
                createEngine(null)
            } else {
                failInitialization()
            }
            }
        }
        try {
        tts = if (preferred != null) {
            TextToSpeech(app, listener, preferred)
        } else {
            TextToSpeech(app, listener)
        }
        } catch (e: Exception) {
            Log.e("HostSpeech", "Unable to create TTS", e)
            failInitialization()
        }
    }

    private fun failInitialization() {
        val pending = pendingSpeech
        shutdown()
        onStatusChanged(Status.UNAVAILABLE)
        onOptionsChanged()
        if (pending != null) onComplete()
    }

    private fun configure() {
        val engine = tts ?: return
        val ticket = generation
        engine.language = LocaleManager.get()
        defaultVoiceName = runCatching { engine.voice?.name }.getOrNull()
        cachedVoices = queryVoices(engine)
        val savedVoice = prefs.getString(KEY_VOICE, null)
        val voice = availableVoices(engine).firstOrNull { it.name == savedVoice && matchesLanguage(it) }
            ?: pickMaleVoice(cachedVoices)
        if (voice != null) {
            engine.voice = voice
            maleVoice = looksMale(voice.name)
            prefs.edit().putString(KEY_VOICE, voice.name).apply()
        } else {
            prefs.edit().remove(KEY_VOICE).apply()
            maleVoice = false
        }
        engine.setSpeechRate(0.95f)
        engine.setPitch(if (maleVoice) 0.98f else 0.78f)
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                main.post {
                    if (ticket != generation || utteranceId != activeUtterance) return@post
                    speaking = true
                    if (utteranceId != PREVIEW_ID) onStatusChanged(Status.READY)
                    rangeDriven = false
                    cycleI = 0
                    setViseme(1)
                    main.removeCallbacks(pulse)
                    main.postDelayed(pulse, 45L)
                }
            }

            override fun onDone(utteranceId: String?) {
                main.post { if (ticket == generation) finishSpeech(completed = utteranceId != PREVIEW_ID, utteranceId) }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                main.post { if (ticket == generation) finishSpeech(completed = utteranceId != PREVIEW_ID, utteranceId) }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                main.post {
                    if (ticket == generation) {
                        if (utteranceId != PREVIEW_ID) onStatusChanged(Status.UNAVAILABLE)
                        finishSpeech(completed = utteranceId != PREVIEW_ID, utteranceId)
                    }
                }
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                main.post { if (ticket == generation) finishSpeech(completed = !interrupted && utteranceId != PREVIEW_ID, utteranceId) }
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                val text = currentText
                if (text.isEmpty()) return
                val a = start.coerceIn(0, text.length)
                val b = end.coerceIn(0, text.length)
                if (a >= b) return
                val chunk = text.substring(a, b)
                main.post {
                    if (ticket != generation || utteranceId != activeUtterance || !speaking) return@post
                    rangeDriven = true
                    // Word-range callbacks are not phoneme timestamps. Articulate
                    // within the word instead of holding one vowel until the next word.
                    rangeVisemes = chunk.filter { it.isLetter() }.map { visemeOf(it.toString()) }
                        .fold(mutableListOf<Int>()) { list, value ->
                            if (list.lastOrNull() != value) list.add(value)
                            list
                        }.toIntArray()
                    rangeI = 0
                    main.removeCallbacks(pulse)
                    pulse.run()
                }
            }
        })
        ready = true
        initializing = false
        main.removeCallbacks(initTimeout)
        onStatusChanged(
            when {
                voice == null -> Status.UNAVAILABLE
                voice.isNetworkConnectionRequired -> Status.LOADING
                else -> Status.READY
            },
        )
        onOptionsChanged()
        val pending = pendingSpeech
        pendingSpeech = null
        pending?.let { speak(it.first, it.second, it.third) }
    }

    private fun applyMood(mood: HostMood) {
        val engine = tts ?: return
        val (rate, pitch) = when (mood) {
            HostMood.HYPE -> 1.02f to 1.06f
            HostMood.HAPPY -> 0.98f to 1.03f
            HostMood.SAD -> 0.88f to 0.9f
            HostMood.IDLE -> 0.92f to 0.96f
            HostMood.TALK -> 0.95f to 1.0f
        }
        engine.setSpeechRate(rate)
        engine.setPitch(if (maleVoice) pitch else (pitch - 0.18f).coerceAtLeast(0.6f))
    }

    private fun setViseme(id: Int) {
        viseme = id
        onLip(speaking, viseme)
    }

    private fun finishSpeech(completed: Boolean, utteranceId: String?) {
        if (completed && (utteranceId == null || utteranceId != activeUtterance)) return
        if (utteranceId != null && utteranceId != activeUtterance) return
        activeUtterance = null
        main.removeCallbacks(pulse)
        speaking = false
        rangeDriven = false
        viseme = 0
        onLip(false, 0)
        if (completed) onComplete()
    }

    private fun matchesLanguage(voice: Voice): Boolean =
        LocaleManager.normalize(voice.locale.language) == LocaleManager.languageTag()

    private fun pickMaleVoice(voices: Set<Voice>): Voice? {
        val ru = voices.filter { matchesLanguage(it) }
        if (ru.isEmpty()) return null
        val local = ru.filter { !it.isNetworkConnectionRequired }.ifEmpty { ru }
        return ru.filter { looksMale(it.name) }
            .sortedWith(compareBy<Voice>({ it.isNetworkConnectionRequired }, { -it.quality }, { it.name })).firstOrNull()
            ?: local.filterNot { looksFemale(it.name) }.maxByOrNull { it.quality }
            ?: local.maxByOrNull { it.quality }
    }

    private fun prettyVoiceLabel(name: String): String {
        val lower = name.lowercase(Locale.ROOT)
        val gender = if (looksMale(name)) LocalizedText.get("HostSpeech.004") else if (looksFemale(name)) LocalizedText.get("HostSpeech.005") else LocalizedText.get("HostSpeech.006")
        val provider = when {
            lower.contains("supertonic") -> "Supertonic"
            lower.contains("piper") || lower.contains("medium") -> "Piper"
            lower.contains("rhvoice") -> "RHVoice"
            lower.contains("network") -> LocalizedText.get("HostSpeech.007")
            else -> ""
        }
        val person = when {
            lower.contains("denis") -> LocalizedText.get("HostSpeech.008")
            lower.contains("dmitri") -> LocalizedText.get("HostSpeech.009")
            lower.contains("ruslan") -> LocalizedText.get("HostSpeech.010")
            lower.contains("aleksandr") || lower.contains("alexander") -> LocalizedText.get("HostSpeech.011")
            lower.contains("vitaliy") -> LocalizedText.get("HostSpeech.012")
            lower.contains("timofey") -> LocalizedText.get("HostSpeech.013")
            lower.contains("evgeniy") -> LocalizedText.get("HostSpeech.014")
            Regex("supertonic-[mf][1-5]", RegexOption.IGNORE_CASE).containsMatchIn(name) ->
                Regex("supertonic-([mf][1-5])", RegexOption.IGNORE_CASE).find(name)?.groupValues?.get(1)?.uppercase(Locale.ROOT)
            else -> null
        }
        if (person != null) return listOfNotNull(person, provider.takeIf { it.isNotBlank() }).joinToString(" · ")

        val googleCode = Regex("(?:^|-x-)([a-z]{3})(?:-(local|network))?", RegexOption.IGNORE_CASE)
            .find(name)
        if (googleCode != null) {
            val code = googleCode.groupValues[1].uppercase(Locale.ROOT)
            val kind = googleCode.groupValues.getOrNull(2)?.lowercase(Locale.ROOT)
                ?.replaceFirstChar { it.uppercase(Locale.ROOT) }
            return listOfNotNull(gender, code, kind).joinToString(" · ")
        }
        return listOf(gender, name).joinToString(" · ")
    }

    companion object {
        private const val PREVIEW_ID = "speech-preview"
        private val PREVIEW_TEXT get() = LocalizedText.get("HostSpeech.015")
        private const val KEY_ENGINE = "engine"
        private const val KEY_VOICE = "voice"
        private val vowelsA = "аяэ".toSet()
        private val vowelsO = "оёую".toSet()
        private val vowelsE = "иые".toSet()
        private val talkCycle = intArrayOf(5, 1, 2, 1, 6, 1)
        private val openVisemes = setOf(1, 2, 3, 6)
        private val maleHints = listOf(
            "male", "man", "rud", "aleksandr", "alexander", "dmitry", "dmitriy",
            "yuri", "filipp", "ermil", "wavenet-b", "wavenet-d", "standard-b",
            "standard-d", "neural2-b", "neural2-d", "#male",
        )
        private val femaleHints = listOf(
            "female", "woman", "dfc", "dfd", "elena", "irina", "katherine",
            "wavenet-a", "wavenet-c", "wavenet-e", "standard-a", "standard-c",
            "#female",
        )

        private fun looksMale(name: String): Boolean =
            !looksFemale(name) && (maleHints.any { name.contains(it, ignoreCase = true) } ||
                Regex("Supertonic-M[1-5]-", RegexOption.IGNORE_CASE).containsMatchIn(name))

        private fun looksFemale(name: String): Boolean =
            femaleHints.any { name.contains(it, ignoreCase = true) } ||
                Regex("Supertonic-F[1-5]-", RegexOption.IGNORE_CASE).containsMatchIn(name)
        fun visemeOf(chunk: String): Int {
            val letters = chunk.lowercase().filter { it.isLetter() }
            if (letters.isEmpty()) return if (chunk.any { it in "!?" }) 2 else 5
            if (letters.any { it in vowelsA }) return 2
            if (letters.any { it in vowelsO }) return 3
            if (letters.any { it in vowelsE }) return 6
            return 1
        }
    }
}


