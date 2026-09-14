/*
 * Copyright (c) Kuba Szczodrzyński 2024.
 */

package pl.szczodrzynski.edziennik.wear.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.Text
import pl.szczodrzynski.edziennik.wear.ui.theme.WearTheme

@Composable
fun ThemeScreen(
        current: WearTheme,
        onBack: () -> Unit,
        onSelect: (WearTheme) -> Unit,
) {
    ScreenScaffold(onBack = onBack) {
        headerItem("Motyw")
        items(count = WearTheme.entries.size) { index ->
            val theme = WearTheme.entries[index]
            val label = if (theme == current) "✓ ${theme.label}" else theme.label
            Chip(
                    onClick = { onSelect(theme) },
                    label = { Text(label) },
                    modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
