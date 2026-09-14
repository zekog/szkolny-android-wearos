/*
 * Copyright (c) Kuba Szczodrzyński 2024.
 */

package pl.szczodrzynski.edziennik.wear.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import pl.szczodrzynski.edziennik.wear.data.WearLuckyNumber
import pl.szczodrzynski.edziennik.wear.data.WearMeta
import pl.szczodrzynski.edziennik.wear.data.WearTimetable
import pl.szczodrzynski.edziennik.wear.util.displayStartTime
import pl.szczodrzynski.edziennik.wear.util.displaySubject
import pl.szczodrzynski.edziennik.wear.util.displayTeacher
import pl.szczodrzynski.edziennik.wear.util.formatTime
import pl.szczodrzynski.edziennik.wear.util.lessonsForDate
import pl.szczodrzynski.edziennik.wear.util.toAppDate
import java.time.LocalDate

@Composable
fun HomeScreen(
        meta: WearMeta?,
        timetable: WearTimetable,
        luckyNumbers: List<WearLuckyNumber>,
        refreshing: Boolean,
        onOpen: (Screen) -> Unit,
        onRefresh: () -> Unit,
) {
    val today = LocalDate.now().toAppDate()
    val todayLessons = remember(timetable) { timetable.lessonsForDate(today) }
    val lucky = remember(luckyNumbers, today) {
        luckyNumbers.firstOrNull { it.date == today } ?: luckyNumbers.firstOrNull()
    }

    ScreenScaffold {
        item {
            Text(
                    text = meta?.studentName?.takeIf { it.isNotBlank() }
                            ?: meta?.profileName?.takeIf { it.isNotBlank() }
                            ?: "Szkolny",
                    style = MaterialTheme.typography.title2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
            )
        }
        meta?.schoolName?.takeIf { it.isNotBlank() }?.let { school ->
            item {
                Text(
                        text = school,
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        meta?.className?.takeIf { it.isNotBlank() }?.let { className ->
            item {
                Text(
                        text = className,
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        lucky?.let {
            item {
                Text(
                        text = "Szczęśliwa liczba: ${it.number}",
                        style = MaterialTheme.typography.body1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            CompactChip(
                    onClick = onRefresh,
                    label = { Text(if (refreshing) "Synchronizacja…" else "Odśwież") },
                    modifier = Modifier.fillMaxWidth(),
            )
        }

        headerItem("Dziś w planie")
        if (todayLessons.isEmpty()) {
            emptyItem("Brak lekcji lub brak danych")
        } else {
            items(count = todayLessons.size, key = { index -> todayLessons[index].id }) { index ->
                val lesson = todayLessons[index]
                Chip(
                        onClick = { onOpen(Screen.Timetable) },
                        label = { Text(lesson.displaySubject ?: "Lekcja") },
                        secondaryLabel = {
                            val time = formatTime(lesson.displayStartTime)
                            val teacher = lesson.displayTeacher?.takeIf { it.isNotBlank() }
                            Text(listOfNotNull(time.takeIf { it.isNotEmpty() }, teacher).joinToString(" • "))
                        },
                        modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        headerItem("Menu")
        menuEntries.forEach { (label, screen) ->
            item {
                Chip(
                        onClick = { onOpen(screen) },
                        label = { Text(label) },
                        modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private val menuEntries = listOf(
        "Plan lekcji" to Screen.Timetable,
        "Oceny" to Screen.Grades,
        "Zadania i wydarzenia" to Screen.Events,
        "Frekwencja" to Screen.Attendance,
        "Uwagi" to Screen.Notices,
        "Wiadomości" to Screen.Messages,
        "Ogłoszenia" to Screen.Announcements,
        "Szczęśliwe liczby" to Screen.Lucky,
        "Motyw" to Screen.Theme,
)
