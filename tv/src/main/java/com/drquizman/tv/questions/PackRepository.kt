package com.drquizman.tv.questions

import android.content.Context
import android.util.AtomicFile
import com.drquizman.core.model.Question
import com.drquizman.core.questions.PackSelection
import com.drquizman.core.questions.QuestionBank
import kotlinx.serialization.json.*
import java.io.File
import java.security.MessageDigest

data class PackInfo(val id: String, val title: String, val language: String, val count: Int,
                    val topics: Int, val sha256: String)
data class InstalledPack(val info: PackInfo, val selected: Boolean, val questions: List<Question>)
data class PacksState(val installed: List<InstalledPack> = emptyList(), val available: List<PackInfo> = emptyList(),
    val baseSelected: Boolean = true, val canDisableBase: Boolean = false, val linked: Boolean = false,
    val code: String? = null, val codeExpiresAt: Long = 0, val busy: Boolean = false, val messageKey: String? = null)

class PackRepository(context: Context) {
    private val directory = File(context.filesDir, "question-packs").apply { mkdirs() }
    private val prefs = context.getSharedPreferences("question-packs", Context.MODE_PRIVATE)
    var token: String
        get() = prefs.getString("deviceToken", "").orEmpty()
        set(value) { prefs.edit().putString("deviceToken", value).commit() }

    fun installed(language: String): List<InstalledPack> = directory.listFiles().orEmpty()
        .filter { it.extension == "pack" }.mapNotNull { file -> runCatching {
            val obj = Json.parseToJsonElement(file.readText()).jsonObject
            val info = parseInfo(obj.getValue("metadata").jsonObject)
            if (info.language != language) return@runCatching null
            val bytes = obj.getValue("content").jsonPrimitive.content.toByteArray(Charsets.UTF_8)
            val questions = validate(info, bytes)
            InstalledPack(info, prefs.getBoolean("selected.${info.id}", true), questions)
        }.getOrNull() }.sortedBy { it.info.title }

    fun state(language: String): PacksState {
        val installed = installed(language)
        val enough = PackSelection.canDisableBase(installed.filter { it.selected }.flatMap { it.questions })
        val base = prefs.getBoolean("base.$language", true) || !enough
        if (base) prefs.edit().putBoolean("base.$language", true).commit()
        return PacksState(installed = installed, baseSelected = base, canDisableBase = enough, linked = token.isNotEmpty())
    }

    fun select(id: String, selected: Boolean) { prefs.edit().putBoolean("selected.$id", selected).commit() }
    fun selectBase(language: String, selected: Boolean) {
        prefs.edit().putBoolean("base.$language", selected || !state(language).canDisableBase).commit()
    }

    fun install(info: PackInfo, bytes: ByteArray) {
        validate(info, bytes)
        val content = buildJsonObject {
            put("metadata", buildJsonObject {
                put("id", info.id); put("title", info.title); put("language", info.language)
                put("count", info.count); put("topics", info.topics); put("sha256", info.sha256)
            })
            put("content", bytes.toString(Charsets.UTF_8))
        }.toString().toByteArray(Charsets.UTF_8)
        val atomic = AtomicFile(File(directory, "${info.id}.pack"))
        val stream = atomic.startWrite()
        try { stream.write(content); atomic.finishWrite(stream) } catch (e: Exception) { atomic.failWrite(stream); throw e }
        // New installations are enabled. Updating an existing pack preserves its checkbox.
        if (!prefs.contains("selected.${info.id}")) select(info.id, true)
    }

    private fun validate(info: PackInfo, bytes: ByteArray): List<Question> {
        require(bytes.size <= 10 * 1024 * 1024)
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        require(hash.equals(info.sha256, true))
        val questions = QuestionBank.readQuestions(bytes.inputStream())
        require(questions.isNotEmpty() && questions.size == info.count)
        require(questions.map { it.id }.distinct().size == questions.size)
        require(questions.all { it.text.isNotBlank() && it.topic.isNotBlank() && it.answers.size == 4 &&
            it.correctIndex == 0 && it.answers.all(String::isNotBlank) && it.answers.distinct().size == 4 })
        require(questions.map { it.topic }.distinct().size == info.topics)
        return questions
    }

    companion object {
        fun parseInfo(obj: JsonObject): PackInfo {
            fun text(key: String) = obj.getValue(key).jsonPrimitive.content
            val info = PackInfo(text("id"), text("title"), text("language"),
                obj.getValue("count").jsonPrimitive.int, obj.getValue("topics").jsonPrimitive.int, text("sha256"))
            require(info.id.matches(Regex("[a-zA-Z0-9_-]{1,100}")))
            require(info.id != "base")
            require(info.language in setOf("ru", "en", "de", "es", "he"))
            require(info.count in 1..100000 && info.topics in 1..info.count && info.title.isNotBlank())
            require(info.sha256.matches(Regex("[a-fA-F0-9]{64}")))
            return info
        }
    }
}
