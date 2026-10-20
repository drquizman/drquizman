package com.drquizman.core.i18n

import java.util.Locale

/** Locale used by all user-facing game text. Russian is kept as the safe default. */
object LocaleManager {
    private val supported = setOf("ru", "he", "en", "es", "de")
    @Volatile private var current: Locale = Locale("ru")

    fun get(): Locale = current

    fun set(locale: Locale) {
        val code = normalize(locale.language)
        current = Locale.forLanguageTag(if (code in supported) code else "ru")
    }

    fun set(languageTag: String) = set(Locale.forLanguageTag(languageTag))
    fun languageTag(): String = current.toLanguageTag()
    fun supportedLanguages(): Set<String> = supported
    fun nativeName(code: String): String {
        val locale = Locale.forLanguageTag(code)
        return locale.getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) }
    }
    fun flag(code: String): String = when (normalize(code)) {
        "ru" -> "🇷🇺"
        "he" -> "🇮🇱"
        "en" -> "🇬🇧"
        "es" -> "🇪🇸"
        "de" -> "🇩🇪"
        else -> "🌐"
    }
    fun normalize(language: String): String = if (language == "iw") "he" else language.lowercase(Locale.ROOT)
}
