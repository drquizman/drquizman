package com.drquizman.core.mechanic.east

import com.drquizman.core.i18n.LocaleManager
import com.drquizman.core.i18n.ru.EastLinesRu
import com.drquizman.core.i18n.he.EastLinesHe
import com.drquizman.core.i18n.en.EastLinesEn
import com.drquizman.core.i18n.de.EastLinesDe
import com.drquizman.core.i18n.es.EastLinesEs
import com.drquizman.core.model.CompetitionType

object EastLines {
    val TITLE: String get() = when (LocaleManager.languageTag()) { "he" -> EastLinesHe.TITLE; "en" -> EastLinesEn.TITLE; "de" -> EastLinesDe.TITLE; "es" -> EastLinesEs.TITLE; else -> EastLinesRu.TITLE }
    val SLOGAN: String get() = when (LocaleManager.languageTag()) { "he" -> EastLinesHe.SLOGAN; "en" -> EastLinesEn.SLOGAN; "de" -> EastLinesDe.SLOGAN; "es" -> EastLinesEs.SLOGAN; else -> EastLinesRu.SLOGAN }
    fun intro(): String = when (LocaleManager.languageTag()) { "he" -> EastLinesHe.intro(); "en" -> EastLinesEn.intro(); "de" -> EastLinesDe.intro(); "es" -> EastLinesEs.intro(); else -> EastLinesRu.intro() }
    fun results(scored: List<Pair<String, Int>>): String = when (LocaleManager.languageTag()) { "he" -> EastLinesHe.results(scored); "en" -> EastLinesEn.results(scored); "de" -> EastLinesDe.results(scored); "es" -> EastLinesEs.results(scored); else -> EastLinesRu.results(scored) }
}
