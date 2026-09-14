/*
 * Copyright (c) Kuba Szczodrzyński 2024.
 */

package pl.szczodrzynski.edziennik.wear.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.Text
import pl.szczodrzynski.edziennik.wear.data.WearAttendance
import pl.szczodrzynski.edziennik.wear.util.relativeLabel
import pl.szczodrzynski.edziennik.wear.util.stripHtml
import pl.szczodrzynski.edziennik.wear.util.toLocalDate
import java.time.LocalDate

@Composable
fun AttendanceScreen(
        attendance: List<WearAttendance>,
        onBack: () -> Unit,
) {
    val summary = remember(attendance) {
        AttendanceSummary(
                sorted = attendance.sortedWith(
                        compareByDescending<WearAttendance> { it.date }.thenByDescending { it.startTime ?: 0 }
                ),
                absent = attendance.count { it.baseType == 1 },
                excused = attendance.count { it.baseType == 2 },
                belated = attendance.count { it.baseType == 4 || it.baseType == 5 },
                present = attendance.count { it.baseType == 0 || it.baseType == 10 },
        )
    }
    val sorted = summary.sorted
    val absent = summary.absent
    val excused = summary.excused
    val belated = summary.belated
    val present = summary.present

    ScreenScaffold(onBack = onBack) {
        headerItem("Frekwencja")
        item {
            Text(
                    text = "Obecne: $present • Nieobecne: $absent\nUsprawiedliwione: $excused • Spóźnienia: $belated",
                    modifier = Modifier.fillMaxWidth(),
            )
        }
        if (sorted.isEmpty()) {
            emptyItem("Brak danych o frekwencji")
        } else {
            items(count = sorted.size, key = { index -> sorted[index].id }) { index ->
                val item = sorted[index]
                val date = item.date.toLocalDate()
                val details = buildList {
                    add(date.relativeLabel(LocalDate.now()))
                    item.subjectName?.takeIf { it.isNotBlank() }?.let { add(it) }
                    item.topic?.takeIf { it.isNotBlank() }?.let { add(it.stripHtml().take(60)) }
                }.joinToString(" • ")
                Chip(
                        onClick = {},
                        label = { Text(item.typeName.ifBlank { item.typeShort }) },
                        secondaryLabel = { Text(details) },
                        modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private data class AttendanceSummary(
        val sorted: List<WearAttendance>,
        val absent: Int,
        val excused: Int,
        val belated: Int,
        val present: Int,
)
