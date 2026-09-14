/*
 * Copyright (c) Kuba Szczodrzyński 2024.
 *
 * Fetches the data prepared by the phone app and keeps a local JSON cache,
 * so the watch can display data offline.
 *
 * Two transports are supported:
 *  - local network (UDP discovery + TCP), which works on OEM watches where
 *    the Wearable Data Layer is not bridged;
 *  - the Wearable Data Layer, used as a fallback.
 */

package pl.szczodrzynski.edziennik.wear.data

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataItemBuffer
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.wear.BuildConfig
import pl.szczodrzynski.edziennik.wear.data.WearPaths.ANNOUNCEMENTS
import pl.szczodrzynski.edziennik.wear.data.WearPaths.ATTENDANCE
import pl.szczodrzynski.edziennik.wear.data.WearPaths.EVENTS
import pl.szczodrzynski.edziennik.wear.data.WearPaths.GRADES
import pl.szczodrzynski.edziennik.wear.data.WearPaths.LUCKY_NUMBERS
import pl.szczodrzynski.edziennik.wear.data.WearPaths.MESSAGES
import pl.szczodrzynski.edziennik.wear.data.WearPaths.META
import pl.szczodrzynski.edziennik.wear.data.WearPaths.NOTICES
import pl.szczodrzynski.edziennik.wear.data.WearPaths.TIMETABLE
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

class WearRepository(context: Context) {
    companion object {
        private const val TAG = "WearRepository"
        private const val PREFS = "wear_cache"
        private const val DISCOVERY_PORT = 42890
        private const val DISCOVERY_MAGIC = "SZKOLNY_DISCOVER"
        private const val BUNDLE = "/szkolny/bundle"

        /** Minimum time between automatic refreshes. */
        private const val AUTO_REFRESH_THROTTLE_MS = 30_000L
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_DATA_LAYER_WORKS = "data_layer_works"
        private val FEATURES = listOf(
                META, TIMETABLE, GRADES, EVENTS, ATTENDANCE,
                NOTICES, MESSAGES, ANNOUNCEMENTS, LUCKY_NUMBERS,
        )
    }

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _meta = MutableStateFlow<WearMeta?>(null)
    val meta: StateFlow<WearMeta?> = _meta.asStateFlow()

    private val _timetable = MutableStateFlow(WearTimetable())
    val timetable: StateFlow<WearTimetable> = _timetable.asStateFlow()

    private val _grades = MutableStateFlow<List<WearGrade>>(emptyList())
    val grades: StateFlow<List<WearGrade>> = _grades.asStateFlow()

    private val _events = MutableStateFlow<List<WearEvent>>(emptyList())
    val events: StateFlow<List<WearEvent>> = _events.asStateFlow()

    private val _attendance = MutableStateFlow<List<WearAttendance>>(emptyList())
    val attendance: StateFlow<List<WearAttendance>> = _attendance.asStateFlow()

    private val _notices = MutableStateFlow<List<WearNotice>>(emptyList())
    val notices: StateFlow<List<WearNotice>> = _notices.asStateFlow()

    private val _messages = MutableStateFlow<List<WearMessage>>(emptyList())
    val messages: StateFlow<List<WearMessage>> = _messages.asStateFlow()

    private val _announcements = MutableStateFlow<List<WearAnnouncement>>(emptyList())
    val announcements: StateFlow<List<WearAnnouncement>> = _announcements.asStateFlow()

    private val _luckyNumbers = MutableStateFlow<List<WearLuckyNumber>>(emptyList())
    val luckyNumbers: StateFlow<List<WearLuckyNumber>> = _luckyNumbers.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _lastSyncSource = MutableStateFlow("")
    val lastSyncSource: StateFlow<String> = _lastSyncSource.asStateFlow()

    /** Loads the cached payloads; call off the main thread. */
    fun loadCache() {
        _meta.value = WearCodec.fromJson(prefs.getString(META, null))
        _timetable.value = WearCodec.fromJson<WearTimetable>(prefs.getString(TIMETABLE, null)) ?: WearTimetable()
        _grades.value = WearCodec.fromJson<List<WearGrade>>(prefs.getString(GRADES, null)) ?: emptyList()
        _events.value = WearCodec.fromJson<List<WearEvent>>(prefs.getString(EVENTS, null)) ?: emptyList()
        _attendance.value = WearCodec.fromJson<List<WearAttendance>>(prefs.getString(ATTENDANCE, null)) ?: emptyList()
        _notices.value = WearCodec.fromJson<List<WearNotice>>(prefs.getString(NOTICES, null)) ?: emptyList()
        _messages.value = WearCodec.fromJson<List<WearMessage>>(prefs.getString(MESSAGES, null)) ?: emptyList()
        _announcements.value = WearCodec.fromJson<List<WearAnnouncement>>(prefs.getString(ANNOUNCEMENTS, null)) ?: emptyList()
        _luckyNumbers.value = WearCodec.fromJson<List<WearLuckyNumber>>(prefs.getString(LUCKY_NUMBERS, null)) ?: emptyList()
    }

