/*
 * Copyright (c) Kuba Szczodrzyński 2024.
 */

package pl.szczodrzynski.edziennik.wear.util

import pl.szczodrzynski.edziennik.wear.data.WearLesson
import pl.szczodrzynski.edziennik.wear.data.WearTimetable

const val LESSON_NO_LESSONS = -1
const val LESSON_NORMAL = 0
const val LESSON_CANCELLED = 1
const val LESSON_CHANGE = 2
const val LESSON_SHIFTED_SOURCE = 3
const val LESSON_SHIFTED_TARGET = 4

val WearLesson.displayDate: Int?
    get() = if (type == LESSON_SHIFTED_SOURCE) oldDate else date ?: oldDate

val WearLesson.displayLessonNumber: Int?
    get() = if (type == LESSON_SHIFTED_SOURCE) oldLessonNumber else lessonNumber ?: oldLessonNumber

val WearLesson.displayStartTime: Int?
    get() = if (type == LESSON_SHIFTED_SOURCE) oldStartTime else startTime ?: oldStartTime

val WearLesson.displayEndTime: Int?
    get() = if (type == LESSON_SHIFTED_SOURCE) oldEndTime else endTime ?: oldEndTime

val WearLesson.displaySubject: String?
    get() = if (type == LESSON_SHIFTED_SOURCE) oldSubjectName else subjectName ?: oldSubjectName

val WearLesson.displayTeacher: String?
    get() = if (type == LESSON_SHIFTED_SOURCE) oldTeacherName else teacherName ?: oldTeacherName

val WearLesson.displayClassroom: String?
    get() = if (type == LESSON_SHIFTED_SOURCE) oldClassroom else classroom ?: oldClassroom

val WearLesson.isCancelled: Boolean
    get() = type == LESSON_CANCELLED || type == LESSON_SHIFTED_SOURCE

fun WearTimetable.lessonsForDate(date: Int): List<WearLesson> =
        lessons
                .filter { it.type != LESSON_NO_LESSONS && it.displayDate == date }
                .sortedBy { it.displayStartTime ?: 0 }

fun WearTimetable.datesWithLessons(): Set<Int> =
        lessons
                .filter { it.type != LESSON_NO_LESSONS }
                .mapNotNull { it.displayDate }
                .toSet()
