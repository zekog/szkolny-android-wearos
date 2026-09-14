/*
 * Copyright (c) Kuba Szczodrzyński 2024.
 *
 * Shared JSON (de)serialization helper for the phone <-> Wear sync.
 */

package pl.szczodrzynski.edziennik.wear.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object WearCodec {
    val gson: Gson = Gson()

    fun toJson(value: Any?): String = gson.toJson(value)

    inline fun <reified T> fromJson(json: String?): T? {
        if (json.isNullOrEmpty()) return null
        return try {
            gson.fromJson(json, object : TypeToken<T>() {}.type)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Splits [json] into chunks that each fit within the Data Layer item limit,
     * counting the UTF-8 byte size (not the character count).
     */
    fun chunks(json: String): List<String> {
        if (json.isEmpty()) return emptyList()
        val result = ArrayList<String>((json.length / WearPaths.CHUNK_SIZE) + 1)
        val builder = StringBuilder()
        var bytes = 0
        for (char in json) {
            val charBytes = utf8Length(char)
            if (bytes + charBytes > WearPaths.CHUNK_SIZE) {
                result.add(builder.toString())
                builder.setLength(0)
                bytes = 0
            }
            builder.append(char)
            bytes += charBytes
        }
        if (builder.isNotEmpty())
            result.add(builder.toString())
        return result
    }

    private fun utf8Length(char: Char): Int = when {
        char.code < 0x80 -> 1
        char.code < 0x800 -> 2
        else -> 3
    }

    fun join(chunks: List<String>): String = chunks.joinToString(separator = "")
}
