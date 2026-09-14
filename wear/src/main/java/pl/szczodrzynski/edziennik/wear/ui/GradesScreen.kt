/*
 * Copyright (c) Kuba Szczodrzyński 2024.
 */

package pl.szczodrzynski.edziennik.wear.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import pl.szczodrzynski.edziennik.wear.data.WearGrade
import pl.szczodrzynski.edziennik.wear.util.formatDate

@Composable
fun GradesScreen(
        grades: List<WearGrade>,
        onBack: () -> Unit,
) {
    val grouped = remember(grades) {
        grades
                .groupBy { it.subjectLongName?.takeIf { name -> name.isNotBlank() } ?: it.subjectShortName ?: "Inne" }
                .toSortedMap()
    }

    ScreenScaffold(onBack = onBack) {
        if (grouped.isEmpty()) {
            emptyItem("Brak ocen")
        } else {
            grouped.forEach { (subject, subjectGrades) ->
                item {
                    val numeric = subjectGrades.filter { it.type == 0 && it.weight > 0f }
                    val average = if (numeric.isEmpty()) null else numeric.sumOf { it.value.toDouble() } / numeric.size
                    Text(
                            text = if (average == null) subject else "%s • śr. %.2f".format(subject, average),
                            style = MaterialTheme.typography.title3,
                            color = MaterialTheme.colors.primary,
                            modifier = Modifier.fillMaxWidth(),
                    )
                }
                items(count = subjectGrades.size, key = { index -> subjectGrades[index].id }) { index ->
                    val grade = subjectGrades[index]
                    val details = buildList {
                        grade.category?.takeIf { it.isNotBlank() }?.let { add(it) }
                        if (grade.weight > 0f)
                            add("waga ${trimNumber(grade.weight)}")
                        grade.teacherName?.takeIf { it.isNotBlank() }?.let { add(it) }
                        add(formatDate(grade.addedDate))
                    }.joinToString(" • ")
                    Chip(
                            onClick = {},
                            label = { Text(grade.name) },
                            secondaryLabel = { Text(details) },
                            modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private fun trimNumber(value: Float): String =
        if (value == value.toInt().toFloat()) value.toInt().toString() else "%.2f".format(value)
