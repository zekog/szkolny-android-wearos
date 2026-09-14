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
import pl.szczodrzynski.edziennik.wear.data.WearEvent
import pl.szczodrzynski.edziennik.wear.util.formatTime
import pl.szczodrzynski.edziennik.wear.util.relativeLabel
import pl.szczodrzynski.edziennik.wear.util.stripHtml
import pl.szczodrzynski.edziennik.wear.util.toLocalDate
import java.time.LocalDate

@Composable
fun EventsScreen(
        events: List<WearEvent>,
        onBack: () -> Unit,
) {
    val sorted = remember(events) {
        events.sortedWith(compareBy<WearEvent>({ it.date }, { it.time ?: 0 }))
    }

    ScreenScaffold(onBack = onBack) {
        headerItem("Zadania i wydarzenia")
        if (sorted.isEmpty()) {
            emptyItem("Brak zadań i wydarzeń")
        } else {
            items(count = sorted.size, key = { index -> sorted[index].id }) { index ->
                val event = sorted[index]
                val date = event.date.toLocalDate()
                val details = buildList {
                    add(date.relativeLabel(LocalDate.now()))
                    val time = formatTime(event.time)
                    if (time.isNotEmpty())
                        add(time)
                    event.subjectName?.takeIf { it.isNotBlank() }?.let { add(it) }
                    event.typeName?.takeIf { it.isNotBlank() && it != event.subjectName }?.let { add(it) }
                }.joinToString(" • ")
                Chip(
                        onClick = {},
                        label = { Text(event.topic.stripHtml().take(120)) },
                        secondaryLabel = { Text(details) },
                        modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
