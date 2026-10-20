package com.drquizman.voices

/** Speaker order in the upstream voice.bin: F1..F5, M1..M5. */
object VoiceCatalog {
    data class Entry(val sid: Int, val name: String, val label: String)
    val entries = (5..9).map { Entry(it, "Supertonic-M${it - 4}-ru-RU", "Мужской ${it - 4} · M${it - 4}") } +
        (0..4).map { Entry(it, "Supertonic-F${it + 1}-ru-RU", "Женский ${it + 1} · F${it + 1}") }
    val default get() = entries.first()
    fun find(name: String?) = entries.firstOrNull { it.name == name }
}
