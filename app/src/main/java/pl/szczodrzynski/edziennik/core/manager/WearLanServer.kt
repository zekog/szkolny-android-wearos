/*
 * Copyright (c) Kuba Szczodrzyński 2024.
 *
 * Small local-network server used to hand the sync payload to the watch.
 * It is needed because the Wearable Data Layer does not replicate data
 * between this phone and some OEM watches (e.g. OnePlus/OPPO paired
 * through OHealth instead of the Google Wear companion).
 *
 * Protocol:
 *  - UDP broadcast on [DISCOVERY_PORT] with "SZKOLNY_DISCOVER"
 *    -> reply "SZKOLNY:<tcpPort>:<token>"
 *  - TCP request "GET <path>?t=<token>\n"
 *    -> "OK <length>\n" followed by <length> raw UTF-8 bytes, or "ERR ..."
 */

package pl.szczodrzynski.edziennik.core.manager

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.wear.data.WearCodec
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

class WearLanServer(private val app: App) {
    companion object {
        private const val TAG = "WearLanServer"
        const val DISCOVERY_PORT = 42890
        private const val MAGIC = "SZKOLNY_DISCOVER"
    }

    private val payloads = ConcurrentHashMap<String, String>()

    @Volatile
    private var bundleJson: String = "{}"

    @Volatile
    private var started = false
    private var tcpSocket: ServerSocket? = null
    private var udpSocket: DatagramSocket? = null

    val token: String = randomToken()

    @Volatile
    var tcpPort: Int = 0
        private set

    /**
     * Stores the newest JSON payload for a feature path (e.g. "/szkolny/timetable").
     */
    fun setPayload(path: String, json: String) {
        payloads[path] = json
        bundleJson = WearCodec.gson.toJson(payloads)
    }

    @Synchronized
    fun start() {
        if (started)
            return
        started = true
        try {
            val tcp = ServerSocket(0)
            tcpSocket = tcp
            tcpPort = tcp.localPort
            udpSocket = DatagramSocket(DISCOVERY_PORT)
        } catch (e: Exception) {
            Timber.e(e, "Cannot start Wear LAN server")
            started = false
            return
        }
        thread(name = "WearLanTcp") { tcpLoop() }
        thread(name = "WearLanUdp") { udpLoop() }
        Timber.i("Wear LAN server started on TCP port $tcpPort")
    }

    private fun tcpLoop() {
        val server = tcpSocket ?: return
        while (!server.isClosed) {
            try {
                val socket = server.accept()
                thread(name = "WearLanClient") { handleClient(socket) }
            } catch (e: Exception) {
                if (!server.isClosed)
                    Timber.w(e, "Wear LAN accept failed")
            }
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.use { client ->
                client.soTimeout = 5000
                val input = BufferedInputStream(client.getInputStream())
                val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
                val request = reader.readLine() ?: return

                val parts = request.trim().split(' ')
                if (parts.size < 2 || parts[0] != "GET") {
                    respond(client, "ERR bad request")
                    return
                }
                val target = parts[1]
                val queryIndex = target.indexOf('?')
                val path = if (queryIndex >= 0) target.substring(0, queryIndex) else target
                val query = if (queryIndex >= 0) target.substring(queryIndex + 1) else ""
                val requestToken = query.split('&')
                        .firstOrNull { it.startsWith("t=") }
                        ?.substring(2)

                if (requestToken != token) {
                    respond(client, "ERR forbidden")
                    return
                }

                val bytes = when {
                    path == "/szkolny/ping" -> "pong".toByteArray(Charsets.UTF_8)
                    path == "/szkolny/bundle" -> bundleJson.toByteArray(Charsets.UTF_8)
                    else -> payloads[path]?.toByteArray(Charsets.UTF_8)
                }

                if (bytes == null) {
                    respond(client, "ERR not found")
                    return
                }

                val output = client.getOutputStream()
                output.write("OK ${bytes.size}\n".toByteArray(Charsets.US_ASCII))
                output.write(bytes)
                output.flush()
            }
        } catch (e: Exception) {
            Timber.w(e, "Wear LAN client failed")
        }
    }

    private fun respond(socket: Socket, line: String) {
        try {
            socket.getOutputStream().write("$line\n".toByteArray(Charsets.US_ASCII))
            socket.getOutputStream().flush()
        } catch (_: Exception) {
        }
    }

    private fun udpLoop() {
        val socket = udpSocket ?: return
        val buffer = ByteArray(64)
        while (!socket.isClosed) {
            try {
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)
                val text = String(packet.data, 0, packet.length, Charsets.UTF_8).trim()
                if (text == MAGIC) {
                    val reply = "SZKOLNY:$tcpPort:$token".toByteArray(Charsets.UTF_8)
                    socket.send(DatagramPacket(reply, reply.size, packet.address, packet.port))
                }
            } catch (e: Exception) {
                if (!socket.isClosed)
                    Timber.w(e, "Wear LAN discovery failed")
            }
        }
    }

    private fun randomToken(): String {
        val bytes = ByteArray(6)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
