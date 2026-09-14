/*
 * Copyright (c) Kuba Szczodrzyński 2024.
 *
 * Pushes the currently selected profile's data to the paired Wear OS device
 * through the Wearable Data Layer API.
 */

package pl.szczodrzynski.edziennik.core.manager

import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.events.ApiTaskAllFinishedEvent
import pl.szczodrzynski.edziennik.data.db.entity.Attendance
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.Notice
import pl.szczodrzynski.edziennik.data.db.full.LessonFull
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.wear.data.WearAnnouncement
import pl.szczodrzynski.edziennik.wear.data.WearAttendance
import pl.szczodrzynski.edziennik.wear.data.WearCodec
import pl.szczodrzynski.edziennik.wear.data.WearEvent
import pl.szczodrzynski.edziennik.wear.data.WearGrade
import pl.szczodrzynski.edziennik.wear.data.WearLesson
import pl.szczodrzynski.edziennik.wear.data.WearLessonRange
import pl.szczodrzynski.edziennik.wear.data.WearLuckyNumber
import pl.szczodrzynski.edziennik.wear.data.WearMessage
import pl.szczodrzynski.edziennik.wear.data.WearMeta
import pl.szczodrzynski.edziennik.wear.data.WearNotice
import pl.szczodrzynski.edziennik.wear.data.WearPaths
import pl.szczodrzynski.edziennik.wear.data.WearTimetable
import timber.log.Timber

class WearSyncManager(private val app: App) {

    val lanServer = WearLanServer(app)

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onApiTaskAllFinished(event: ApiTaskAllFinishedEvent) {
        syncNow()
    }

    fun syncNow() {
        try {
            val profileId = app.profileId
            if (profileId <= 0)
                return
            val profile = app.profile

            lanServer.start()

            val subjects = app.db.subjectDao().getAllNow(profileId).associateBy { it.id }

            Wearable.getNodeClient(app).connectedNodes
                    .addOnSuccessListener { nodes ->
                        Timber.i("Wear nodes: ${nodes.map { "${it.displayName}(${it.id})" }}")
                    }
                    .addOnFailureListener { e ->
                        Timber.e(e, "Wear nodes query failed")
                    }

            val meta = WearMeta(
                    version = WearPaths.VERSION,
                    sentAt = System.currentTimeMillis(),
                    profileId = profileId,
                    profileName = profile.name,
                    accountName = profile.accountOwnerName,
                    studentName = profile.studentNameLong,
                    schoolName = profile.subname.orEmpty(),
                    className = profile.studentClassName.orEmpty(),
            )
            put(WearPaths.META, WearCodec.toJson(meta))

            put(WearPaths.TIMETABLE, WearCodec.toJson(buildTimetable(profileId, subjects)))
            put(WearPaths.GRADES, WearCodec.toJson(buildGrades(profileId, subjects)))
            put(WearPaths.EVENTS, WearCodec.toJson(buildEvents(profileId)))
            put(WearPaths.ATTENDANCE, WearCodec.toJson(buildAttendance(profileId)))
            put(WearPaths.NOTICES, WearCodec.toJson(buildNotices(profileId)))
            put(WearPaths.MESSAGES, WearCodec.toJson(buildMessages(profileId)))
            put(WearPaths.ANNOUNCEMENTS, WearCodec.toJson(buildAnnouncements(profileId)))
            put(WearPaths.LUCKY_NUMBERS, WearCodec.toJson(buildLuckyNumbers(profileId)))

            Timber.i("Wear sync finished")
        } catch (e: Exception) {
            Timber.e(e, "Wear sync failed")
        }
    }

    /* ---------------------------------- features ---------------------------------- */

    private fun buildTimetable(profileId: Int, subjects: Map<Long, pl.szczodrzynski.edziennik.data.db.entity.Subject>): WearTimetable {
        val today = Date.getToday()
        val from = today.clone().stepForward(0, 0, -7)
        val to = today.clone().stepForward(0, 0, 28)

        val lessons = app.db.timetableDao().getAllNow(profileId)
                .asSequence()
                .filter { lesson ->
                    val display = if (lesson.type == Lesson.TYPE_SHIFTED_SOURCE) lesson.oldDate else lesson.date
                    display != null && display >= from && display <= to
                }
                .map { it.toWearLesson(subjects) }
                .toList()

        val ranges = app.db.lessonRangeDao().getAllNow(profileId).map {
            WearLessonRange(
                    lessonNumber = it.lessonNumber,
                    startTime = it.startTime.value,
                    endTime = it.endTime.value,
            )
        }

        return WearTimetable(
                lessons = lessons,
                ranges = ranges,
                generatedOn = today.value,
        )
    }

    private fun LessonFull.toWearLesson(subjects: Map<Long, pl.szczodrzynski.edziennik.data.db.entity.Subject>): WearLesson {
        val subject = subjectId?.let { subjects[it] }
        return WearLesson(
                id = id,
                type = type,
                date = date?.value,
                lessonNumber = lessonNumber,
                startTime = startTime?.value,
                endTime = endTime?.value,
                subjectId = subjectId,
                subjectName = subjectName,
                subjectShortName = subject?.shortName,
                subjectColor = subject?.color,
                teacherName = teacherName,
                classroom = classroom,
                teamName = teamName,
                oldDate = oldDate?.value,
                oldLessonNumber = oldLessonNumber,
                oldStartTime = oldStartTime?.value,
                oldEndTime = oldEndTime?.value,
                oldSubjectName = oldSubjectName,
                oldTeacherName = oldTeacherName,
                oldClassroom = oldClassroom,
        )
    }

