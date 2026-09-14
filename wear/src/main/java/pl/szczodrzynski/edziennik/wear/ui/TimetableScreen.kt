/*
 * Copyright (c) Kuba Szczodrzyński 2024.
 */

package pl.szczodrzynski.edziennik.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import pl.szczodrzynski.edziennik.wear.data.WearTimetable
import pl.szczodrzynski.edziennik.wear.util.LESSON_CHANGE
import pl.szczodrzynski.edziennik.wear.util.LESSON_SHIFTED_SOURCE
import pl.szczodrzynski.edziennik.wear.util.LESSON_SHIFTED_TARGET
import pl.szczodrzynski.edziennik.wear.util.displayClassroom
import pl.szczodrzynski.edziennik.wear.util.displayLessonNumber
import pl.szczodrzynski.edziennik.wear.util.displayStartTime
import pl.szczodrzynski.edziennik.wear.util.displaySubject
import pl.szczodrzynski.edziennik.wear.util.displayTeacher
import pl.szczodrzynski.edziennik.wear.util.formatTime
import pl.szczodrzynski.edziennik.wear.util.lessonsForDate
import pl.szczodrzynski.edziennik.wear.util.relativeLabel
import pl.szczodrzynski.edziennik.wear.util.toAppDate
import pl.szczodrzynski.edziennik.wear.util.weekdayShort
import java.time.LocalDate

@Composable
fun TimetableScreen(
        timetable: WearTimetable,
        onBack: () -> Unit,
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val appDate = selectedDate.toAppDate()
    val lessons = remember(timetable, appDate) { timetable.lessonsForDate(appDate) }

    ScreenScaffold(onBack = onBack) {
        item {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                CompactChip(
                        onClick = { selectedDate = selectedDate.minusDays(1) },
                        label = { Text("◀") },
                )
                CompactChip(
                        onClick = { selectedDate = LocalDate.now() },
                        label = { Text("Dziś") },
                )
                CompactChip(
                        onClick = { selectedDate = selectedDate.plusDays(1) },
                        label = { Text("▶") },
                )
            }
        }

        item {
            Text(
                    text = "${selectedDate.weekdayShort()}, ${selectedDate.relativeLabel()}",
                    style = MaterialTheme.typography.title2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
            )
        }

        if (lessons.isEmpty()) {
            emptyItem("Brak lekcji tego dnia")
        } else {
            items(count = lessons.size, key = { index -> lessons[index].id }) { index ->
                val lesson = lessons[index]
                val number = lesson.displayLessonNumber
                val annotation = when (lesson.type) {
                    LESSON_CHANGE -> "Zmiana"
                    LESSON_SHIFTED_TARGET -> "Przeniesiona"
                    LESSON_SHIFTED_SOURCE -> "Przeniesiona"
                    else -> null
                }
                val details = buildList {
                    if (number != null)
                        add("$number.")
                    val time = formatTime(lesson.displayStartTime)
                    if (time.isNotEmpty())
                        add(time)
                    lesson.displayClassroom?.takeIf { it.isNotBlank() }?.let { add("s. $it") }
                    lesson.displayTeacher?.takeIf { it.isNotBlank() }?.let { add(it) }
                    annotation?.let { add(it) }
                }.joinToString(" • ")

                Chip(
                        onClick = {},
                        label = { Text(lesson.displaySubject ?: "Lekcja") },
                        secondaryLabel = { Text(details) },
                        modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
