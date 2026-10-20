package com.drquizman.core

import com.drquizman.core.host.HostLines
import com.drquizman.core.i18n.LocaleManager
import com.drquizman.core.i18n.LocalizedText
import com.drquizman.core.mechanic.MechanicCatalog
import com.drquizman.core.model.CompetitionType
import com.drquizman.core.questions.QuestionBank
import com.drquizman.core.questions.TrueFalseBank
import kotlinx.serialization.json.Json

fun main() {
    LocaleManager.set("ru")
    check(HostLines.welcome().contains(Regex("[А-Яа-я]")))
    val russianTitle = MechanicCatalog[CompetitionType.BOMB]!!.title
    val originalTf = TrueFalseBank.questions
    check(originalTf.size == 30 && originalTf.all { it.trueFalse && it.correctIndex in it.answers.indices })
    check(QuestionBank.load(null).multipleChoice.size == 10)
    check(LocalizedText.get("GameEngine.002", 6).contains("6"))
    LocaleManager.set("he")
    check(LocaleManager.languageTag() == "he")
    check(HostLines.welcome().contains(Regex("[א-ת]")))
    check(MechanicCatalog[CompetitionType.BOMB]!!.title != russianTitle)
    check(LocalizedText.get("HostSpeech.015").contains(Regex("[א-ת]")))
    check(LocalizedText.get("GameEngine.002", 6).contains("6"))
    val hebrewTf = TrueFalseBank.questions
    check(hebrewTf.map { it.id to it.correctIndex } == originalTf.map { it.id to it.correctIndex })
    check(hebrewTf.all { it.text.contains(Regex("[א-ת]")) })
    check(QuestionBank.load(null).multipleChoice.size == 9)
    check(LocalizedText.get("GameEngine.002", 6).contains(Regex("[א-ת]")))
    val remote = Json.decodeFromString<Map<String, String>>(LocalizedText.remoteDictionary())
    check(remote["remote.016"]!!.contains("{0}"))
    LocaleManager.set("iw")
    check(LocaleManager.languageTag() == "he")
    LocaleManager.set("ru")
    check(MechanicCatalog[CompetitionType.BOMB]!!.title == russianTitle)
    LocaleManager.set("xx")
    check(LocaleManager.languageTag() == "ru")
    LocaleManager.set("en")
    check(LocaleManager.languageTag() == "en")
    check(LocaleManager.nativeName("en") == "English")
    check(HostLines.welcome().startsWith("Hello") || HostLines.matchStart().contains("battle"))
    check(MechanicCatalog[CompetitionType.BOMB]!!.title == "Bomb")
    check(LocalizedText.get("LanguagePicker.title") == "Choose a language")
    check(LocalizedText.get("HostSpeech.015") == "Hello, my intellectual friends!")
    check(TrueFalseBank.questions.map { it.id to it.correctIndex } == originalTf.map { it.id to it.correctIndex })
    check(TrueFalseBank.questions.all { !it.text.contains(Regex("[А-Яа-яא-ת]")) })
    check(QuestionBank.load(null).multipleChoice.size == 9)
    LocaleManager.set("ru")
    check(MechanicCatalog[CompetitionType.BOMB]!!.title == russianTitle)
    LocaleManager.set("es")
    check(LocaleManager.nativeName("es") == "Español")
    check(LocaleManager.flag("es") == "🇪🇸")
    check(HostLines.matchStart().contains("Empezamos"))
    check(MechanicCatalog[CompetitionType.BOMB]!!.title == "Bomba")
    check(LocalizedText.get("LanguagePicker.title") == "Elige un idioma")
    check(TrueFalseBank.questions.map { it.id to it.correctIndex } == originalTf.map { it.id to it.correctIndex })
    check(QuestionBank.load(null).multipleChoice.size == 9)
    LocaleManager.set("ru")
    check(MechanicCatalog[CompetitionType.BOMB]!!.title == russianTitle)
    LocaleManager.set("de")
    check(LocaleManager.nativeName("de") == "Deutsch")
    check(LocaleManager.flag("de") == "🇩🇪")
    check(HostLines.topicPicked("Test").startsWith("Rundenthema"))
    check(MechanicCatalog[CompetitionType.BOMB]!!.title == "Bombe")
    check(LocalizedText.get("LanguagePicker.title") == "Sprache auswählen")
    check(TrueFalseBank.questions.map { it.id to it.correctIndex } == originalTf.map { it.id to it.correctIndex })
    check(QuestionBank.load(null).multipleChoice.size == 9)
    LocaleManager.set("ru")
    check(MechanicCatalog[CompetitionType.BOMB]!!.title == russianTitle)
    println("Localization checks passed: switching, catalog refresh, UTF-8, templates, question banks, fallback, Hebrew alias.")
}
