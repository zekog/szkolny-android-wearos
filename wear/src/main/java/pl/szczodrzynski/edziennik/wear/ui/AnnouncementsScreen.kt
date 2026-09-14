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
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import pl.szczodrzynski.edziennik.wear.data.WearAnnouncement
import pl.szczodrzynski.edziennik.wear.util.stripHtml

@Composable
fun AnnouncementsScreen(
        announcements: List<WearAnnouncement>,
        onBack: () -> Unit,
        onOpen: (Long) -> Unit,
) {
    val sorted = remember(announcements) { announcements.sortedByDescending { it.addedDate } }

    ScreenScaffold(onBack = onBack) {
        headerItem("Ogłoszenia")
        if (sorted.isEmpty()) {
            emptyItem("Brak ogłoszeń")
        } else {
            items(count = sorted.size, key = { index -> sorted[index].id }) { index ->
                val announcement = sorted[index]
                Chip(
                        onClick = { onOpen(announcement.id) },
                        label = { Text(announcement.subject.stripHtml().take(80)) },
                        secondaryLabel = {
                            announcement.teacherName?.takeIf { it.isNotBlank() }?.let { Text(it) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun AnnouncementDetailScreen(
        announcement: WearAnnouncement?,
        onBack: () -> Unit,
) {
    ScreenScaffold(onBack = onBack) {
        if (announcement == null) {
            emptyItem("Nie znaleziono ogłoszenia")
            return@ScreenScaffold
        }
        item {
            Text(
                    text = announcement.subject.stripHtml(),
                    style = MaterialTheme.typography.title2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
            )
        }
        announcement.teacherName?.takeIf { it.isNotBlank() }?.let { teacher ->
            item {
                Text(
                        text = teacher,
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            Text(
                    text = announcement.text?.stripHtml()?.take(4000) ?: "(brak treści)",
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
