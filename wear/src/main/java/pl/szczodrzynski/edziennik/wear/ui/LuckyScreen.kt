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
import pl.szczodrzynski.edziennik.wear.data.WearLuckyNumber
import pl.szczodrzynski.edziennik.wear.util.dateLong
import pl.szczodrzynski.edziennik.wear.util.relativeLabel
import pl.szczodrzynski.edziennik.wear.util.toLocalDate
import java.time.LocalDate

@Composable
fun LuckyScreen(
        luckyNumbers: List<WearLuckyNumber>,
        onBack: () -> Unit,
) {
    val sorted = remember(luckyNumbers) { luckyNumbers.sortedByDescending { it.date } }

    ScreenScaffold(onBack = onBack) {
        headerItem("Szczęśliwe liczby")
        if (sorted.isEmpty()) {
            emptyItem("Brak danych")
        } else {
            items(count = sorted.size, key = { index -> sorted[index].date }) { index ->
                val item = sorted[index]
                val date = item.date.toLocalDate()
                Chip(
                        onClick = {},
                        label = { Text(item.number.toString()) },
                        secondaryLabel = {
                            Text("${date.relativeLabel(LocalDate.now())} • ${date.dateLong()}")
                        },
                        modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
