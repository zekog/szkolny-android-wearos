/*
 * Copyright (c) Kuba Szczodrzyński 2024.
 *
 * Shared data model for the phone <-> Wear OS companion sync.
 */

package pl.szczodrzynski.edziennik.wear.data

/**
 * Data Layer paths used to transfer feature payloads.
 *
 * Every feature is stored as a set of DataClient items:
 *  - `<base>/count` - a plain-text integer with the chunk count
 *  - `<base>/<n>`   - consecutive UTF-8 JSON chunks
 *
 * This keeps each item well below the 100 kB Data Layer limit.
 */
object WearPaths {
    const val VERSION = 1
    const val BASE = "/szkolny"

    const val META = "$BASE/meta"
    const val TIMETABLE = "$BASE/timetable"
    const val GRADES = "$BASE/grades"
    const val EVENTS = "$BASE/events"
    const val ATTENDANCE = "$BASE/attendance"
    const val NOTICES = "$BASE/notices"
    const val MESSAGES = "$BASE/messages"
    const val ANNOUNCEMENTS = "$BASE/announcements"
    const val LUCKY_NUMBERS = "$BASE/lucky"

    val ALL = listOf(
        META, TIMETABLE, GRADES, EVENTS, ATTENDANCE,
        NOTICES, MESSAGES, ANNOUNCEMENTS, LUCKY_NUMBERS,
    )

    /** Maximum size of a single JSON chunk (Data Layer hard limit is 100 kB). */
    const val CHUNK_SIZE = 80_000
}

/** Payload envelope with the source profile information. */
data class WearMeta(
    val version: Int = WearPaths.VERSION,
    val sentAt: Long = 0L,
    val profileId: Int = -1,
    val profileName: String = "",
    val accountName: String = "",
    val studentName: String = "",
    val schoolName: String = "",
    val className: String = "",
)

data class WearLesson(
    val id: Long,
    val type: Int,
    val date: Int?,
    val lessonNumber: Int?,
    val startTime: Int?,
    val endTime: Int?,
    val subjectId: Long?,
    val subjectName: String?,
    val subjectShortName: String?,
    val subjectColor: Int?,
    val teacherName: String?,
    val classroom: String?,
    val teamName: String?,
    val oldDate: Int?,
    val oldLessonNumber: Int?,
    val oldStartTime: Int?,
    val oldEndTime: Int?,
    val oldSubjectName: String?,
    val oldTeacherName: String?,
    val oldClassroom: String?,
)

data class WearLessonRange(
    val lessonNumber: Int,
    val startTime: Int,
    val endTime: Int,
)

data class WearTimetable(
    val lessons: List<WearLesson> = emptyList(),
    val ranges: List<WearLessonRange> = emptyList(),
    val generatedOn: Int = 0,
)

data class WearGrade(
    val id: Long,
    val name: String,
    val type: Int,
    val value: Float,
    val weight: Float,
    val color: Int,
    val category: String?,
    val description: String?,
    val comment: String?,
    val semester: Int,
    val subjectId: Long,
    val subjectLongName: String?,
    val subjectShortName: String?,
    val subjectColor: Int?,
    val teacherName: String?,
    val addedDate: Long,
    val valueMax: Float?,
    val classAverage: Float?,
)

data class WearEvent(
    val id: Long,
    val date: Int,
    val time: Int?,
    val topic: String,
    val body: String?,
    val type: Long,
    val typeName: String?,
    val color: Int,
    val subjectName: String?,
    val teacherName: String?,
    val isDone: Boolean,
    val attachments: List<String> = emptyList(),
)

data class WearAttendance(
    val id: Long,
    val date: Int,
    val startTime: Int?,
    val lessonNumber: Int?,
    val topic: String?,
    val baseType: Int,
    val typeName: String,
    val typeShort: String,
    val typeColor: Int?,
    val isCounted: Boolean,
    val semester: Int,
    val subjectName: String?,
)

data class WearNotice(
    val id: Long,
    val type: Int,
    val semester: Int,
    val text: String,
    val category: String?,
    val points: Float?,
    val teacherName: String?,
    val addedDate: Long,
)

data class WearMessage(
    val id: Long,
    val type: Int,
    val subject: String,
    val body: String?,
    val senderName: String?,
    val recipients: List<String> = emptyList(),
    val addedDate: Long,
    val isStarred: Boolean,
    val attachments: List<String> = emptyList(),
)

data class WearAnnouncement(
    val id: Long,
    val subject: String,
    val text: String?,
    val startDate: Int?,
    val endDate: Int?,
    val teacherName: String?,
    val addedDate: Long,
)

data class WearLuckyNumber(
    val date: Int,
    val number: Int,
)
