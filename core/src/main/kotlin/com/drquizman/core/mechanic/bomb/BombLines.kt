package com.drquizman.core.mechanic.bomb

import com.drquizman.core.i18n.LocaleManager
import com.drquizman.core.i18n.ru.BombLinesRu
import com.drquizman.core.i18n.he.BombLinesHe
import com.drquizman.core.i18n.en.BombLinesEn
import com.drquizman.core.i18n.de.BombLinesDe
import com.drquizman.core.i18n.es.BombLinesEs
import com.drquizman.core.model.CompetitionType

object BombLines {
    val TITLE: String get() = when (LocaleManager.languageTag()) { "he" -> BombLinesHe.TITLE; "en" -> BombLinesEn.TITLE; "de" -> BombLinesDe.TITLE; "es" -> BombLinesEs.TITLE; else -> BombLinesRu.TITLE }
    val SLOGAN: String get() = when (LocaleManager.languageTag()) { "he" -> BombLinesHe.SLOGAN; "en" -> BombLinesEn.SLOGAN; "de" -> BombLinesDe.SLOGAN; "es" -> BombLinesEs.SLOGAN; else -> BombLinesRu.SLOGAN }
    fun intro(): String = when (LocaleManager.languageTag()) { "he" -> BombLinesHe.intro(); "en" -> BombLinesEn.intro(); "de" -> BombLinesDe.intro(); "es" -> BombLinesEs.intro(); else -> BombLinesRu.intro() }
    fun yourTurn(name: String): String = when (LocaleManager.languageTag()) { "he" -> BombLinesHe.yourTurn(name); "en" -> BombLinesEn.yourTurn(name); "de" -> BombLinesDe.yourTurn(name); "es" -> BombLinesEs.yourTurn(name); else -> BombLinesRu.yourTurn(name) }
    fun passed(from: String, to: String): String = when (LocaleManager.languageTag()) { "he" -> BombLinesHe.passed(from, to); "en" -> BombLinesEn.passed(from, to); "de" -> BombLinesDe.passed(from, to); "es" -> BombLinesEs.passed(from, to); else -> BombLinesRu.passed(from, to) }
    fun strike(name: String, left: Int): String = when (LocaleManager.languageTag()) { "he" -> BombLinesHe.strike(name, left); "en" -> BombLinesEn.strike(name, left); "de" -> BombLinesDe.strike(name, left); "es" -> BombLinesEs.strike(name, left); else -> BombLinesRu.strike(name, left) }
    fun exploded(name: String, penalty: Int): String = when (LocaleManager.languageTag()) { "he" -> BombLinesHe.exploded(name, penalty); "en" -> BombLinesEn.exploded(name, penalty); "de" -> BombLinesDe.exploded(name, penalty); "es" -> BombLinesEs.exploded(name, penalty); else -> BombLinesRu.exploded(name, penalty) }
    fun survived(name: String): String = when (LocaleManager.languageTag()) { "he" -> BombLinesHe.survived(name); "en" -> BombLinesEn.survived(name); "de" -> BombLinesDe.survived(name); "es" -> BombLinesEs.survived(name); else -> BombLinesRu.survived(name) }
}
