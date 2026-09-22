package com.vitreminder.app.data.model

import java.util.Calendar

data class ClassSession(
    val id: String,
    val dayOfWeek: Int, // Calendar.MONDAY (2) to Calendar.SUNDAY (1)
    val dayName: String,
    val slotTime: String, // "08:00-09:00"
    val startTime: String, // "08:00"
    val endTime: String, // "09:00"
    val startMinute: Int, // minutes from midnight, e.g. 480
    val endMinute: Int, // minutes from midnight, e.g. 540
    val subjectCode: String, // "ES26104"
    val subjectName: String, // "Programming for Engineers"
    val loadType: String, // "Lab", "Theory", "Tutorial"
    val batch: String?, // "B3" or null if combined
    val isCombined: Boolean,
    val classroom: String, // "1114"
    val facultyCode: String, // "GKK"
    val facultyName: String // "Gauri Kaluram Kharat"
) {
    val durationMinutes: Int
        get() = (endMinute - startMinute).coerceAtLeast(0)

    val formattedTimeRange: String
        get() = "$startTime - $endTime"

    val displayTitle: String
        get() = if (subjectName.isNotBlank()) subjectName else subjectCode

    val displaySubtitle: String
        get() = buildString {
            append(loadType)
            if (!isCombined && !batch.isNullOrBlank()) {
                append(" ($batch)")
            } else {
                append(" (Combined)")
            }
            if (classroom.isNotBlank()) {
                append(" • Room $classroom")
            }
        }
}

data class BreakPeriod(
    val dayOfWeek: Int,
    val dayName: String,
    val startTime: String,
    val endTime: String,
    val startMinute: Int,
    val endMinute: Int,
    val durationMinutes: Int
) {
    val formattedDuration: String
        get() {
            val hours = durationMinutes / 60
            val mins = durationMinutes % 60
            return when {
                hours > 0 && mins > 0 -> "${hours}h ${mins}m"
                hours > 0 -> "${hours} hour${if (hours > 1) "s" else ""}"
                else -> "$mins mins"
            }
        }
}
