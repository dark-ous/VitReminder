package com.vitreminder.app

import com.vitreminder.app.data.model.ClassSession
import com.vitreminder.app.data.model.Timetable
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ScheduleCalculationTest {

    @Test
    fun testConsecutiveLabMerging() {
        // Two consecutive 1-hour PE lab slots on Monday: 08:00-09:00 and 09:00-10:00
        val labSlot1 = ClassSession(
            id = "Mon_08_PE_B3",
            dayOfWeek = Calendar.MONDAY,
            dayName = "Monday",
            slotTime = "08:00-09:00",
            startTime = "08:00",
            endTime = "09:00",
            startMinute = 480,
            endMinute = 540,
            subjectCode = "ES26104",
            subjectName = "Programming for Engineers",
            loadType = "Lab",
            batch = "B3",
            isCombined = false,
            classroom = "1114",
            facultyCode = "GKK",
            facultyName = "Gauri Kaluram Kharat"
        )
        val labSlot2 = ClassSession(
            id = "Mon_09_PE_B3",
            dayOfWeek = Calendar.MONDAY,
            dayName = "Monday",
            slotTime = "09:00-10:00",
            startTime = "09:00",
            endTime = "10:00",
            startMinute = 540,
            endMinute = 600,
            subjectCode = "ES26104",
            subjectName = "Programming for Engineers",
            loadType = "Lab",
            batch = "B3",
            isCombined = false,
            classroom = "1114",
            facultyCode = "GKK",
            facultyName = "Gauri Kaluram Kharat"
        )

        val timetable = Timetable(
            division = "FY CS-H",
            allSessions = listOf(labSlot1, labSlot2)
        )

        val filtered = timetable.getFilteredSessions("B3")
        assertEquals(1, filtered.size)
        val merged = filtered[0]
        assertEquals("08:00", merged.startTime)
        assertEquals("10:00", merged.endTime)
        assertEquals(120, merged.durationMinutes)
        assertEquals("1114", merged.classroom)
    }

    @Test
    fun testBatchIsolation() {
        // B1, B2, B3 parallel labs, and 1 combined theory
        val b1Lab = ClassSession(
            id = "s1", dayOfWeek = Calendar.MONDAY, dayName = "Monday",
            slotTime = "08:00-09:00", startTime = "08:00", endTime = "09:00",
            startMinute = 480, endMinute = 540, subjectCode = "ES26105",
            subjectName = "Basics of Engineering", loadType = "Lab", batch = "B1",
            isCombined = false, classroom = "1220-A", facultyCode = "SVP", facultyName = "SVP"
        )
        val b3Lab = ClassSession(
            id = "s2", dayOfWeek = Calendar.MONDAY, dayName = "Monday",
            slotTime = "08:00-09:00", startTime = "08:00", endTime = "09:00",
            startMinute = 480, endMinute = 540, subjectCode = "ES26104",
            subjectName = "Programming for Engineers", loadType = "Lab", batch = "B3",
            isCombined = false, classroom = "1114", facultyCode = "GKK", facultyName = "GKK"
        )
        val combinedTheory = ClassSession(
            id = "s3", dayOfWeek = Calendar.MONDAY, dayName = "Monday",
            slotTime = "10:00-11:00", startTime = "10:00", endTime = "11:00",
            startMinute = 600, endMinute = 660, subjectCode = "ES26101",
            subjectName = "Linear Algebra", loadType = "Theory", batch = null,
            isCombined = true, classroom = "1102", facultyCode = "PAK", facultyName = "PAK"
        )

        val timetable = Timetable(
            division = "FY CS-H",
            allSessions = listOf(b1Lab, b3Lab, combinedTheory)
        )

        val b3Sessions = timetable.getFilteredSessions("B3")
        assertEquals(2, b3Sessions.size)
        assertTrue(b3Sessions.any { it.classroom == "1114" })
        assertTrue(b3Sessions.any { it.classroom == "1102" })
        assertFalse(b3Sessions.any { it.classroom == "1220-A" }) // B1 should be excluded
    }

    @Test
    fun testBreakBetweenClasses() {
        val class1 = ClassSession(
            id = "c1", dayOfWeek = Calendar.WEDNESDAY, dayName = "Wednesday",
            slotTime = "11:00-12:00", startTime = "11:00", endTime = "12:00",
            startMinute = 660, endMinute = 720, subjectCode = "ES26109",
            subjectName = "Student Activity", loadType = "Theory", batch = null,
            isCombined = true, classroom = "1103", facultyCode = "VAP", facultyName = "VAP"
        )
        val class2 = ClassSession(
            id = "c2", dayOfWeek = Calendar.WEDNESDAY, dayName = "Wednesday",
            slotTime = "13:00-14:00", startTime = "13:00", endTime = "14:00",
            startMinute = 780, endMinute = 840, subjectCode = "ES26107",
            subjectName = "Indian Knowledge System", loadType = "Theory", batch = null,
            isCombined = true, classroom = "1001", facultyCode = "VSN", facultyName = "VSN"
        )

        val timetable = Timetable(
            division = "FY CS-H",
            allSessions = listOf(class1, class2)
        )

        val breaks = timetable.getBreaksForDay(Calendar.WEDNESDAY, listOf(class1, class2))
        assertEquals(1, breaks.size)
        val lunchBreak = breaks[0]
        assertEquals("12:00", lunchBreak.startTime)
        assertEquals("13:00", lunchBreak.endTime)
        assertEquals(60, lunchBreak.durationMinutes)
        assertEquals("1 hour", lunchBreak.formattedDuration)
    }
}
