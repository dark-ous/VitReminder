package com.vitreminder.app

import com.vitreminder.app.data.model.CourseAttendance
import org.junit.Assert.*
import org.junit.Test

class CourseAttendanceTest {

    @Test
    fun testAttendance_perfectScore() {
        val course = CourseAttendance("CS1001", "Computer Networks", attendedClasses = 10, totalClasses = 10)
        assertEquals(100f, course.percentage, 0.01f)
        assertTrue(course.isSafe)
        // 10 - 7.5 = 2.5 / 0.75 = 3 bunkable classes
        assertEquals(3, course.bunkableClasses)
        assertEquals(0, course.requiredToCatchUp)
    }

    @Test
    fun testAttendance_atEdgeOf75Percent() {
        // 9 attended out of 12 = 75.0%
        val course = CourseAttendance("CS1002", "Operating Systems", attendedClasses = 9, totalClasses = 12)
        assertEquals(75.0f, course.percentage, 0.01f)
        assertTrue(course.isSafe)
        // If bunks 1, 9/13 = 69.2% (unsafe), so bunkable = 0
        assertEquals(0, course.bunkableClasses)
        assertEquals(0, course.requiredToCatchUp)
    }

    @Test
    fun testAttendance_below75Percent_recoveryCalculation() {
        // 6 attended out of 10 = 60.0%
        val course = CourseAttendance("CS1003", "Data Structures", attendedClasses = 6, totalClasses = 10)
        assertEquals(60.0f, course.percentage, 0.01f)
        assertFalse(course.isSafe)
        assertEquals(0, course.bunkableClasses)
        // Needs 6 consecutive classes to reach (6+6)/(10+6) = 12/16 = 75%
        assertEquals(6, course.requiredToCatchUp)
    }
}
