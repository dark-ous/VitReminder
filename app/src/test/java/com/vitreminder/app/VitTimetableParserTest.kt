package com.vitreminder.app

import com.vitreminder.app.data.model.Timetable
import com.vitreminder.app.parser.VitTimetableParser
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.util.Calendar

class VitTimetableParserTest {

    private lateinit var parser: VitTimetableParser
    private lateinit var timetable: Timetable

    @Before
    fun setUp() {
        parser = VitTimetableParser(JvmPdfExtractor())
        val stream = javaClass.classLoader?.getResourceAsStream("sample_timetable.pdf")
            ?: listOf(
                File("sample_timetable.pdf"),
                File("/workspace/sample_timetable.pdf"),
                File("/home/retr0/VitReminder/sample_timetable.pdf")
            ).firstOrNull { it.exists() }?.let { FileInputStream(it) }
            ?: throw IllegalStateException("Could not find sample_timetable.pdf")
        timetable = parser.parse(stream)
    }

    @Test
    fun testMetadataExtraction() {
        println("Division: '${timetable.division}'")
        println("AcademicYear: '${timetable.academicYear}'")
        println("Semester: '${timetable.semester}'")
        println("WEF: '${timetable.wefDate}'")
        println("ToDate: '${timetable.toDate}'")
        assertEquals("FY CS-H", timetable.division)
        assertTrue("Academic Year should contain 2026", timetable.academicYear.contains("2026"))
        assertEquals("1", timetable.semester)
        assertEquals("15-Sep-2026", timetable.wefDate)
        assertEquals("10-Jan-2027", timetable.toDate)
    }

    @Test
    fun testFacultyDirectoryExtraction() {
        val facultyCodes = timetable.facultyList.map { it.code }
        assertTrue("Should contain GKK", facultyCodes.contains("GKK"))
        assertTrue("Should contain SVP", facultyCodes.contains("SVP"))
        assertTrue("Should contain PVJ", facultyCodes.contains("PVJ"))
        assertTrue("Should contain PAK", facultyCodes.contains("PAK"))

        val gkk = timetable.facultyList.find { it.code == "GKK" }
        assertNotNull(gkk)
        assertTrue("GKK full name should match", gkk!!.fullName.contains("Gauri Kaluram Kharat"))
    }

    @Test
    fun testAvailableBatches() {
        val batches = timetable.availableBatches
        println("Available batches found: $batches")
        assertTrue("Should contain B1", batches.contains("B1"))
        assertTrue("Should contain B2", batches.contains("B2"))
        assertTrue("Should contain B3", batches.contains("B3"))
    }

    @Test
    fun testBatchB3Filtering() {
        val b3Sessions = timetable.getFilteredSessions("B3")
        assertFalse("B3 should have classes", b3Sessions.isEmpty())
        println("Total B3 merged sessions: ${b3Sessions.size}")
        b3Sessions.forEach { session ->
            println("${session.dayName} ${session.slotTime}: ${session.subjectCode} - ${session.subjectName} (${session.loadType}) @ ${session.classroom} [${session.facultyName}]")
            if (!session.isCombined) {
                assertEquals("Should only include B3 or combined", "B3", session.batch)
            }
        }

        // Verify Monday schedule for B3
        val monSessions = b3Sessions.filter { it.dayOfWeek == Calendar.MONDAY }
        assertTrue("Monday should have sessions", monSessions.isNotEmpty())

        // Monday 08:00 should be PE Lab in 1114
        val morningLab = monSessions.find { it.startTime == "08:00" }
        assertNotNull("Monday 08:00 lab should exist", morningLab)
        assertEquals("1114", morningLab!!.classroom)
        assertEquals("Lab", morningLab.loadType)
    }

    @Test
    fun testBreakDetection() {
        val b3Sessions = timetable.getFilteredSessions("B3")
        // Wednesday has a lunch break around 12:00-13:00
        val wedBreaks = timetable.getBreaksForDay(Calendar.WEDNESDAY, b3Sessions)
        println("Wednesday breaks: $wedBreaks")
        val breakAt12 = wedBreaks.find { it.startTime == "12:00" || it.endTime == "13:00" }
        assertNotNull("Wednesday should have a break around 12:00", breakAt12)
        assertEquals(60, breakAt12!!.durationMinutes)
    }

    @Test
    fun testExportDefaultJson() {
        val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
        val json = gson.toJson(timetable)
        val file = File("/workspace/app/src/main/assets/default_timetable.json")
        file.parentFile?.mkdirs()
        file.writeText(json)
        println("Exported default_timetable.json to: ${file.absolutePath}, size: ${json.length}")
        assertTrue("JSON should have length > 100", json.length > 100)
    }
}
