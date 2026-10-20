package com.drquizman.voices

object SpeechAudio {
    /** Short chunks bound memory and allow cancellation between native inference calls. */
    fun chunks(text: String, limit: Int = 180): List<String> {
        require(limit >= 2)
        val result = mutableListOf<String>()
        var rest = text.replace(Regex("\\s+"), " ").trim()
        while (rest.isNotEmpty()) {
            if (rest.length <= limit) { result += rest; break }
            val head = rest.take(limit)
            var end = head.indexOfLast { it in ".!?;:。！？" }.takeIf { it >= limit / 3 }?.plus(1)
                ?: head.lastIndexOf(' ').takeIf { it > 0 } ?: limit
            if (Character.isHighSurrogate(rest[end - 1])) end--
            result += rest.substring(0, end).trim()
            rest = rest.substring(end).trimStart()
        }
        return result
    }

    fun pcm16(samples: FloatArray, offset: Int, count: Int): ByteArray {
        val out = ByteArray(count * 2)
        for (i in 0 until count) {
            val f = samples[offset + i].let { if (it.isFinite()) it else 0f }.coerceIn(-1f, 1f)
            val value = (f * 32767f).toInt()
            out[i * 2] = value.toByte()
            out[i * 2 + 1] = (value shr 8).toByte()
        }
        return out
    }
}
