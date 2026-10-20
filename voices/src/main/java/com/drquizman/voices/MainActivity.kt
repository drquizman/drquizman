package com.drquizman.voices

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.InputFilter
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.util.Locale

class MainActivity : Activity() {
    private var tts: TextToSpeech? = null
    private var ready = false
    private lateinit var status: TextView
    private lateinit var play: Button
    private lateinit var voices: Spinner
    private lateinit var text: EditText
    private lateinit var speed: SeekBar
    private var activeId: String? = null
    private val prefs by lazy { getSharedPreferences("voices", MODE_PRIVATE) }
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(20), dp(24), dp(24))
        }
        val outer = android.widget.FrameLayout(this)
        outer.addView(column, android.widget.FrameLayout.LayoutParams(
            minOf(resources.displayMetrics.widthPixels, dp(680)), ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL))
        scroll.addView(outer)
        setContentView(scroll)
        scroll.setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            insets
        }
        fun label(value: String, size: Float = 16f): TextView = TextView(this).apply {
            text = value; textSize = size; setTextColor(Color.rgb(27, 45, 39))
            setPadding(0, dp(8), 0, dp(8))
            column.addView(this)
        }
        fun button(value: String, action: () -> Unit): Button = Button(this).apply {
            text = value; isAllCaps = false; minHeight = dp(48)
            setOnClickListener { action() }
            column.addView(this, LinearLayout.LayoutParams(-1, -2))
        }
        label("Quizman Voices", 28f)
        label("Supertonic 3 · 10 голосов · Без интернета", 14f)
        status = label("Подключаем движок…", 14f)
        label("Голос", 18f)
        voices = Spinner(this).apply {
            contentDescription = "Выбор голоса"
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item,
                VoiceCatalog.entries.map { it.label })
            minimumHeight = dp(48)
        }
        column.addView(voices, LinearLayout.LayoutParams(-1, -2))
        val saved = VoiceCatalog.find(prefs.getString("voice", null)) ?: VoiceCatalog.default
        voices.setSelection(VoiceCatalog.entries.indexOf(saved))
        voices.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                stopPreview()
                val entry = VoiceCatalog.entries[position]
                prefs.edit().putString("voice", entry.name).apply()
                if (ready) tts?.voices?.firstOrNull { it.name == entry.name }?.let { tts?.voice = it }
            }
        }
        val speedLabel = label("Скорость: 1,00×")
        speed = SeekBar(this).apply {
            max = 80; progress = prefs.getInt("speed", 30).coerceIn(0, 80)
            contentDescription = "Скорость речи"
        }
        column.addView(speed, LinearLayout.LayoutParams(-1, dp(48)))
        fun updateSpeed() {
            speedLabel.text = String.format(Locale("ru"), "Скорость: %.2f×", (speed.progress + 70) / 100f)
        }
        updateSpeed()
        speed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) { updateSpeed() }
            override fun onStartTrackingTouch(bar: SeekBar?) = Unit
            override fun onStopTrackingTouch(bar: SeekBar?) { prefs.edit().putInt("speed", speed.progress).apply() }
        })
        text = EditText(this).apply {
            setText(savedInstanceState?.getString("text") ?: "Добро пожаловать в Квизман! Какую категорию выбираем?")
            hint = "Текст для проверки"
            contentDescription = "Текст для проверки"
            minLines = 2; maxLines = 4; textSize = 17f
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            filters = arrayOf(InputFilter.LengthFilter(1000))
        }
        column.addView(text, LinearLayout.LayoutParams(-1, -2))
        play = button("Прослушать") { preview() }.apply { isEnabled = false }
        button("Стоп") { stopPreview() }
        label("В Квизмане выберите движок Quizman Voices, затем нужный голос. Все 10 голосов уже установлены.", 14f)
        button("Открыть Квизман") {
            val intent = packageManager.getLeanbackLaunchIntentForPackage("com.drquizman.tv")
                ?: packageManager.getLaunchIntentForPackage("com.drquizman.tv")
            if (intent != null) startActivity(intent)
            else Toast.makeText(this, "Квизман пока не установлен на этом устройстве", Toast.LENGTH_LONG).show()
        }
        button("Настройки озвучивания Android") {
            try { startActivity(Intent("com.android.settings.TTS_SETTINGS")) }
            catch (_: android.content.ActivityNotFoundException) {
                Toast.makeText(this, "Выберите движок в настройках языка и озвучивания Android", Toast.LENGTH_LONG).show()
            }
        }
        button("О приложении и лицензии") { showLicenses() }
        tts = TextToSpeech(this, { code -> runOnUiThread {
            if (isDestroyed) return@runOnUiThread
            val engine = tts
            ready = code == TextToSpeech.SUCCESS && engine?.voices.orEmpty().any { VoiceCatalog.find(it.name) != null }
            play.isEnabled = ready
            status.text = if (ready) "Готово. При первом прослушивании модель загружается несколько секунд."
                else "Не удалось подключить Quizman Voices. Закройте и откройте приложение."
            if (ready) {
                engine?.language = Locale("ru", "RU")
                engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) = update(id, "Подготавливаем и воспроизводим речь…")
                    override fun onDone(id: String?) = update(id, "Готово к прослушиванию", true)
                    @Deprecated("Deprecated in Java")
                    override fun onError(id: String?) = update(id, "Не удалось озвучить текст. Попробуйте ещё раз.", true)
                    override fun onError(id: String?, errorCode: Int) = onError(id)
                    override fun onStop(id: String?, interrupted: Boolean) = update(id, "Остановлено", true)
                })
            }
        } }, packageName)
    }

    private fun update(id: String?, message: String, finished: Boolean = false) = runOnUiThread {
        if (!isDestroyed && id == activeId) {
            status.text = message
            if (finished) { activeId = null; play.isEnabled = ready }
        }
    }

    private fun preview() {
        if (!ready) return
        val input = text.text.toString().trim()
        if (input.isEmpty()) { text.error = "Введите текст"; return }
        val entry = VoiceCatalog.entries[voices.selectedItemPosition]
        val engine = tts ?: return
        engine.voices.firstOrNull { it.name == entry.name }?.let { engine.voice = it }
        engine.setSpeechRate((speed.progress + 70) / 100f)
        prefs.edit().putInt("speed", speed.progress).apply()
        activeId = "preview-${System.nanoTime()}"
        play.isEnabled = false
        status.text = "Подготавливаем речь…"
        if (engine.speak(input, TextToSpeech.QUEUE_FLUSH, Bundle(), activeId) != TextToSpeech.SUCCESS) {
            status.text = "Не удалось запустить озвучивание"; play.isEnabled = true; activeId = null
        }
    }

    private fun stopPreview() {
        activeId = null
        tts?.stop()
        if (::status.isInitialized && ready) status.text = "Готово к прослушиванию"
        if (::play.isInitialized) play.isEnabled = ready
    }

    private fun showLicenses() {
        val content = listOf("THIRD_PARTY_NOTICES.txt", "LICENSE-supertonic-model.txt", "LICENSE-sherpa-onnx.txt").joinToString("\n\n") {
            assets.open(it).bufferedReader().use { reader -> reader.readText() }
        }
        val body = TextView(this).apply { text = content; textSize = 13f; setPadding(dp(20), dp(12), dp(20), dp(12)) }
        AlertDialog.Builder(this).setTitle("Quizman Voices 0.1")
            .setView(ScrollView(this).apply { addView(body) }).setPositiveButton("Закрыть", null).show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("text", text.text.toString())
        super.onSaveInstanceState(outState)
    }
    override fun onStop() { stopPreview(); super.onStop() }
    override fun onDestroy() { tts?.shutdown(); tts = null; super.onDestroy() }
}
