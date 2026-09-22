package com.vitreminder.app.data.model

import kotlin.math.ceil

data class CourseAttendance(
    val courseCode: String,
    val courseTitle: String,
    val attendedClasses: Int = 0,
    val totalClasses: Int = 0
) {
    val percentage: Float
        get() = if (totalClasses == 0) 100f else (attendedClasses.toFloat() / totalClasses) * 100f

    val isSafe: Boolean
        get() = percentage >= 75f

    /**
     * How many classes you can afford to miss without dropping below 75%.
     */
    val bunkableClasses: Int
        get() = if (totalClasses == 0) 0 else {
            val maxAllowed = ((attendedClasses - 0.75 * totalClasses) / 0.75).toInt()
            maxAllowed.coerceAtLeast(0)
        }

    /**
     * How many consecutive classes you must attend to recover back to 75%.
     */
    val requiredToCatchUp: Int
        get() = if (isSafe || totalClasses == 0) 0 else {
            val req = ceil((3.0 * totalClasses - 4.0 * attendedClasses)).toInt()
            req.coerceAtLeast(1)
        }

    val statusMessage: String
        get() = when {
            totalClasses == 0 -> "No classes recorded yet"
            isSafe && bunkableClasses > 0 -> "You can safely bunk $bunkableClasses class${if (bunkableClasses > 1) "es" else ""} 🌴"
            isSafe -> "Attendance on track (75%)! Don't miss the next class ⚠️"
            else -> "Must attend $requiredToCatchUp class${if (requiredToCatchUp > 1) "es" else ""} without missing to reach 75% 🚨"
        }
}
