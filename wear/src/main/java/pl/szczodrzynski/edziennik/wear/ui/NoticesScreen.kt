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
import pl.szczodrzynski.edziennik.wear.data.WearNotice
import pl.szczodrzynski.edziennik.wear.util.stripHtml

@Composable
fun NoticesScreen(
        notices: List<WearNotice>,
        onBack: () -> Unit,
) {
    val sorted = remember(notices) { notices.sortedByDescending { it.addedDate } }

    ScreenScaffold(onBack = onBack) {
        headerItem("Uwagi")
        if (sorted.isEmpty()) {
            emptyItem("Brak uwag")
        } else {
            items(count = sorted.size, key = { index -> sorted[index].id }) { index ->
                val notice = sorted[index]
                val typeLabel = when (notice.type) {
                    1 -> "Pochwała"
                    2 -> "Uwaga"
                    else -> "Neutralna"
                }
                val details = buildList {
                    add(typeLabel)
                    notice.category?.takeIf { it.isNotBlank() }?.let { add(it) }
                    notice.points?.let { add("$it pkt") }
                    notice.teacherName?.takeIf { it.isNotBlank() }?.let { add(it) }
                }.joinToString(" • ")
                Chip(
                        onClick = {},
                        label = { Text(notice.text.stripHtml().take(120)) },
                        secondaryLabel = { Text(details) },
                        modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
