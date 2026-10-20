package com.drquizman.core.mechanic.bets

import com.drquizman.core.i18n.LocaleManager
import com.drquizman.core.i18n.ru.BetsLinesRu
import com.drquizman.core.i18n.he.BetsLinesHe
import com.drquizman.core.i18n.en.BetsLinesEn
import com.drquizman.core.i18n.de.BetsLinesDe
import com.drquizman.core.i18n.es.BetsLinesEs
import com.drquizman.core.model.CompetitionType

object BetsLines {
    val TITLE: String get() = when (LocaleManager.languageTag()) { "he" -> BetsLinesHe.TITLE; "en" -> BetsLinesEn.TITLE; "de" -> BetsLinesDe.TITLE; "es" -> BetsLinesEs.TITLE; else -> BetsLinesRu.TITLE }
    val SLOGAN: String get() = when (LocaleManager.languageTag()) { "he" -> BetsLinesHe.SLOGAN; "en" -> BetsLinesEn.SLOGAN; "de" -> BetsLinesDe.SLOGAN; "es" -> BetsLinesEs.SLOGAN; else -> BetsLinesRu.SLOGAN }
    fun intro(): String = when (LocaleManager.languageTag()) { "he" -> BetsLinesHe.intro(); "en" -> BetsLinesEn.intro(); "de" -> BetsLinesDe.intro(); "es" -> BetsLinesEs.intro(); else -> BetsLinesRu.intro() }
    fun announceTopic(topic: String, index: Int, total: Int): String = when (LocaleManager.languageTag()) { "he" -> BetsLinesHe.announceTopic(topic, index, total); "en" -> BetsLinesEn.announceTopic(topic, index, total); "de" -> BetsLinesDe.announceTopic(topic, index, total); "es" -> BetsLinesEs.announceTopic(topic, index, total); else -> BetsLinesRu.announceTopic(topic, index, total) }
    fun placeBets(): String = when (LocaleManager.languageTag()) { "he" -> BetsLinesHe.placeBets(); "en" -> BetsLinesEn.placeBets(); "de" -> BetsLinesDe.placeBets(); "es" -> BetsLinesEs.placeBets(); else -> BetsLinesRu.placeBets() }
    fun results(
        won: List<Pair<String, Int>>,
        lost: List<Pair<String, Int>>,
        skipped: List<String>,
    ): String = when (LocaleManager.languageTag()) { "he" -> BetsLinesHe.results(won, lost, skipped); "en" -> BetsLinesEn.results(won, lost, skipped); "de" -> BetsLinesDe.results(won, lost, skipped); "es" -> BetsLinesEs.results(won, lost, skipped); else -> BetsLinesRu.results(won, lost, skipped) }
}