    private val refreshLock = java.util.concurrent.atomic.AtomicBoolean(false)

    @Volatile
    private var lastSyncAt: Long = prefs.getLong(KEY_LAST_SYNC, 0L)

    suspend fun refresh(force: Boolean = false) {
        if (!force && System.currentTimeMillis() - lastSyncAt < AUTO_REFRESH_THROTTLE_MS)
            return
        if (!refreshLock.compareAndSet(false, true))
            return
        _refreshing.value = true
        try {
            val lanPayloads = fetchViaLan()
            if (!lanPayloads.isNullOrEmpty()) {
                Log.i(TAG, "LAN payload received: ${lanPayloads.keys}")
                applyPayloads(lanPayloads)
                _lastSyncSource.value = "LAN"
                markSynced()
            } else if (isDataLayerEnabled()) {
                val payloads = fetchViaDataLayer()
                if (payloads.isNotEmpty()) {
                    applyPayloads(payloads)
                    _lastSyncSource.value = "Data Layer"
                    markSynced()
                } else {
                    // remember that the Data Layer is unavailable to avoid
                    // powering the radio for nothing on every refresh
                    setDataLayerEnabled(false)
                    Log.i(TAG, "No data received; not touching cache")
                }
            } else {
                Log.i(TAG, "No data received; not touching cache")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cannot refresh", e)
        } finally {
            _refreshing.value = false
            refreshLock.set(false)
        }
    }

    private fun markSynced() {
        lastSyncAt = System.currentTimeMillis()
        prefs.edit().putLong(KEY_LAST_SYNC, lastSyncAt).apply()
    }

    private fun isDataLayerEnabled(): Boolean =
            prefs.getBoolean(KEY_DATA_LAYER_WORKS, true)

    private fun setDataLayerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DATA_LAYER_WORKS, enabled).apply()
    }

    private fun applyPayloads(payloads: Map<String, String>) {
        val editor = prefs.edit()

        payloads[META]?.let {
            editor.putString(META, it)
            WearCodec.fromJson<WearMeta>(it)?.let { meta -> _meta.value = meta }
        }
        payloads[TIMETABLE]?.let {
            editor.putString(TIMETABLE, it)
            WearCodec.fromJson<WearTimetable>(it)?.let { value -> _timetable.value = value }
        }
        payloads[GRADES]?.let {
            editor.putString(GRADES, it)
            _grades.value = WearCodec.fromJson<List<WearGrade>>(it) ?: emptyList()
        }
        payloads[EVENTS]?.let {
            editor.putString(EVENTS, it)
            _events.value = WearCodec.fromJson<List<WearEvent>>(it) ?: emptyList()
        }
        payloads[ATTENDANCE]?.let {
            editor.putString(ATTENDANCE, it)
            _attendance.value = WearCodec.fromJson<List<WearAttendance>>(it) ?: emptyList()
        }
        payloads[NOTICES]?.let {
            editor.putString(NOTICES, it)
            _notices.value = WearCodec.fromJson<List<WearNotice>>(it) ?: emptyList()
        }
        payloads[MESSAGES]?.let {
            editor.putString(MESSAGES, it)
            _messages.value = WearCodec.fromJson<List<WearMessage>>(it) ?: emptyList()
        }
        payloads[ANNOUNCEMENTS]?.let {
            editor.putString(ANNOUNCEMENTS, it)
            _announcements.value = WearCodec.fromJson<List<WearAnnouncement>>(it) ?: emptyList()
        }
        payloads[LUCKY_NUMBERS]?.let {
            editor.putString(LUCKY_NUMBERS, it)
            _luckyNumbers.value = WearCodec.fromJson<List<WearLuckyNumber>>(it) ?: emptyList()
        }

        editor.apply()
    }

    /* --------------------------------- local network --------------------------------- */

    private suspend fun fetchViaLan(): Map<String, String>? = withContext(Dispatchers.IO) {
        val server = discoverPhone() ?: run {
            Log.i(TAG, "LAN: phone not found")
            return@withContext null
        }
        Log.i(TAG, "LAN: phone found at ${server.first}:${server.second}")

        // prefer a single request with all features (saves radio wake-ups)
        val bundle = tcpGet(server.first, server.second, server.third, BUNDLE)
        if (bundle != null) {
            val payloads = WearCodec.fromJson<Map<String, String>>(bundle)
            if (!payloads.isNullOrEmpty())
                return@withContext payloads
        }

        // fallback: one request per feature (older phone builds)
        val result = HashMap<String, String>()
        for (feature in FEATURES) {
            val json = tcpGet(server.first, server.second, server.third, feature) ?: continue
            result[feature] = json
        }
        result
    }

    private fun discoverPhone(): Triple<String, Int, String>? {
        return try {
            DatagramSocket().use { socket ->
                socket.broadcast = true
                socket.soTimeout = 2000
                val magic = DISCOVERY_MAGIC.toByteArray(Charsets.UTF_8)
                socket.send(DatagramPacket(
                        magic, magic.size,
                        InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT
                ))
                val buffer = ByteArray(128)
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)
                val text = String(packet.data, 0, packet.length, Charsets.UTF_8).trim()
                if (!text.startsWith("SZKOLNY:"))
                    return null
                val parts = text.split(':')
                val port = parts.getOrNull(1)?.toIntOrNull() ?: return null
                val token = parts.getOrElse(2) { "" }
                val host = packet.address?.hostAddress ?: return null
                Triple(host, port, token)
            }
        } catch (e: Exception) {
            Log.i(TAG, "LAN discovery failed: ${e.message}")
            null
        }
    }

    private fun tcpGet(host: String, port: Int, token: String, path: String): String? {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 3000)
                socket.soTimeout = 6000
                val output = socket.getOutputStream()
                output.write("GET $path?t=$token\n".toByteArray(Charsets.UTF_8))
                output.flush()

                val input = BufferedInputStream(socket.getInputStream())
                val status = readAsciiLine(input) ?: return null
                if (!status.startsWith("OK "))
                    return null
                val length = status.substring(3).trim().toIntOrNull() ?: return null
                val bytes = ByteArray(length)
                var read = 0
                while (read < length) {
                    val count = input.read(bytes, read, length - read)
                    if (count < 0)
                        break
                    read += count
                }
                String(bytes, 0, read, Charsets.UTF_8)
            }
        } catch (e: Exception) {
            Log.w(TAG, "LAN GET $path failed: ${e.message}")
            null
        }
    }

    private fun readAsciiLine(input: InputStream): String? {
        val builder = StringBuilder()
        while (true) {
            val byte = input.read()
            if (byte < 0)
                return if (builder.isEmpty()) null else builder.toString()
            if (byte == '\n'.code)
                break
            if (byte != '\r'.code)
                builder.append(byte.toChar())
        }
        return builder.toString()
    }

    /* --------------------------------- -- data layer --------------------------------- */

    private suspend fun fetchViaDataLayer(): Map<String, String> = withContext(Dispatchers.IO) {
        Wearable.getNodeClient(appContext).connectedNodes
                .addOnSuccessListener { nodes ->
                    Log.i(TAG, "Connected nodes: ${nodes.map { "${it.displayName}(${it.id})" }}")
                }
                .addOnFailureListener { e -> Log.w(TAG, "Nodes query failed", e) }

        val items = fetchAllItems()
        val result = HashMap<String, String>()
        for (base in FEATURES) {
            readFeature(items, base)?.let { result[base] = it }
        }
        result
    }

    private fun fetchAllItems(): List<Pair<String, ByteArray>> {
        val dataClient = Wearable.getDataClient(appContext)
        val buffer: DataItemBuffer = Tasks.await(dataClient.dataItems, 8, TimeUnit.SECONDS)
        try {
            val items = buffer.map { item -> item.uri.path.orEmpty() to (item.data ?: ByteArray(0)) }
            Log.i(TAG, "Data items on watch (${items.size}): ${items.map { it.first }}")
            return items
        } finally {
            buffer.release()
        }
    }

    private fun readFeature(items: List<Pair<String, ByteArray>>, base: String): String? {
        val countItem = items.firstOrNull { it.first == "$base/count" } ?: return null
        val count = String(countItem.second, Charsets.UTF_8).toIntOrNull() ?: return null
        if (count <= 0)
            return ""
        val builder = StringBuilder()
        for (index in 0 until count) {
            val item = items.firstOrNull { it.first == "$base/$index" } ?: return null
            builder.append(String(item.second, Charsets.UTF_8))
        }
        return builder.toString()
    }

    val isDebug: Boolean
        get() = BuildConfig.DEBUG
}
