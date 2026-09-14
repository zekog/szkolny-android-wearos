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
import pl.szczodrzynski.edziennik.wear.data.WearMessage
import pl.szczodrzynski.edziennik.wear.util.formatDate
import pl.szczodrzynski.edziennik.wear.util.stripHtml

@Composable
fun MessagesScreen(
        messages: List<WearMessage>,
        onBack: () -> Unit,
        onOpen: (Long) -> Unit,
) {
    val sorted = remember(messages) { messages.sortedByDescending { it.addedDate } }

    ScreenScaffold(onBack = onBack) {
        headerItem("Wiadomości")
        if (sorted.isEmpty()) {
            emptyItem("Brak wiadomości")
        } else {
            items(count = sorted.size, key = { index -> sorted[index].id }) { index ->
                val message = sorted[index]
                val sender = when {
                    message.type == 1 -> "Wysłana"
                    message.senderName?.isNotBlank() == true -> message.senderName
                    else -> "Odebrana"
                }
                Chip(
                        onClick = { onOpen(message.id) },
                        label = { Text(message.subject.stripHtml().take(80)) },
                        secondaryLabel = { Text("$sender • ${formatDate(message.addedDate)}") },
                        modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun MessageDetailScreen(
        message: WearMessage?,
        onBack: () -> Unit,
) {
    ScreenScaffold(onBack = onBack) {
        if (message == null) {
            emptyItem("Nie znaleziono wiadomości")
            return@ScreenScaffold
        }
        item {
            Text(
                    text = message.subject.stripHtml(),
                    style = MaterialTheme.typography.title2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            val sender = if (message.type == 1) "Wysłana" else message.senderName.orEmpty()
            Text(
                    text = listOf(sender, formatDate(message.addedDate))
                            .filter { it.isNotBlank() }
                            .joinToString(" • "),
                    style = MaterialTheme.typography.body2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Text(
                    text = message.body?.stripHtml()?.take(4000) ?: "(brak treści)",
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.fillMaxWidth(),
            )
        }
        if (message.attachments.isNotEmpty()) {
            headerItem("Załączniki")
            items(count = message.attachments.size) { index ->
                Chip(
                        onClick = {},
                        label = { Text(message.attachments[index]) },
                        modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
