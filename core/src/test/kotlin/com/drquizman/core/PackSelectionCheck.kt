package com.drquizman.core

import com.drquizman.core.model.Question
import com.drquizman.core.questions.PackSelection
import com.drquizman.core.questions.QuestionBank

fun main() {
    fun questions(count: Int, topics: Int) = (0 until count).map {
        Question(it, "Extra $it", listOf("A", "B", "C", "D"), 0, "Topic ${it % topics}")
    }
    check(!PackSelection.canDisableBase(questions(750, 15)))
    check(!PackSelection.canDisableBase(questions(1000, 9)))
    check(!PackSelection.canDisableBase(questions(1001, 8)))
    check(PackSelection.canDisableBase(questions(1001, 9)))
    check(!PackSelection.canDisableBase(questions(750, 15) + questions(750, 15)))
    check(!PackSelection.canDisableBase(questions(1001, 9).map { it.copy(topic = " ") }))
    val base = QuestionBank(listOf(Question(0, "Base", listOf("A", "B"), 0)),
        listOf(Question(1, "Base fact", listOf("True", "False"), 0, trueFalse = true)))
    fun combine(extra: List<Question>, selected: Boolean) = PackSelection.combine(base, extra, selected, "True", "False") { "Suggested: $it" }
    check(combine(questions(750, 15), false).multipleChoice.any { it.text == "Base" })
    val extraOnly = combine(questions(1001, 9), false)
    check(extraOnly.multipleChoice.size == 1001 && extraOnly.multipleChoice.none { it.text == "Base" })
    check(extraOnly.trueFalse.size == 1001 && extraOnly.trueFalse.none { it.text.contains("Base") })
    check(extraOnly.trueFalse.all { it.trueFalse && it.answers.size == 2 })
    val all = combine(questions(1001, 9), true)
    check(all.multipleChoice.size == 1002)
    check((all.multipleChoice + all.trueFalse).map { it.id }.distinct().size == 1003)
    check(combine(emptyList(), false).multipleChoice == base.multipleChoice.map { it.copy(id = 1) })
    println("Pack selection: threshold boundaries, deduplication, base fallback, extra-only rounds, and IDs passed")
}
