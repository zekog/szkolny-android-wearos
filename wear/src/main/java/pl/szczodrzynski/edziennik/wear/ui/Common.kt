/*
 * Copyright (c) Kuba Szczodrzyński 2024.
 */

package pl.szczodrzynski.edziennik.wear.ui

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.ScalingLazyListScope
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.rememberScalingLazyListState
import kotlinx.coroutines.channels.Channel

@Composable
fun ScreenScaffold(
        onBack: (() -> Unit)? = null,
        content: ScalingLazyListScope.() -> Unit,
) {
    val listState = rememberScalingLazyListState()

    // accumulate rotary deltas and apply them in a single scroll per frame,
    // instead of launching a coroutine for every crown tick
    val scrollChannel = remember { Channel<Float>(Channel.UNLIMITED) }

    LaunchedEffect(listState) {
        for (first in scrollChannel) {
            var total = first
            while (true) {
                val next = scrollChannel.tryReceive().getOrNull() ?: break
                total += next
            }
            listState.scrollBy(total)
        }
    }

    DisposableEffect(listState) {
        WearRotary.register { pixels -> scrollChannel.trySend(pixels) }
        onDispose {
            WearRotary.clear()
            scrollChannel.close()
        }
    }

    Scaffold(
            positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
            timeText = { TimeText() },
    ) {
        ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 20.dp),
        ) {
            if (onBack != null) {
                item {
                    CompactChip(
                            onClick = onBack,
                            label = { Text("← Wstecz") },
                            modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            content()
        }
    }
}

fun ScalingLazyListScope.emptyItem(text: String) {
    item {
        Text(
                text = text,
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
        )
    }
}

fun ScalingLazyListScope.headerItem(text: String) {
    item {
        Text(
                text = text,
                style = MaterialTheme.typography.title3,
                color = MaterialTheme.colors.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
        )
    }
}

internal fun Int?.toComposeColor(fallback: Long = 0xFF3F51B5): Color =
        Color(this ?: fallback.toInt())
