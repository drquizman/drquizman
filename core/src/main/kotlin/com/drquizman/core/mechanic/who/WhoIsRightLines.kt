package com.drquizman.core.mechanic.who

import com.drquizman.core.i18n.LocaleManager
import com.drquizman.core.i18n.ru.WhoIsRightLinesRu
import com.drquizman.core.i18n.he.WhoIsRightLinesHe
import com.drquizman.core.i18n.en.WhoIsRightLinesEn
import com.drquizman.core.i18n.de.WhoIsRightLinesDe
import com.drquizman.core.i18n.es.WhoIsRightLinesEs
import com.drquizman.core.model.CompetitionType

object WhoIsRightLines {
    val TITLE: String get() = when (LocaleManager.languageTag()) { "he" -> WhoIsRightLinesHe.TITLE; "en" -> WhoIsRightLinesEn.TITLE; "de" -> WhoIsRightLinesDe.TITLE; "es" -> WhoIsRightLinesEs.TITLE; else -> WhoIsRightLinesRu.TITLE }
    val SLOGAN: String get() = when (LocaleManager.languageTag()) { "he" -> WhoIsRightLinesHe.SLOGAN; "en" -> WhoIsRightLinesEn.SLOGAN; "de" -> WhoIsRightLinesDe.SLOGAN; "es" -> WhoIsRightLinesEs.SLOGAN; else -> WhoIsRightLinesRu.SLOGAN }
    fun intro(): String = when (LocaleManager.languageTag()) { "he" -> WhoIsRightLinesHe.intro(); "en" -> WhoIsRightLinesEn.intro(); "de" -> WhoIsRightLinesDe.intro(); "es" -> WhoIsRightLinesEs.intro(); else -> WhoIsRightLinesRu.intro() }
    fun race(leaderName: String?, leaderMarks: Int, atSix: List<String>, goal: Int): String = when (LocaleManager.languageTag()) { "he" -> WhoIsRightLinesHe.race(leaderName, leaderMarks, atSix, goal); "en" -> WhoIsRightLinesEn.race(leaderName, leaderMarks, atSix, goal); "de" -> WhoIsRightLinesDe.race(leaderName, leaderMarks, atSix, goal); "es" -> WhoIsRightLinesEs.race(leaderName, leaderMarks, atSix, goal); else -> WhoIsRightLinesRu.race(leaderName, leaderMarks, atSix, goal) }
    fun jackpot(name: String, prize: Int): String = when (LocaleManager.languageTag()) { "he" -> WhoIsRightLinesHe.jackpot(name, prize); "en" -> WhoIsRightLinesEn.jackpot(name, prize); "de" -> WhoIsRightLinesDe.jackpot(name, prize); "es" -> WhoIsRightLinesEs.jackpot(name, prize); else -> WhoIsRightLinesRu.jackpot(name, prize) }
    fun capOver(name: String?, perCorrect: Int): String = when (LocaleManager.languageTag()) { "he" -> WhoIsRightLinesHe.capOver(name, perCorrect); "en" -> WhoIsRightLinesEn.capOver(name, perCorrect); "de" -> WhoIsRightLinesDe.capOver(name, perCorrect); "es" -> WhoIsRightLinesEs.capOver(name, perCorrect); else -> WhoIsRightLinesRu.capOver(name, perCorrect) }
}
