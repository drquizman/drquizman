package com.drquizman.core.questions

import com.drquizman.core.model.Question
import java.util.Locale

/** Count content, not file IDs: separate packs may reuse source IDs or questions. */
object PackSelection {
    fun unique(questions: List<Question>): List<Question> = questions.distinctBy {
        it.text.trim().lowercase(Locale.ROOT) to it.answers[it.correctIndex].trim().lowercase(Locale.ROOT)
    }

    fun canDisableBase(additional: List<Question>): Boolean {
        val unique = unique(additional)
        return unique.size > 1000 && unique.map { it.topic.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }.distinct().size > 8
    }

    fun combine(base: QuestionBank, additional: List<Question>, baseSelected: Boolean,
                trueLabel: String, falseLabel: String, answerLabel: (String) -> String): QuestionBank {
        val includeBase = baseSelected || !canDisableBase(additional)
        val mc = unique((if (includeBase) base.multipleChoice else emptyList()) + additional)
            .mapIndexed { index, question -> question.copy(id = index + 1) }
        // The true/false round must not silently draw from the excluded base pack.
        val tf = if (includeBase) base.trueFalse.mapIndexed { index, question -> question.copy(id = mc.size + index + 1) }
        else mc.mapIndexed { index, question ->
            val truthful = index % 2 == 0
            val answer = if (truthful) question.answers[question.correctIndex]
                else question.answers.indices.first { it != question.correctIndex }.let { question.answers[it] }
            Question(mc.size + index + 1, question.text + "\n" + answerLabel(answer),
                listOf(trueLabel, falseLabel), if (truthful) 0 else 1, question.topic, true)
        }
        return QuestionBank(mc, tf)
    }
}
