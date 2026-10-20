package com.drquizman.tv.questions

import kotlinx.serialization.json.*
import java.net.HttpURLConnection
import java.net.URL

/** The bot approves a server-created code; only the server can issue a device token. */
class PackService(private val origin: String) {
    val configured get() = origin.startsWith("https://")
    data class Pairing(val code: String, val pollToken: String, val expiresAt: Long)

    fun begin(): Pairing {
        val result = json("/v1/device-links", "POST")
        val code = result.getValue("code").jsonPrimitive.content
        val pollToken = result.getValue("pollToken").jsonPrimitive.content
        val expires = result.getValue("expiresAt").jsonPrimitive.long * 1000
        require(code.matches(Regex("[A-Z0-9-]{4,16}")) && pollToken.isNotBlank())
        require(expires > System.currentTimeMillis() && expires <= System.currentTimeMillis() + 15 * 60 * 1000)
        return Pairing(code, pollToken, expires)
    }
    fun poll(pairing: Pairing): String? {
        val result = json("/v1/device-links/status", token = pairing.pollToken)
        return if (result.getValue("status").jsonPrimitive.content == "approved")
            result.getValue("deviceToken").jsonPrimitive.content.also { require(it.isNotBlank()) } else null
    }
    fun purchases(token: String, language: String): List<PackInfo> = json("/v1/packs?language=$language", token = token)
        .getValue("packs").jsonArray.map { PackRepository.parseInfo(it.jsonObject) }
        .also { list -> require(list.all { it.language == language }); require(list.map { it.id }.distinct().size == list.size) }
    fun download(token: String, info: PackInfo): ByteArray = request("/v1/packs/${info.id}/download", "GET", token, 10 * 1024 * 1024)
    private fun json(path: String, method: String = "GET", token: String = "") =
        Json.parseToJsonElement(request(path, method, token, 1024 * 1024).toString(Charsets.UTF_8)).jsonObject
    private fun request(path: String, method: String, token: String, limit: Int): ByteArray {
        check(configured)
        val connection = URL(origin.trimEnd('/') + path).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 15000; connection.readTimeout = 30000
        if (token.isNotEmpty()) connection.setRequestProperty("Authorization", "Bearer $token")
        try {
            check(connection.responseCode in 200..299) { "Pack service HTTP ${connection.responseCode}" }
            return connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= limit)
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        } finally { connection.disconnect() }
    }
}
