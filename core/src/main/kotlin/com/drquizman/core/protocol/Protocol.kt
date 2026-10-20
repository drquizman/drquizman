package com.drquizman.core.protocol

import com.drquizman.core.model.GameSnapshot
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val ProtocolJson = Json {
    classDiscriminator = "type"
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
sealed interface ClientMsg {
    @Serializable
    @SerialName("hello")
    data class Hello(
        val pin: String,
        val name: String,
        val resumeToken: String? = null,
    ) : ClientMsg

    @Serializable
    @SerialName("ready")
    data class Ready(val ready: Boolean = true) : ClientMsg

    @Serializable
    @SerialName("avatar")
    data class PickAvatar(val index: Int) : ClientMsg

    @Serializable
    @SerialName("answer")
    data class Answer(val questionId: Int, val index: Int) : ClientMsg

    @Serializable
    @SerialName("topic")
    data class PickTopic(val topic: String) : ClientMsg

    @Serializable
    @SerialName("bet")
    data class Bet(val amount: Int) : ClientMsg

    @Serializable
    @SerialName("leave")
    data object Leave : ClientMsg

    @Serializable
    @SerialName("pause")
    data object Pause : ClientMsg

    @Serializable
    @SerialName("resume")
    data object Resume : ClientMsg
}

@Serializable
sealed interface ServerMsg {
    @Serializable
    @SerialName("welcome")
    data class Welcome(val playerId: String, val colorIndex: Int, val resumeToken: String) : ServerMsg

    @Serializable
    @SerialName("error")
    data class Error(val code: String, val message: String) : ServerMsg

    @Serializable
    @SerialName("snapshot")
    data class Snapshot(val state: GameSnapshot) : ServerMsg
}
