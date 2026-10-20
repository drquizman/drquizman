package com.drquizman.voices

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TtsIntegrationTest {
    @Test fun installedEngineSynthesizesRussianVoicesAndRecoversAfterStop() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val initialized = CountDownLatch(1)
        var initCode = TextToSpeech.ERROR
        lateinit var client: TextToSpeech
        instrumentation.runOnMainSync {
            client = TextToSpeech(context, { initCode = it; initialized.countDown() }, context.packageName)
        }
        try {
            assertTrue("Engine initialization timed out", initialized.await(30, TimeUnit.SECONDS))
            assertEquals(TextToSpeech.SUCCESS, initCode)
            assertEquals(10, client.voices.size)
            assertTrue(client.voices.all { it.locale.language == "ru" && !it.isNetworkConnectionRequired })
            assertTrue(client.setLanguage(Locale("ru", "RU")) >= 0)
            assertEquals(TextToSpeech.LANG_NOT_SUPPORTED, client.isLanguageAvailable(Locale.JAPANESE))
            val pending = AtomicReference<CountDownLatch>()
            val expected = AtomicReference<String>()
            val failure = AtomicReference<String>()
            client.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) = Unit
                override fun onDone(id: String?) { if (id == expected.get()) pending.get()?.countDown() }
                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) {
                    if (id == expected.get()) { failure.set(id); pending.get()?.countDown() }
                }
            })
            for ((index, name) in listOf("Supertonic-M1-ru-RU", "Supertonic-F1-ru-RU").withIndex()) {
                assertEquals(TextToSpeech.SUCCESS, client.setVoice(client.voices.first { it.name == name }))
                client.setSpeechRate(if (index == 0) 1f else 1.2f)
                val file = File(context.cacheDir, "test-$index.wav")
                expected.set("test-$index"); pending.set(CountDownLatch(1))
                assertEquals(TextToSpeech.SUCCESS, client.synthesizeToFile("Привет! Это проверка голоса.", Bundle(), file, expected.get()))
                assertTrue("Synthesis timed out", pending.get().await(120, TimeUnit.SECONDS))
                assertNull("Synthesis error", failure.get())
                val bytes = file.readBytes()
                assertTrue("No audio samples", bytes.size > 44100)
                assertEquals("RIFF", String(bytes, 0, 4, Charsets.US_ASCII))
                assertEquals(44100, ByteBuffer.wrap(bytes, 24, 4).order(ByteOrder.LITTLE_ENDIAN).int)
                assertTrue("Silent output", bytes.drop(44).any { it.toInt() != 0 })
            }
            client.speak("Это длинная реплика. ".repeat(80), TextToSpeech.QUEUE_FLUSH, Bundle(), "cancel")
            Thread.sleep(200)
            assertEquals(TextToSpeech.SUCCESS, client.stop())
            expected.set("after-stop"); pending.set(CountDownLatch(1))
            val file = File(context.cacheDir, "after-stop.wav")
            client.synthesizeToFile("Продолжаем игру.", Bundle(), file, expected.get())
            assertTrue("Engine did not recover after stop", pending.get().await(120, TimeUnit.SECONDS))
            assertNull(failure.get())
            assertTrue(file.length() > 44100)
        } finally { client.stop(); client.shutdown() }
    }
}
