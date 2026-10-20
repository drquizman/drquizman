package com.drquizman.voices

import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import android.util.Log
import com.k2fsa.sherpa.onnx.*
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

class SupertonicService : TextToSpeechService() {
    private val cancellation = AtomicLong()
    private val modelLock = Any()
    private var model: OfflineTts? = null
    @Volatile private var loadedVoice = VoiceCatalog.default.name

    override fun onGetLanguage() = arrayOf("rus", "RUS", "")
    override fun onIsLanguageAvailable(lang: String, country: String, variant: String): Int =
        if (lang.equals("ru", true) || lang.equals("rus", true)) {
            if (country.isEmpty()) TextToSpeech.LANG_AVAILABLE
            else if (country.equals("RU", true) || country.equals("RUS", true)) TextToSpeech.LANG_COUNTRY_AVAILABLE
            else TextToSpeech.LANG_AVAILABLE
        } else TextToSpeech.LANG_NOT_SUPPORTED

    override fun onLoadLanguage(lang: String, country: String, variant: String) =
        onIsLanguageAvailable(lang, country, variant)

    override fun onGetVoices(): List<Voice> = VoiceCatalog.entries.map {
        Voice(it.name, Locale("ru", "RU"), Voice.QUALITY_HIGH, Voice.LATENCY_NORMAL, false, emptySet())
    }

    override fun onIsValidVoiceName(voiceName: String) =
        if (VoiceCatalog.find(voiceName) != null) TextToSpeech.SUCCESS else TextToSpeech.ERROR

    override fun onLoadVoice(voiceName: String): Int {
        if (onIsValidVoiceName(voiceName) != TextToSpeech.SUCCESS) return TextToSpeech.ERROR
        loadedVoice = voiceName
        return TextToSpeech.SUCCESS
    }

    override fun onGetDefaultVoiceNameFor(lang: String, country: String, variant: String): String? {
        if (onIsLanguageAvailable(lang, country, variant) < 0) return null
        val saved = getSharedPreferences("voices", MODE_PRIVATE).getString("voice", null)
        return (VoiceCatalog.find(saved) ?: VoiceCatalog.default).name
    }

    override fun onStop() { cancellation.incrementAndGet() }

    private fun engine(): OfflineTts = model ?: OfflineTts(
        assetManager = assets,
        config = OfflineTtsConfig(model = OfflineTtsModelConfig(
            supertonic = OfflineTtsSupertonicModelConfig(
                durationPredictor = "supertonic/duration_predictor.int8.onnx",
                textEncoder = "supertonic/text_encoder.int8.onnx",
                vectorEstimator = "supertonic/vector_estimator.int8.onnx",
                vocoder = "supertonic/vocoder.int8.onnx",
                ttsJson = "supertonic/tts.json",
                unicodeIndexer = "supertonic/unicode_indexer.bin",
                voiceStyle = "supertonic/voice.bin",
            ), numThreads = 2, provider = "cpu",
        )),
    ).also { model = it }

    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        val generation = cancellation.get()
        fun cancelled() = cancellation.get() != generation
        if (onIsLanguageAvailable(request.language, request.country, request.variant) < 0) {
            callback.error(TextToSpeech.ERROR_INVALID_REQUEST); return
        }
        val voice = VoiceCatalog.find(request.voiceName ?: loadedVoice)
        if (voice == null) { callback.error(TextToSpeech.ERROR_INVALID_REQUEST); return }
        synchronized(modelLock) {
            try {
                if (cancelled()) return
                val tts = engine()
                if (cancelled()) return
                if (callback.start(tts.sampleRate(), AudioFormat.ENCODING_PCM_16BIT, 1) != TextToSpeech.SUCCESS) return
                val config = GenerationConfig(sid = voice.sid, speed = (request.speechRate / 100f).coerceIn(0.5f, 2f),
                    numSteps = 5, extra = mapOf("lang" to "ru"))
                for (chunk in SpeechAudio.chunks(request.charSequenceText.toString())) {
                    if (cancelled()) return
                    var sent = false
                    var failed = false
                    fun send(samples: FloatArray): Int {
                        var offset = 0
                        val maxSamples = (callback.maxBufferSize / 2).coerceAtLeast(1)
                        while (offset < samples.size && !cancelled()) {
                            val count = minOf(maxSamples, samples.size - offset)
                            val pcm = SpeechAudio.pcm16(samples, offset, count)
                            if (callback.audioAvailable(pcm, 0, pcm.size) != TextToSpeech.SUCCESS) {
                                failed = true; return 0
                            }
                            sent = true
                            offset += count
                        }
                        return if (cancelled()) 0 else 1
                    }
                    val audio = tts.generateWithConfigAndCallback(chunk, config, ::send)
                    if (cancelled() || failed) return
                    if (!sent && audio.samples.isNotEmpty()) send(audio.samples)
                    if (cancelled() || failed) return
                    if (!sent) { callback.error(TextToSpeech.ERROR_SYNTHESIS); return }
                }
                if (!cancelled()) callback.done()
            } catch (e: Exception) {
                Log.e("QuizmanVoices", "Synthesis failed", e)
                if (!cancelled()) callback.error(TextToSpeech.ERROR_SYNTHESIS)
            } catch (e: LinkageError) {
                Log.e("QuizmanVoices", "Native runtime unavailable", e)
                if (!cancelled()) callback.error(TextToSpeech.ERROR_SERVICE)
            }
        }
    }

    override fun onDestroy() {
        onStop()
        synchronized(modelLock) { model?.free(); model = null }
        super.onDestroy()
    }
}
