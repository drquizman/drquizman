package com.drquizman.tv.net

import com.drquizman.core.i18n.LocalizedText

import com.drquizman.core.engine.GameEngine
import com.drquizman.core.model.GameSnapshot
import com.drquizman.core.protocol.ClientMsg
import com.drquizman.core.protocol.ProtocolJson
import com.drquizman.core.protocol.ServerMsg
import io.ktor.http.ContentType
import io.ktor.http.withCharset
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import android.content.res.AssetManager
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.NetworkInterface
import java.util.concurrent.ConcurrentHashMap

class HostServer(
    private val engine: GameEngine,
    private val main: CoroutineScope,
    private val indexHtml: String,
    private val assets: AssetManager,
) {
    private var ktor: ApplicationEngine? = null
    private val sockets = ConcurrentHashMap<String, DefaultWebSocketSession>()

    fun start() {
        ktor = embeddedServer(CIO, port = PORT, host = "0.0.0.0") {
            install(WebSockets)
            routing {
                get("/") {
                    call.respondText(indexHtml, ContentType.Text.Html.withCharset(Charsets.UTF_8))
                }
                get("/keepawake.mp4") {
                    val bytes = withContext(Dispatchers.IO) {
                        runCatching { assets.open("remote/keepawake.mp4").use { it.readBytes() } }.getOrNull()
                    }
                    if (bytes == null) {
                        call.respondText("no", status = HttpStatusCode.NotFound)
                    } else {
                        call.respondBytes(bytes, ContentType.Video.MP4)
                    }
                }
                get("/avatars/{file}") {
                    val file = call.parameters["file"].orEmpty()
                    if (!file.matches(Regex("""^[0-9]\.png$"""))) {
                        call.respondText("no", status = HttpStatusCode.NotFound)
                        return@get
                    }
                    val bytes = withContext(Dispatchers.IO) {
                        runCatching { assets.open("remote/avatars/$file").use { it.readBytes() } }.getOrNull()
                    }
                    if (bytes == null) {
                        call.respondText("no", status = HttpStatusCode.NotFound)
                    } else {
                        call.respondBytes(bytes, ContentType.Image.PNG)
                    }
                }
                webSocket("/ws") {
                    val ws = this
                    var playerId: String? = null
                    var left = false
                    try {
                        for (frame in incoming) {
                            if (frame !is Frame.Text) continue
                            val msg = runCatching {
                                ProtocolJson.decodeFromString(ClientMsg.serializer(), frame.readText())
                            }.getOrNull() ?: continue
                            when (msg) {
                                is ClientMsg.Hello -> {
                                    val result = withContext(Dispatchers.Main) {
                                        if (msg.pin != engine.pin) {
                                            Result.failure(IllegalArgumentException(LocalizedText.get("HostServer.001")))
                                        } else {
                                            val token = msg.resumeToken?.trim().orEmpty()
                                            if (token.isNotEmpty()) {
                                                val resumed = engine.resumePlayer(token)
                                                if (resumed.isSuccess) resumed else engine.addPlayer(msg.name)
                                            } else {
                                                engine.addPlayer(msg.name)
                                            }
                                        }
                                    }
                                    result.fold(
                                        onSuccess = { seat ->
                                            playerId = seat.player.id
                                            val old = sockets.put(seat.player.id, ws)
                                            if (old != null && old !== ws) {
                                                runCatching {
                                                    old.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "replaced"))
                                                }
                                            }
                                            ws.sendMsg(
                                                ServerMsg.Welcome(
                                                    seat.player.id,
                                                    seat.player.colorIndex,
                                                    seat.resumeToken,
                                                ),
                                            )
                                            ws.sendMsg(ServerMsg.Snapshot(withContext(Dispatchers.Main) { engine.snapshot() }))
                                        },
                                        onFailure = {
                                            ws.sendMsg(ServerMsg.Error("join", it.message ?: LocalizedText.get("HostServer.002")))
                                        },
                                    )
                                }
                                is ClientMsg.Ready -> withContext(Dispatchers.Main) {
                                    playerId?.let { engine.setReady(it, msg.ready) }
                                }
                                is ClientMsg.Answer -> withContext(Dispatchers.Main) {
                                    playerId?.let { engine.submitAnswer(it, msg.questionId, msg.index) }
                                }
                                is ClientMsg.PickTopic -> withContext(Dispatchers.Main) {
                                    playerId?.let { engine.pickTopic(it, msg.topic) }
                                }
                                is ClientMsg.Bet -> withContext(Dispatchers.Main) {
                                    playerId?.let { engine.placeBet(it, msg.amount) }
                                }
                                is ClientMsg.PickAvatar -> withContext(Dispatchers.Main) {
                                    playerId?.let { engine.setAvatar(it, msg.index) }
                                }
                                ClientMsg.Leave -> {
                                    val id = playerId
                                    left = true
                                    playerId = null
                                    if (id != null) {
                                        sockets.remove(id, ws)
                                        withContext(Dispatchers.Main) { engine.removePlayer(id) }
                                    }
                                    close(CloseReason(CloseReason.Codes.NORMAL, "leave"))
                                }
                                ClientMsg.Pause, ClientMsg.Resume -> withContext(Dispatchers.Main) {
                                    engine.togglePauseFromRemote()
                                }
                            }
                        }
                    } catch (_: ClosedReceiveChannelException) {
                    } finally {
                        val id = playerId
                        if (!left && id != null && sockets[id] === ws) {
                            sockets.remove(id, ws)
                            withContext(Dispatchers.Main) { engine.markDisconnected(id) }
                        }
                    }
                }
            }
        }.start(wait = false)
    }

    suspend fun broadcast(snapshot: GameSnapshot) {
        sockets.forEach { (_, ws) ->
            runCatching { ws.sendMsg(ServerMsg.Snapshot(snapshot)) }
        }
    }

    fun stop() {
        sockets.clear()
        ktor?.stop(300, 500)
        ktor = null
    }

    companion object {
        const val PORT = com.drquizman.core.net.Lan.PORT
    }
}

