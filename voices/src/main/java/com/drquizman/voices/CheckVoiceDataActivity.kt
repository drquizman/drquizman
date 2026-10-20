package com.drquizman.voices

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech

class CheckVoiceDataActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.action == TextToSpeech.Engine.ACTION_GET_SAMPLE_TEXT) {
            setResult(TextToSpeech.LANG_AVAILABLE, Intent().putExtra(TextToSpeech.Engine.EXTRA_SAMPLE_TEXT,
                "Добро пожаловать в Квизман! Выберите категорию и проверьте свои знания."))
        } else {
            setResult(TextToSpeech.Engine.CHECK_VOICE_DATA_PASS, Intent()
                .putStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES, arrayListOf("rus-RUS"))
                .putStringArrayListExtra(TextToSpeech.Engine.EXTRA_UNAVAILABLE_VOICES, arrayListOf()))
        }
        finish()
    }
}
