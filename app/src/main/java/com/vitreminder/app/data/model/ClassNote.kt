package com.vitreminder.app.data.model

import java.util.UUID

data class ClassNote(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val dayOfWeek: Int = 0, // e.g. Calendar.MONDAY
    val dayName: String = "",
    val subjectCode: String = "",
    val subjectName: String = "",
    val classroom: String = "",
    val timeSlot: String = "",
    val batch: String = ""
) {
    val isClassBound: Boolean
        get() = subjectCode.isNotBlank() || subjectName.isNotBlank()

    val displayTag: String
        get() = when {
            subjectName.isNotBlank() && classroom.isNotBlank() -> "$subjectName ($classroom)"
            subjectName.isNotBlank() -> subjectName
            subjectCode.isNotBlank() -> subjectCode
            dayName.isNotBlank() -> dayName
            else -> "General Task"
        }
}