private suspend fun DefaultWebSocketSession.sendMsg(msg: ServerMsg) {
    outgoing.send(Frame.Text(ProtocolJson.encodeToString(ServerMsg.serializer(), msg)))
}

fun wifiSsid(context: android.content.Context): String? = runCatching {
    val app = context.applicationContext
    val names = mutableListOf<String?>()
    val wm = app.getSystemService(android.content.Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
    @Suppress("DEPRECATION")
    names += wm?.connectionInfo?.ssid
    if (android.os.Build.VERSION.SDK_INT >= 29) {
        val cm = app.getSystemService(android.net.ConnectivityManager::class.java)
        val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
        val info = caps?.transportInfo as? android.net.wifi.WifiInfo
        names += info?.ssid
    }
    names.mapNotNull { cleanSsid(it) }.firstOrNull()
}.getOrNull()

private fun cleanSsid(raw: String?): String? {
    val s = raw?.trim()?.trim('"')?.trim().orEmpty()
    if (s.isEmpty() || s.equals("<unknown ssid>", true) || s.equals("0x", true)) return null
    return s
}

fun localIpv4(): String? = runCatching {
    val found = mutableListOf<String>()
    val interfaces = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
    for (nic in interfaces) {
        if (!nic.isUp || nic.isLoopback) continue
        for (addr in nic.inetAddresses) {
            val host = addr.hostAddress ?: continue
            if (addr.isLoopbackAddress) continue
            if (host.contains(':')) continue
            if (host.startsWith("169.254")) continue
            found += host
        }
    }
    found.firstOrNull { !it.startsWith("10.0.2.") } ?: found.firstOrNull()
}.getOrNull()
