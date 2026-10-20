package com.drquizman.core.host

import com.drquizman.core.i18n.LocaleManager
import com.drquizman.core.i18n.ru.HostLinesRu
import com.drquizman.core.i18n.he.HostLinesHe
import com.drquizman.core.i18n.en.HostLinesEn
import com.drquizman.core.i18n.de.HostLinesDe
import com.drquizman.core.i18n.es.HostLinesEs
import com.drquizman.core.model.CompetitionType

object HostLines {
    fun welcome(): String = when (LocaleManager.languageTag()) { "he" -> HostLinesHe.welcome(); "en" -> HostLinesEn.welcome(); "de" -> HostLinesDe.welcome(); "es" -> HostLinesEs.welcome(); else -> HostLinesRu.welcome() }
    fun waiting(): String = when (LocaleManager.languageTag()) { "he" -> HostLinesHe.waiting(); "en" -> HostLinesEn.waiting(); "de" -> HostLinesDe.waiting(); "es" -> HostLinesEs.waiting(); else -> HostLinesRu.waiting() }
    fun lobbyChatter(count: Int, ready: Int, previous: String = ""): String = when (LocaleManager.languageTag()) { "he" -> HostLinesHe.lobbyChatter(count, ready, previous); "en" -> HostLinesEn.lobbyChatter(count, ready, previous); "de" -> HostLinesDe.lobbyChatter(count, ready, previous); "es" -> HostLinesEs.lobbyChatter(count, ready, previous); else -> HostLinesRu.lobbyChatter(count, ready, previous) }
    fun playerJoined(name: String, first: Boolean): String = when (LocaleManager.languageTag()) { "he" -> HostLinesHe.playerJoined(name, first); "en" -> HostLinesEn.playerJoined(name, first); "de" -> HostLinesDe.playerJoined(name, first); "es" -> HostLinesEs.playerJoined(name, first); else -> HostLinesRu.playerJoined(name, first) }
    fun lobbyIdle(count: Int): String = when (LocaleManager.languageTag()) { "he" -> HostLinesHe.lobbyIdle(count); "en" -> HostLinesEn.lobbyIdle(count); "de" -> HostLinesDe.lobbyIdle(count); "es" -> HostLinesEs.lobbyIdle(count); else -> HostLinesRu.lobbyIdle(count) }
    fun lobbyWaiting(count: Int, ready: Int): String = when (LocaleManager.languageTag()) { "he" -> HostLinesHe.lobbyWaiting(count, ready); "en" -> HostLinesEn.lobbyWaiting(count, ready); "de" -> HostLinesDe.lobbyWaiting(count, ready); "es" -> HostLinesEs.lobbyWaiting(count, ready); else -> HostLinesRu.lobbyWaiting(count, ready) }
    fun matchStart(): String = when (LocaleManager.languageTag()) { "he" -> HostLinesHe.matchStart(); "en" -> HostLinesEn.matchStart(); "de" -> HostLinesDe.matchStart(); "es" -> HostLinesEs.matchStart(); else -> HostLinesRu.matchStart() }
    fun matchStartWithGrant(points: Int): String = when (LocaleManager.languageTag()) { "he" -> HostLinesHe.matchStartWithGrant(points); "en" -> HostLinesEn.matchStartWithGrant(points); "de" -> HostLinesDe.matchStartWithGrant(points); "es" -> HostLinesEs.matchStartWithGrant(points); else -> HostLinesRu.matchStartWithGrant(points) }
    fun intro(type: CompetitionType): String = when (LocaleManager.languageTag()) { "he" -> HostLinesHe.intro(type); "en" -> HostLinesEn.intro(type); "de" -> HostLinesDe.intro(type); "es" -> HostLinesEs.intro(type); else -> HostLinesRu.intro(type) }
    fun chooseTopic(name: String, lastPlace: Boolean): String = when (LocaleManager.languageTag()) { "he" -> HostLinesHe.chooseTopic(name, lastPlace); "en" -> HostLinesEn.chooseTopic(name, lastPlace); "de" -> HostLinesDe.chooseTopic(name, lastPlace); "es" -> HostLinesEs.chooseTopic(name, lastPlace); else -> HostLinesRu.chooseTopic(name, lastPlace) }
    fun topicPicked(topic: String): String = when (LocaleManager.languageTag()) { "he" -> HostLinesHe.topicPicked(topic); "en" -> HostLinesEn.topicPicked(topic); "de" -> HostLinesDe.topicPicked(topic); "es" -> HostLinesEs.topicPicked(topic); else -> HostLinesRu.topicPicked(topic) }
    fun titles(type: CompetitionType): String = when (LocaleManager.languageTag()) { "he" -> HostLinesHe.titles(type); "en" -> HostLinesEn.titles(type); "de" -> HostLinesDe.titles(type); "es" -> HostLinesEs.titles(type); else -> HostLinesRu.titles(type) }
    fun allCorrect(): String = when (LocaleManager.languageTag()) { "he" -> HostLinesHe.allCorrect(); "en" -> HostLinesEn.allCorrect(); "de" -> HostLinesDe.allCorrect(); "es" -> HostLinesEs.allCorrect(); else -> HostLinesRu.allCorrect() }
    fun oneCorrect(name: String): String = when (LocaleManager.languageTag()) { "he" -> HostLinesHe.oneCorrect(name); "en" -> HostLinesEn.oneCorrect(name); "de" -> HostLinesDe.oneCorrect(name); "es" -> HostLinesEs.oneCorrect(name); else -> HostLinesRu.oneCorrect(name) }
    fun allWrong(): String = when (LocaleManager.languageTag()) { "he" -> HostLinesHe.allWrong(); "en" -> HostLinesEn.allWrong(); "de" -> HostLinesDe.allWrong(); "es" -> HostLinesEs.allWrong(); else -> HostLinesRu.allWrong() }
    fun mixedResults(): String = when (LocaleManager.languageTag()) { "he" -> HostLinesHe.mixedResults(); "en" -> HostLinesEn.mixedResults(); "de" -> HostLinesDe.mixedResults(); "es" -> HostLinesEs.mixedResults(); else -> HostLinesRu.mixedResults() }
    fun afterFirstThird(): String = when (LocaleManager.languageTag()) { "he" -> HostLinesHe.afterFirstThird(); "en" -> HostLinesEn.afterFirstThird(); "de" -> HostLinesDe.afterFirstThird(); "es" -> HostLinesEs.afterFirstThird(); else -> HostLinesRu.afterFirstThird() }
    fun lastStretch(): String = when (LocaleManager.languageTag()) { "he" -> HostLinesHe.lastStretch(); "en" -> HostLinesEn.lastStretch(); "de" -> HostLinesDe.lastStretch(); "es" -> HostLinesEs.lastStretch(); else -> HostLinesRu.lastStretch() }
    fun questionFlavor(index: Int, total: Int): String? = when (LocaleManager.languageTag()) { "he" -> HostLinesHe.questionFlavor(index, total); "en" -> HostLinesEn.questionFlavor(index, total); "de" -> HostLinesDe.questionFlavor(index, total); "es" -> HostLinesEs.questionFlavor(index, total); else -> HostLinesRu.questionFlavor(index, total) }
    fun roundOver(): String = when (LocaleManager.languageTag()) { "he" -> HostLinesHe.roundOver(); "en" -> HostLinesEn.roundOver(); "de" -> HostLinesDe.roundOver(); "es" -> HostLinesEs.roundOver(); else -> HostLinesRu.roundOver() }
    fun standings(ranked: List<String>): String = when (LocaleManager.languageTag()) { "he" -> HostLinesHe.standings(ranked); "en" -> HostLinesEn.standings(ranked); "de" -> HostLinesDe.standings(ranked); "es" -> HostLinesEs.standings(ranked); else -> HostLinesRu.standings(ranked) }
    fun winner(name: String): String = when (LocaleManager.languageTag()) { "he" -> HostLinesHe.winner(name); "en" -> HostLinesEn.winner(name); "de" -> HostLinesDe.winner(name); "es" -> HostLinesEs.winner(name); else -> HostLinesRu.winner(name) }
    fun backToLobby(winner: String?): String = when (LocaleManager.languageTag()) { "he" -> HostLinesHe.backToLobby(winner); "en" -> HostLinesEn.backToLobby(winner); "de" -> HostLinesDe.backToLobby(winner); "es" -> HostLinesEs.backToLobby(winner); else -> HostLinesRu.backToLobby(winner) }
}
