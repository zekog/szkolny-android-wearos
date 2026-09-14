/*
 * Copyright (c) Kuba Szczodrzyński 2024.
 */

package pl.szczodrzynski.edziennik.wear.ui

import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.MaterialTheme
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.wear.WearApp
import pl.szczodrzynski.edziennik.wear.data.WearRepository
import pl.szczodrzynski.edziennik.wear.data.WearSettings
import pl.szczodrzynski.edziennik.wear.ui.theme.WearTheme
import pl.szczodrzynski.edziennik.wear.ui.theme.applyTheme

sealed interface Screen {
    object Home : Screen
    object Timetable : Screen
    object Grades : Screen
    object Events : Screen
    object Attendance : Screen
    object Notices : Screen
    object Messages : Screen
    data class MessageDetail(val id: Long) : Screen
    object Announcements : Screen
    data class AnnouncementDetail(val id: Long) : Screen
    object Lucky : Screen
    object Theme : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = (application as WearApp).repository
        setContent {
            val context = LocalContext.current
            var theme by remember { mutableStateOf(WearSettings.getTheme(context)) }

            MaterialTheme {
                MaterialTheme(colors = MaterialTheme.colors.applyTheme(theme)) {
                    SzkolnyWearApp(
                            repository = repository,
                            theme = theme,
                            onThemeChange = {
                                theme = it
                                WearSettings.setTheme(context, it)
                            },
                    )
                }
            }
        }
        lifecycleScope.launch {
            repository.refresh()
        }
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_SCROLL) {
            val scroll = ev.getAxisValue(MotionEvent.AXIS_SCROLL)
            if (scroll != 0f) {
                WearRotary.onRotate(scroll)
                return true
            }
        }
        return super.dispatchGenericMotionEvent(ev)
    }
}

@Composable
private fun SzkolnyWearApp(
        repository: WearRepository,
        theme: WearTheme,
        onThemeChange: (WearTheme) -> Unit,
) {
    val meta by repository.meta.collectAsState()
    val timetable by repository.timetable.collectAsState()
    val grades by repository.grades.collectAsState()
    val events by repository.events.collectAsState()
    val attendance by repository.attendance.collectAsState()
    val notices by repository.notices.collectAsState()
    val messages by repository.messages.collectAsState()
    val announcements by repository.announcements.collectAsState()
    val luckyNumbers by repository.luckyNumbers.collectAsState()
    val refreshing by repository.refreshing.collectAsState()

    val backStack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val scope = rememberCoroutineScope()

    fun push(screen: Screen) = backStack.add(screen)
    fun pop() {
        if (backStack.size > 1)
            backStack.removeAt(backStack.lastIndex)
    }

    BackHandler(enabled = backStack.size > 1) { pop() }

    when (val screen = backStack.last()) {
        is Screen.Home -> HomeScreen(
                meta = meta,
                timetable = timetable,
                luckyNumbers = luckyNumbers,
                refreshing = refreshing,
                onOpen = ::push,
                onRefresh = { scope.launch { repository.refresh(force = true) } },
        )

        is Screen.Timetable -> TimetableScreen(
                timetable = timetable,
                onBack = ::pop,
        )

        is Screen.Grades -> GradesScreen(
                grades = grades,
                onBack = ::pop,
        )

        is Screen.Events -> EventsScreen(
                events = events,
                onBack = ::pop,
        )

        is Screen.Attendance -> AttendanceScreen(
                attendance = attendance,
                onBack = ::pop,
        )

        is Screen.Notices -> NoticesScreen(
                notices = notices,
                onBack = ::pop,
        )

        is Screen.Messages -> MessagesScreen(
                messages = messages,
                onBack = ::pop,
                onOpen = { push(Screen.MessageDetail(it)) },
        )

        is Screen.MessageDetail -> MessageDetailScreen(
                message = messages.firstOrNull { it.id == screen.id },
                onBack = ::pop,
        )

        is Screen.Announcements -> AnnouncementsScreen(
                announcements = announcements,
                onBack = ::pop,
                onOpen = { push(Screen.AnnouncementDetail(it)) },
        )

        is Screen.AnnouncementDetail -> AnnouncementDetailScreen(
                announcement = announcements.firstOrNull { it.id == screen.id },
                onBack = ::pop,
        )

        is Screen.Lucky -> LuckyScreen(
                luckyNumbers = luckyNumbers,
                onBack = ::pop,
        )

        is Screen.Theme -> ThemeScreen(
                current = theme,
                onBack = ::pop,
                onSelect = onThemeChange,
        )
    }
}
