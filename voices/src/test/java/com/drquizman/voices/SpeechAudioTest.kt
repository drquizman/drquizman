package com.drquizman.voices

import org.junit.Assert.*
import org.junit.Test

class SpeechAudioTest {
    @Test fun chunksKeepWordsAndLimitMemory() {
        val input = (1..80).joinToString(" ") { "Слово$it" }
        val chunks = SpeechAudio.chunks(input)
        assertEquals(input, chunks.joinToString(" "))
        assertTrue(chunks.all { it.length <= 180 && it.isNotBlank() })
    }
    @Test fun longWordsAndSurrogatesAreSafe() {
        val input = "😀".repeat(200)
        val parts = SpeechAudio.chunks(input, 179)
        assertEquals(input, parts.joinToString(""))
        assertTrue(parts.all { it.length <= 179 && !Character.isHighSurrogate(it.last()) })
        assertTrue(SpeechAudio.chunks("  \n  ").isEmpty())
    }
    @Test fun pcmIsLittleEndianAndClampsInvalidInput() {
        val bytes = SpeechAudio.pcm16(floatArrayOf(-2f, 0f, 2f, Float.NaN), 0, 4)
        assertArrayEquals(byteArrayOf(1, -128, 0, 0, -1, 127, 0, 0), bytes)
    }
    @Test fun speakerIdsMatchBundledModel() {
        assertEquals((0..9).toList(), VoiceCatalog.entries.map { it.sid }.sorted())
        assertEquals(5, VoiceCatalog.default.sid)
        assertEquals(0, VoiceCatalog.find("Supertonic-F1-ru-RU")?.sid)
        assertNull(VoiceCatalog.find("unknown"))
    }
}
