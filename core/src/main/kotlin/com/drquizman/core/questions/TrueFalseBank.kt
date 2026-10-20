package com.drquizman.core.questions

import com.drquizman.core.i18n.LocalizedText
import com.drquizman.core.model.Question

object TrueFalseBank {
    val questions: List<Question> get() = LocalizedText.resource("true-false.json").use {
        QuestionBank.readQuestions(it).map { question -> question.copy(trueFalse = true) }
    }
}

internal object FallbackMc {
    val questions: List<Question> get() = LocalizedText.resource("fallback-questions.json").use {
        QuestionBank.readQuestions(it)
    }
}
