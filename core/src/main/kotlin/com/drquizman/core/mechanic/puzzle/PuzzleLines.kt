package com.drquizman.core.mechanic.puzzle

import com.drquizman.core.i18n.LocaleManager
import com.drquizman.core.i18n.ru.PuzzleLinesRu
import com.drquizman.core.i18n.he.PuzzleLinesHe
import com.drquizman.core.i18n.en.PuzzleLinesEn
import com.drquizman.core.i18n.de.PuzzleLinesDe
import com.drquizman.core.i18n.es.PuzzleLinesEs
import com.drquizman.core.model.CompetitionType

object PuzzleLines {
    val TITLE: String get() = when (LocaleManager.languageTag()) { "he" -> PuzzleLinesHe.TITLE; "en" -> PuzzleLinesEn.TITLE; "de" -> PuzzleLinesDe.TITLE; "es" -> PuzzleLinesEs.TITLE; else -> PuzzleLinesRu.TITLE }
    val SLOGAN: String get() = when (LocaleManager.languageTag()) { "he" -> PuzzleLinesHe.SLOGAN; "en" -> PuzzleLinesEn.SLOGAN; "de" -> PuzzleLinesDe.SLOGAN; "es" -> PuzzleLinesEs.SLOGAN; else -> PuzzleLinesRu.SLOGAN }
    fun intro(): String = when (LocaleManager.languageTag()) { "he" -> PuzzleLinesHe.intro(); "en" -> PuzzleLinesEn.intro(); "de" -> PuzzleLinesDe.intro(); "es" -> PuzzleLinesEs.intro(); else -> PuzzleLinesRu.intro() }
    fun correct(name: String): String = when (LocaleManager.languageTag()) { "he" -> PuzzleLinesHe.correct(name); "en" -> PuzzleLinesEn.correct(name); "de" -> PuzzleLinesDe.correct(name); "es" -> PuzzleLinesEs.correct(name); else -> PuzzleLinesRu.correct(name) }
    fun wrong(name: String): String = when (LocaleManager.languageTag()) { "he" -> PuzzleLinesHe.wrong(name); "en" -> PuzzleLinesEn.wrong(name); "de" -> PuzzleLinesDe.wrong(name); "es" -> PuzzleLinesEs.wrong(name); else -> PuzzleLinesRu.wrong(name) }
    fun timeout(): String = when (LocaleManager.languageTag()) { "he" -> PuzzleLinesHe.timeout(); "en" -> PuzzleLinesEn.timeout(); "de" -> PuzzleLinesDe.timeout(); "es" -> PuzzleLinesEs.timeout(); else -> PuzzleLinesRu.timeout() }
    fun out(names: List<String>): String = when (LocaleManager.languageTag()) { "he" -> PuzzleLinesHe.out(names); "en" -> PuzzleLinesEn.out(names); "de" -> PuzzleLinesDe.out(names); "es" -> PuzzleLinesEs.out(names); else -> PuzzleLinesRu.out(names) }
    fun settle(lead: String, payouts: List<Pair<String, Int>>): String = when (LocaleManager.languageTag()) { "he" -> PuzzleLinesHe.settle(lead, payouts); "en" -> PuzzleLinesEn.settle(lead, payouts); "de" -> PuzzleLinesDe.settle(lead, payouts); "es" -> PuzzleLinesEs.settle(lead, payouts); else -> PuzzleLinesRu.settle(lead, payouts) }
}
