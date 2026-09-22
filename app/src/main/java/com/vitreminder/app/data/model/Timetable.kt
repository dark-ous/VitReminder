package com.vitreminder.app.data.model

data class Timetable(
    val division: String, // e.g. "FY CS-H"
    val program: String = "", // e.g. "DESH"
    val academicYear: String = "", // e.g. "2026-27"
    val semester: String = "", // e.g. "1"
    val version: String = "", // e.g. "V1"
    val wefDate: String = "", // e.g. "15-Sep-2026"
    val toDate: String = "", // e.g. "10-Jan-2027"
    val allSessions: List<ClassSession> = emptyList(),
    val facultyList: List<Faculty> = emptyList()
) {
    val availableBatches: List<String>
        get() {
            val list = allSessions.mapNotNull { it.batch }
                .filter { it.isNotBlank() && it.matches(Regex("(?i)^B?[1-9]$")) }
                .map { if (it.length == 1) "B$it" else it.uppercase() }
                .distinct()
                .sorted()
            return if (list.isNotEmpty()) list else listOf("B1", "B2", "B3")
        }

    /**
     * Filters sessions for a specific batch (e.g. "B3").
     * Returns:
     * 1. Sessions explicitly assigned to [userBatch]
     * 2. All combined / division sessions (isCombined == true or batch == null)
     *
     * Consecutive same-course sessions in the same room (e.g. 2-hour labs: 08:00-09:00 and 09:00-10:00)
     * are cleanly merged into a single multi-hour session block!
     */
    fun getFilteredSessions(userBatch: String?): List<ClassSession> {
        val normalizedUserBatch = userBatch?.trim()?.uppercase()
        val rawFiltered = allSessions.filter { session ->
            if (normalizedUserBatch.isNullOrBlank()) {
                true
            } else {
                val sBatch = session.batch?.trim()?.uppercase()
                session.isCombined || sBatch == normalizedUserBatch ||
                    (sBatch != null && normalizedUserBatch.removePrefix("B") == sBatch.removePrefix("B"))
            }
        }.sortedWith(compareBy({ it.dayOfWeek }, { it.startMinute }))

        return mergeConsecutiveSessions(rawFiltered)
    }

    /**
     * Detects breaks between scheduled classes for the given day and filtered sessions.
     */
    fun getBreaksForDay(dayOfWeek: Int, filteredSessions: List<ClassSession>): List<BreakPeriod> {
        val daysClasses = filteredSessions
            .filter { it.dayOfWeek == dayOfWeek }
            .sortedBy { it.startMinute }

        if (daysClasses.size < 2) return emptyList()

        val breaks = mutableListOf<BreakPeriod>()
        for (i in 0 until daysClasses.size - 1) {
            val current = daysClasses[i]
            val next = daysClasses[i + 1]

            if (next.startMinute > current.endMinute) {
                val duration = next.startMinute - current.endMinute
                // A gap of 15 minutes or more is considered a break
                if (duration >= 15) {
                    breaks.add(
                        BreakPeriod(
                            dayOfWeek = dayOfWeek,
                            dayName = current.dayName,
                            startTime = current.endTime,
                            endTime = next.startTime,
                            startMinute = current.endMinute,
                            endMinute = next.startMinute,
                            durationMinutes = duration
                        )
                    )
                }
            }
        }
        return breaks
    }

    private fun mergeConsecutiveSessions(sessions: List<ClassSession>): List<ClassSession> {
        if (sessions.isEmpty()) return emptyList()
        val merged = mutableListOf<ClassSession>()

        var current = sessions[0]
        for (i in 1 until sessions.size) {
            val next = sessions[i]
            val isSameDay = current.dayOfWeek == next.dayOfWeek
            val isBackToBack = current.endMinute == next.startMinute
            val isSameSubject = current.subjectCode == next.subjectCode
            val isSameRoom = current.classroom == next.classroom
            val isSameType = current.loadType == next.loadType

            if (isSameDay && isBackToBack && isSameSubject && isSameRoom && isSameType) {
                // Merge into single extended block
                current = current.copy(
                    endTime = next.endTime,
                    endMinute = next.endMinute,
                    slotTime = "${current.startTime}-${next.endTime}"
                )
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        return merged
    }
}
