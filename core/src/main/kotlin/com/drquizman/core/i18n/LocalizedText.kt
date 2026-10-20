package com.drquizman.core.i18n

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.InputStream

/** UTF-8 resources with per-file and per-key Russian fallback. */
object LocalizedText {
    fun resource(file: String, language: String = LocaleManager.languageTag()): InputStream =
        LocalizedText::class.java.getResourceAsStream("/locales/$language/$file")
            ?: requireNotNull(LocalizedText::class.java.getResourceAsStream("/locales/ru/$file")) {
                "Missing locale resource: $file"
            }

    private val dictionaries = mutableMapOf<String, Map<String, String>>()
    fun remoteDictionary(): String {
        fun read(language: String) = resource("remote.json", language).bufferedReader(Charsets.UTF_8).use {
            Json.decodeFromString<Map<String, String>>(it.readText())
        }
        return Json.encodeToString(read("ru") + read(LocaleManager.languageTag())).replace("<", "\\u003c")
    }
    @Synchronized
    private fun dictionary(language: String): Map<String, String> = dictionaries.getOrPut(language) {
        resource("strings.json", language).bufferedReader(Charsets.UTF_8).use {
            Json.decodeFromString<Map<String, String>>(it.readText())
        }
    }

    fun get(key: String, vararg arguments: Any?): String {
        val template = dictionary(LocaleManager.languageTag())[key] ?: dictionary("ru")[key] ?: key
        return Regex("\\{(\\d+)\\}").replace(template) { match ->
            arguments.getOrNull(match.groupValues[1].toInt())?.toString() ?: match.value
        }
    }
}
