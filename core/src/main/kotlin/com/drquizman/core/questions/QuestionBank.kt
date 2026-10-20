package com.drquizman.core.questions

import com.drquizman.core.model.Question
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream

@Serializable
private data class QuizmanFile(
    val Questions: List<QuizmanQ> = emptyList(),
)

@Serializable
private data class QuizmanQ(
    val Question_ID: Int,
    val Question: String,
    val Answers: List<String>,
    val Correct_Answer_Index: Int,
    val Topic: String = "",
)

class QuestionBank(
    val multipleChoice: List<Question>,
    val trueFalse: List<Question>,
) {
    fun shuffledMc(): ArrayDeque<Question> = ArrayDeque(multipleChoice.shuffled())
    fun shuffledMc(topic: String): ArrayDeque<Question> {
        val pool = multipleChoice.filter { it.topic == topic }.ifEmpty { multipleChoice }
        return ArrayDeque(pool.shuffled())
    }
    fun shuffledMcForEast(topic: String = ""): ArrayDeque<Question> {
        val topicPool = if (topic.isBlank()) multipleChoice else multipleChoice.filter { it.topic == topic }
        val preferred = topicPool.filter { it.hasAnswersWithAtMost(2) }.shuffled()
        val reserve = topicPool.filter {
            !it.hasAnswersWithAtMost(2) && it.hasAnswersWithAtMost(3)
        }.shuffled()
        return ArrayDeque(preferred + reserve)
    }
    fun shuffledTf(): ArrayDeque<Question> = ArrayDeque(trueFalse.shuffled())
    fun topicsWithAtLeast(min: Int): List<String> =
        multipleChoice.groupingBy { it.topic }.eachCount()
            .filter { it.key.isNotBlank() && it.value >= min }
            .keys.sorted()
    fun topicsForEastWithAtLeast(min: Int): List<String> =
        multipleChoice.filter { it.hasAnswersWithAtMost(3) }
            .groupingBy { it.topic }.eachCount()
            .filter { it.key.isNotBlank() && it.value >= min }
            .keys.sorted()

    private fun Question.hasAnswersWithAtMost(maxWords: Int): Boolean =
        answers.isNotEmpty() && answers.all { answer ->
            val trimmed = answer.trim()
            trimmed.isNotEmpty() && trimmed.split(Regex("\\s+")).size <= maxWords
        }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun readQuestions(stream: InputStream): List<Question> {
            val file = json.decodeFromString<QuizmanFile>(stream.bufferedReader(Charsets.UTF_8).readText())
            return file.Questions.map { q ->
                require(q.Answers.size >= 2 && q.Correct_Answer_Index in q.Answers.indices) {
                    "Invalid answers for question ${q.Question_ID}"
                }
                Question(q.Question_ID, q.Question, q.Answers, q.Correct_Answer_Index, q.Topic)
            }
        }

        fun load(mcStream: InputStream?, extraTf: List<Question> = TrueFalseBank.questions): QuestionBank {
            val mc = if (mcStream != null) {
                val text = mcStream.bufferedReader(Charsets.UTF_8).readText()
                val file = json.decodeFromString<QuizmanFile>(text)
                file.Questions.map { q ->
                    Question(
                        id = q.Question_ID,
                        text = q.Question,
                        answers = q.Answers,
                        correctIndex = q.Correct_Answer_Index.coerceIn(0, (q.Answers.size - 1).coerceAtLeast(0)),
                        topic = q.Topic,
                        trueFalse = false,
                    )
                }.filter { it.answers.size >= 2 }
            } else emptyList()
            val fallback = if (mc.isEmpty()) FallbackMc.questions else mc
            return QuestionBank(fallback, extraTf)
        }
    }
}

