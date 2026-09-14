/*
 * Copyright (c) Kuba Szczodrzyński 2024.
 */

package pl.szczodrzynski.edziennik.wear.util

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.util.Date
import java.util.Locale

/** Converts the app's integer date format (yyyyMMdd) into a [LocalDate]. */
fun Int.toLocalDate(): LocalDate =
        LocalDate.of(this / 10000, (this / 100) % 100, this % 100)

/** Converts a [LocalDate] into the app's integer date format (yyyyMMdd). */
fun LocalDate.toAppDate(): Int = year * 10000 + monthValue * 100 + dayOfMonth

/** Converts the app's integer time format (HHmmss) into a [LocalTime]. */
fun Int.toLocalTime(): LocalTime =
        LocalTime.of(this / 10000, (this / 100) % 100)

/** Converts a [LocalTime] into the app's integer time format (HHmmss). */
fun LocalTime.toAppTime(): Int = hour * 10000 + minute * 100 + second

private val weekdays = arrayOf("pon", "wt", "śr", "czw", "pt", "sob", "niedz")
private val months = arrayOf(
        "stycznia", "lutego", "marca", "kwietnia", "maja", "czerwca",
        "lipca", "sierpnia", "września", "października", "listopada", "grudnia",
)

fun LocalDate.weekdayShort(): String = weekdays[dayOfWeek.value - 1]

fun LocalDate.dateLong(): String = "$dayOfMonth ${months[monthValue - 1]}"

fun LocalDate.dateShort(): String = "$dayOfMonth.${if (monthValue < 10) "0" else ""}$monthValue"

fun LocalDate.relativeLabel(today: LocalDate = LocalDate.now()): String = when (this) {
    today -> "Dziś"
    today.plusDays(1) -> "Jutro"
    today.minusDays(1) -> "Wczoraj"
    today.plusDays(2) -> "Pojutrze"
    today.minusDays(2) -> "Przedwczoraj"
    else -> "${weekdayShort()} ${dateShort()}"
}

/** Formats a time in HH:mm or an empty string. */
fun formatTime(value: Int?): String {
    if (value == null) return ""
    val local = value.toLocalTime()
    return "%02d:%02d".format(local.hour, local.minute)
}

/** Formats a time range in HH:mm-HH:mm. */
fun formatTimeRange(start: Int?, end: Int?): String {
    val from = formatTime(start)
    val to = formatTime(end)
    return when {
        from.isEmpty() -> to
        to.isEmpty() -> from
        else -> "$from-$to"
    }
}

// reused formatter - only ever used from the UI thread
private val shortDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

/** Formats an epoch-millis timestamp as dd.MM.yyyy (empty when non-positive). */
fun formatDate(millis: Long): String {
    if (millis <= 0L) return ""
    return shortDateFormat.format(Date(millis))
}

private val htmlTags = Regex("<[^>]*>")
private val htmlEntities = mapOf(
        "&nbsp;" to " ", "&amp;" to "&", "&lt;" to "<", "&gt;" to ">",
        "&quot;" to "\"", "&#39;" to "'", "&oacute;" to "ó", "&Oacute;" to "Ó",
        "&aacute;" to "á", "&Aacute;" to "Á", "&eacute;" to "é", "&Eacute;" to "É",
        "&iacute;" to "í", "&Iacute;" to "Í", "&oelig;" to "œ", "&le;" to "≤",
        "&ge;" to "≥", "&hellip;" to "…", "&ndash;" to "–", "&mdash;" to "—",
)

/** Removes HTML tags and decodes the most common entities. */
fun String.stripHtml(): String {
    var text = replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
    text = text.replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n")
    text = text.replace(htmlTags, "")
    htmlEntities.forEach { (entity, value) -> text = text.replace(entity, value) }
    return text.trim()
}
