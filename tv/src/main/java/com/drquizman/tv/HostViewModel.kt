package com.drquizman.tv

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drquizman.core.engine.GameEngine
import com.drquizman.core.engine.ShowPlan
import com.drquizman.core.model.GameSnapshot
import com.drquizman.core.model.Phase
import com.drquizman.core.questions.QuestionBank
import com.drquizman.core.i18n.LocaleManager
import com.drquizman.core.i18n.LocalizedText
import com.drquizman.tv.net.HostServer
import com.drquizman.tv.net.localIpv4
import com.drquizman.tv.net.wifiSsid
import com.drquizman.tv.speech.HostSpeech
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import com.drquizman.core.questions.PackSelection
import com.drquizman.tv.questions.PackRepository
import com.drquizman.tv.questions.PackService
import com.drquizman.tv.questions.PacksState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class HostViewModel(app: android.app.Application) : AndroidViewModel(app) {
    enum class Screen { MENU, SHOW }

    private val _screen = MutableStateFlow(Screen.MENU)
    val screen: StateFlow<Screen> = _screen
    private val _snapshot = MutableStateFlow<GameSnapshot?>(null)
    val snapshot: StateFlow<GameSnapshot?> = _snapshot
    private val _help = MutableStateFlow(false)
    val help: StateFlow<Boolean> = _help
    private val _starting = MutableStateFlow(false)
    val starting: StateFlow<Boolean> = _starting
    private val _speechEngines = MutableStateFlow<List<HostSpeech.EngineOption>>(emptyList())
    val speechEngines: StateFlow<List<HostSpeech.EngineOption>> = _speechEngines
    private val _speechVoices = MutableStateFlow<List<HostSpeech.VoiceOption>>(emptyList())
    val speechVoices: StateFlow<List<HostSpeech.VoiceOption>> = _speechVoices
    private val _selectedSpeechEngine = MutableStateFlow<String?>(null)
    val selectedSpeechEngine: StateFlow<String?> = _selectedSpeechEngine
    private val _selectedSpeechVoice = MutableStateFlow<String?>(null)
    val selectedSpeechVoice: StateFlow<String?> = _selectedSpeechVoice
    private val _speechStatus = MutableStateFlow(HostSpeech.Status.LOADING)
    val speechStatus: StateFlow<HostSpeech.Status> = _speechStatus
    private val localePrefs = app.getSharedPreferences("locale", 0)
    private val _language = MutableStateFlow(localePrefs.getString("language", null) ?: deviceGameLanguage())
    val language: StateFlow<String> = _language
    private val packRepository = PackRepository(app)
    private val packService = PackService(BuildConfig.PACK_SERVICE_URL)
    private val _packs = MutableStateFlow(PacksState())
    val packs: StateFlow<PacksState> = _packs
    private var packJob: Job? = null
    private var linkingPacks = false

    private var engine: GameEngine? = null
    private var server: HostServer? = null
    private var serverStopJob: Job? = null
    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking
    private val _viseme = MutableStateFlow(0)
    val viseme: StateFlow<Int> = _viseme
    private val speech = HostSpeech(
        app,
        onLip = { speaking, viseme ->
            _speaking.value = speaking
            _viseme.value = viseme
            sounds.setSpeaking(speaking)
        },
        onComplete = { engine?.onSpeechFinished() },
        onOptionsChanged = { refreshSpeechOptions() },
        onStatusChanged = { _speechStatus.value = it },
    )
    private var lastSpoken: String? = null
    private var lastRemoteKey: String? = null
    private val tones = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
    private val sounds = ShowSounds(app)

    init {
        LocaleManager.set(_language.value)
        _language.value = LocaleManager.languageTag()
        refreshSpeechOptions()
        speech.start()
        sounds.startBed()
        refreshPacks()
    }

    fun selectLanguage(language: String) {
        if (_starting.value || _packs.value.busy) return
        if (language !in LocaleManager.supportedLanguages()) return
        if (language == _language.value) return
        LocaleManager.set(language)
        localePrefs.edit().putString("language", language).apply()
        _language.value = language
        speech.setLanguage(Locale.forLanguageTag(language))
        refreshSpeechOptions()
        refreshPacks()
    }

    private suspend fun readPackState(): PacksState = withContext(Dispatchers.IO) { packRepository.state(_language.value) }

    private fun packAction(action: suspend () -> Unit) {
        if (_packs.value.busy || _starting.value) return
        _packs.value = _packs.value.copy(busy = true, messageKey = null)
        packJob = viewModelScope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _packs.value = _packs.value.copy(code = null, messageKey = "Packs.error") }
            finally { linkingPacks = false; _packs.value = _packs.value.copy(busy = false) }
        }
    }

    fun refreshPacks() = packAction {
        _packs.value = readPackState().copy(busy = true)
        if (packService.configured && packRepository.token.isNotEmpty()) fetchPurchasedPacks()
    }

    private suspend fun fetchPurchasedPacks() {
        val purchased = withContext(Dispatchers.IO) { packService.purchases(packRepository.token, _language.value) }
        val local = readPackState()
        val installed = local.installed.map { it.info.id }.toSet()
        _packs.value = local.copy(available = purchased.filter { it.id !in installed }, busy = true)
    }

    fun selectPack(id: String, selected: Boolean) = packAction {
        val available = _packs.value.available
        withContext(Dispatchers.IO) {
            if (id == "base") packRepository.selectBase(_language.value, selected)
            else if (_packs.value.installed.any { it.info.id == id }) packRepository.select(id, selected)
        }
        _packs.value = readPackState().copy(available = available, busy = true)
    }

    fun linkTelegram() {
        if (_packs.value.busy || _starting.value) return
        if (!packService.configured) {
            _packs.value = _packs.value.copy(messageKey = "Packs.notConfigured")
            return
        }
        linkingPacks = true
        packAction {
            val pairing = withContext(Dispatchers.IO) { packService.begin() }
            _packs.value = _packs.value.copy(code = pairing.code, codeExpiresAt = pairing.expiresAt)
            while (System.currentTimeMillis() < pairing.expiresAt && viewModelScope.isActive) {
                delay(3000)
                val token = withContext(Dispatchers.IO) { packService.poll(pairing) }
                if (token != null) {
                    withContext(Dispatchers.IO) { packRepository.token = token }
                    _packs.value = _packs.value.copy(linked = true, code = null)
                    fetchPurchasedPacks()
                    return@packAction
                }
            }
            _packs.value = _packs.value.copy(code = null, messageKey = "Packs.expired")
        }
    }

    fun closePacks() {
        if (linkingPacks) {
            packJob?.cancel()
            _packs.value = _packs.value.copy(code = null)
        }
    }

    fun downloadAllPacks() = packAction {
        val missing = _packs.value.available.toList()
        var failed = false
        for (pack in missing) {
            try {
                withContext(Dispatchers.IO) { packRepository.install(pack, packService.download(packRepository.token, pack)) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { failed = true }
            val local = readPackState()
            _packs.value = local.copy(busy = true, available = missing.filter { pending ->
                local.installed.none { it.info.id == pending.id }
            })
        }
        _packs.value = _packs.value.copy(messageKey = if (failed) "Packs.partial" else "Packs.downloaded")
    }

    fun refreshSpeechOptions() {
        _speechEngines.value = speech.engines()
        _speechVoices.value = speech.voices()
        _selectedSpeechEngine.value = speech.selectedEngine()
        _selectedSpeechVoice.value = speech.selectedVoice()
    }

    fun selectSpeechEngine(packageName: String) {
        _selectedSpeechEngine.value = packageName
        _speechVoices.value = emptyList()
        speech.selectEngine(packageName)
    }

    fun selectSpeechVoice(voiceName: String) {
        speech.selectVoice(voiceName)
        refreshSpeechOptions()
    }

    fun previewSpeechVoice(voiceName: String) {
        speech.previewVoice(voiceName)
    }

    fun showHelp(show: Boolean) { _help.value = show }

    fun startShow(plan: ShowPlan = ShowPlan.Standard) {
        if (_screen.value == Screen.SHOW || _starting.value || _packs.value.busy) return
        _starting.value = true
        viewModelScope.launch {
            var nextEngine: GameEngine? = null
            var nextServer: HostServer? = null
            try {
                val html = withContext(Dispatchers.IO) {
                    getApplication<android.app.Application>().assets.open("remote/index.html")
                        .bufferedReader(Charsets.UTF_8).use { it.readText() }
                        .replace("__REMOTE_DICTIONARY__", LocalizedText.remoteDictionary())
                        .replace("<html lang=\"ru\">", "<html lang=\"${LocaleManager.languageTag()}\" dir=\"${if (LocaleManager.languageTag() == "he") "rtl" else "ltr"}\">")
                }
                val bank = withContext(Dispatchers.IO) {
                    val assets = getApplication<android.app.Application>().assets
                    val localized = "questions/${LocaleManager.languageTag()}.json"
                    val base = runCatching { assets.open(localized) }
                        .getOrElse { assets.open("questions/ru.json") }
                        .use { QuestionBank.load(it) }
                    val selected = packRepository.state(_language.value)
                    PackSelection.combine(base, selected.installed.filter { it.selected }.flatMap { it.questions },
                        selected.baseSelected, LocalizedText.get("Packs.true"), LocalizedText.get("Packs.false")) {
                        LocalizedText.get("Packs.answer", it)
                    }
                }
                // backToMenu() stops Ktor on Dispatchers.IO. A fast second start
                // must wait for that stop, otherwise both servers race for PORT.
                serverStopJob?.join()
                serverStopJob = null
                engine?.shutdown()
                val engine = GameEngine(bank, viewModelScope) { snap ->
                    val prev = _snapshot.value
                    _snapshot.value = snap
                    speakIfNeeded(snap)
                    sfx(prev, snap)
                    val key = remoteKey(snap)
                    if (key != lastRemoteKey) {
                        lastRemoteKey = key
                        viewModelScope.launch(Dispatchers.IO) { server?.broadcast(snap) }
                    }
                }
                nextEngine = engine
                engine.hostAddress = withContext(Dispatchers.IO) {
                    runCatching { localIpv4() }.getOrNull().orEmpty()
                }
                engine.wifiName = withContext(Dispatchers.IO) {
                    runCatching { wifiSsid(getApplication()) }.getOrNull().orEmpty()
                }
                engine.setPlan(plan)
                nextServer = HostServer(engine, viewModelScope, html, getApplication<android.app.Application>().assets)
                engine.resetToLobby()
                withContext(Dispatchers.IO) {
                    nextServer.start()
                }
                this@HostViewModel.engine = engine
                this@HostViewModel.server = nextServer
                _screen.value = Screen.SHOW
            } catch (e: CancellationException) {
                nextEngine?.shutdown()
                withContext(Dispatchers.IO) { runCatching { nextServer?.stop() } }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Unable to start show", e)
                nextEngine?.shutdown()
                withContext(Dispatchers.IO) { runCatching { nextServer?.stop() } }
                if (engine === nextEngine) engine = null
                if (server === nextServer) server = null
                _snapshot.value = null
                _screen.value = Screen.MENU
            } finally {
                _starting.value = false
            }
        }
    }

    fun skipIntro() { engine?.skipIntro() }
    fun skipWinner() { engine?.skipWinner() }
    fun newShow() { engine?.skipWinner() }

    fun backToMenu() {
        _starting.value = false
        engine?.shutdown()
        val serverToStop = server
        server = null
        val previousStop = serverStopJob
        serverStopJob = viewModelScope.launch(Dispatchers.IO) {
            previousStop?.join()
            serverToStop?.stop()
        }
        engine = null
        _snapshot.value = null
        _screen.value = Screen.MENU
        lastRemoteKey = null
        speech.stop()
        sounds.stopFx()
        sounds.startBed()
        _speaking.value = false
        _viseme.value = 0
    }

    private fun remoteKey(snap: GameSnapshot): String =
        buildString {
            append(snap.phase).append('|')
            append(snap.question?.id).append('|')
            append(snap.paused).append('|')
            append(snap.hostLine).append('|')
            append(snap.winnerId).append('|')
            append(snap.topicChooserId).append('|')
            append(snap.chosenTopic).append('|')
            append(snap.topics.joinToString(","))
            append(snap.bombHolderId).append('|')
            append(snap.bombStrikes).append('|')
            append(snap.bombExploded).append('|')
            append(snap.contestMarksGoal).append('|')
            append(snap.competition).append('|')
            append(snap.bettingOpen).append('|')
            snap.players.forEach { p ->
                append(p.id).append(':')
                append(p.ready).append(p.answered).append(p.score).append(p.lastCorrect).append(p.avatarIndex)
                append(p.eliminated).append(p.finalFrozen).append(p.finalTimeMs / 1000)
                append(p.contestMarks).append(p.bet).append(p.connected).append(';')
            }
        }

    private fun speakIfNeeded(snap: GameSnapshot) {
        if (snap.phase == Phase.TITLE) return
        if (snap.hostLine.isBlank() || snap.hostLine == lastSpoken) return
        lastSpoken = snap.hostLine
        speech.speak(snap.hostLine, snap.hostMood, "${snap.phase}-${snap.hostLine.hashCode()}")
    }

    private fun sfx(prev: GameSnapshot?, snap: GameSnapshot) {
        sounds.onSnapshot(prev, snap)
        when {
            prev != null && prev.players.size < snap.players.size ->
                tones.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        }
    }

    fun onAppPause() { sounds.onAppPause() }
    fun onAppResume() {
        speech.start()
        refreshSpeechOptions()
        sounds.onAppResume()
    }

    private fun deviceGameLanguage(): String {
        val device = LocaleManager.normalize(Locale.getDefault().language)
        return device.takeIf { it in LocaleManager.supportedLanguages() } ?: "en"
    }

    override fun onCleared() {
        backToMenu()
        speech.shutdown()
        sounds.stopAll()
        tones.release()
        super.onCleared()
    }

    private companion object {
        const val TAG = "HostViewModel"
    }
}