    private fun buildGrades(profileId: Int, subjects: Map<Long, pl.szczodrzynski.edziennik.data.db.entity.Subject>): List<WearGrade> =
            app.db.gradeDao().getAllNow(profileId).map { grade ->
                val subject = subjects[grade.subjectId]
                WearGrade(
                        id = grade.id,
                        name = grade.name,
                        type = grade.type,
                        value = grade.value,
                        weight = grade.weight,
                        color = grade.color,
                        category = grade.category,
                        description = grade.description,
                        comment = grade.comment,
                        semester = grade.semester,
                        subjectId = grade.subjectId,
                        subjectLongName = grade.subjectLongName,
                        subjectShortName = grade.subjectShortName ?: subject?.shortName,
                        subjectColor = subject?.color,
                        teacherName = grade.teacherName,
                        addedDate = grade.addedDate,
                        valueMax = grade.valueMax,
                        classAverage = grade.classAverage,
                )
            }

    private fun buildEvents(profileId: Int): List<WearEvent> =
            app.db.eventDao().getAllNow(profileId).map { event ->
                WearEvent(
                        id = event.id,
                        date = event.date.value,
                        time = event.time?.value,
                        topic = event.topic.take(4000),
                        body = event.homeworkBody?.take(8000),
                        type = event.type,
                        typeName = event.typeName,
                        color = event.eventColor,
                        subjectName = event.subjectLongName,
                        teacherName = event.teacherName,
                        isDone = event.isDone,
                        attachments = event.attachmentNames?.take(20) ?: emptyList(),
                )
            }

    private fun buildAttendance(profileId: Int): List<WearAttendance> =
            app.db.attendanceDao().getAllNow(profileId).map { attendance ->
                WearAttendance(
                        id = attendance.id,
                        date = attendance.date.value,
                        startTime = attendance.startTime?.value,
                        lessonNumber = attendance.lessonNumber,
                        topic = attendance.lessonTopic,
                        baseType = attendance.baseType,
                        typeName = attendance.typeName,
                        typeShort = attendance.typeShort,
                        typeColor = attendance.typeColor,
                        isCounted = attendance.isCounted,
                        semester = attendance.semester,
                        subjectName = attendance.subjectLongName,
                )
            }

    private fun buildNotices(profileId: Int): List<WearNotice> =
            app.db.noticeDao().getAllNow(profileId).map { notice ->
                WearNotice(
                        id = notice.id,
                        type = notice.type,
                        semester = notice.semester,
                        text = notice.text.take(4000),
                        category = notice.category,
                        points = notice.points,
                        teacherName = notice.teacherName,
                        addedDate = notice.addedDate,
                )
            }

    private fun buildMessages(profileId: Int): List<WearMessage> =
            app.db.messageDao().getAllNow(profileId)
                    .asSequence()
                    .filter { it.type == Message.TYPE_RECEIVED || it.type == Message.TYPE_SENT }
                    .take(80)
                    .map { message ->
                        WearMessage(
                                id = message.id,
                                type = message.type,
                                subject = message.subject.take(500),
                                body = message.body?.take(8000),
                                senderName = message.senderName,
                                recipients = emptyList(),
                                addedDate = message.addedDate,
                                isStarred = message.isStarred,
                                attachments = message.attachmentNames?.take(20) ?: emptyList(),
                        )
                    }
                    .toList()

    private fun buildAnnouncements(profileId: Int): List<WearAnnouncement> =
            app.db.announcementDao().getAllNow(profileId).take(80).map { announcement ->
                WearAnnouncement(
                        id = announcement.id,
                        subject = announcement.subject.take(500),
                        text = announcement.text?.take(8000),
                        startDate = announcement.startDate?.value,
                        endDate = announcement.endDate?.value,
                        teacherName = announcement.teacherName,
                        addedDate = announcement.addedDate,
                )
            }

    private fun buildLuckyNumbers(profileId: Int): List<WearLuckyNumber> =
            app.db.luckyNumberDao().getAllNow(profileId).take(30).map {
                WearLuckyNumber(date = it.date.value, number = it.number)
            }

    /* ----------------------------------- transport ----------------------------------- */

    private fun put(base: String, json: String) {
        lanServer.setPayload(base, json)
        val dataClient = Wearable.getDataClient(app)
        val chunks = WearCodec.chunks(json)
        putItem(dataClient, "$base/count", chunks.size.toString().toByteArray())
        chunks.forEachIndexed { index, chunk ->
            putItem(dataClient, "$base/$index", chunk.toByteArray(Charsets.UTF_8))
        }
    }

    private fun putItem(dataClient: com.google.android.gms.wearable.DataClient, path: String, bytes: ByteArray) {
        dataClient.putDataItem(PutDataRequest.create(path).setData(bytes))
                .addOnSuccessListener {
                    Timber.i("Wear put OK: $path (${bytes.size} B)")
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Wear put FAILED: $path (${bytes.size} B)")
                }
    }
}
