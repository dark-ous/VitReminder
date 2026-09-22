package com.vitreminder.app.parser

import com.vitreminder.app.data.model.ClassSession
import com.vitreminder.app.data.model.Faculty
import com.vitreminder.app.data.model.Timetable
import java.io.InputStream
import java.util.Calendar
import java.util.regex.Pattern

class VitTimetableParser(private val extractor: PdfExtractor = PdfExtractorFactory.create()) {

    companion object {
        val SLOT_TIMES = listOf(
            "08:00-09:00" to (8 * 60 to 9 * 60),
            "09:00-10:00" to (9 * 60 to 10 * 60),
            "10:00-11:00" to (10 * 60 to 11 * 60),
            "11:00-12:00" to (11 * 60 to 12 * 60),
            "12:00-13:00" to (12 * 60 to 13 * 60),
            "13:00-14:00" to (13 * 60 to 14 * 60),
            "14:00-15:00" to (14 * 60 to 15 * 60),
            "15:00-16:00" to (15 * 60 to 16 * 60),
            "16:00-17:00" to (16 * 60 to 17 * 60),
            "17:00-18:00" to (17 * 60 to 18 * 60),
            "18:00-19:00" to (18 * 60 to 19 * 60),
            "19:00-20:00" to (19 * 60 to 20 * 60)
        )

        private const val SLOT_X_START = 130.5f
        private const val SLOT_X_WIDTH = 86.3f
    }

    fun parse(inputStream: InputStream): Timetable {
        val textPositions = extractor.extractPositions(inputStream)
        if (textPositions.isEmpty()) {
            throw IllegalArgumentException("No text could be extracted from the timetable PDF.")
        }

        val metadata = extractMetadata(textPositions)
        val facultyMap = extractFacultyDirectory(textPositions)
        val sessions = extractSessions(textPositions, metadata.division, facultyMap)

        return Timetable(
            division = metadata.division,
            program = metadata.program,
            academicYear = metadata.academicYear,
            semester = metadata.semester,
            version = metadata.version,
            wefDate = metadata.wefDate,
            toDate = metadata.toDate,
            allSessions = sessions,
            facultyList = facultyMap.values.toList()
        )
    }

    private data class Metadata(
        val division: String = "FY CS-H",
        val program: String = "",
        val academicYear: String = "",
        val semester: String = "",
        val version: String = "",
        val wefDate: String = "",
        val toDate: String = ""
    )

    private fun extractMetadata(items: List<PdfTextPosition>): Metadata {
        val allText = items.joinToString(" ") { it.text }

        val divisionMatch = Pattern.compile("Division\\s*:\\s*([A-Za-z0-9\\- ]+?)(?=\\s+Program|\\s+Academic|\\s+Department|$)").matcher(allText)
        val division = if (divisionMatch.find()) divisionMatch.group(1)?.trim() ?: "FY CS-H" else "FY CS-H"

        val programMatch = Pattern.compile("Program\\s*:\\s*([A-Za-z0-9\\-]+)").matcher(allText)
        val program = if (programMatch.find()) programMatch.group(1)?.trim() ?: "" else ""

        val acadMatch = Pattern.compile("Academic\\s*Year\\s*:\\s*([0-9\\-]+(?:\\s*[0-9]+)?)").matcher(allText)
        val acadYear = if (acadMatch.find()) acadMatch.group(1)?.replace(Regex("\\s+"), "")?.trim() ?: "" else ""

        val semMatch = Pattern.compile("Semester\\s*:\\s*([0-9]+)").matcher(allText)
        val semester = if (semMatch.find()) semMatch.group(1)?.trim() ?: "1" else "1"

        val verMatch = Pattern.compile("Version\\s*:\\s*([A-Za-z0-9]+)").matcher(allText)
        val version = if (verMatch.find()) verMatch.group(1)?.trim() ?: "V1" else "V1"

        val wefMatch = Pattern.compile("W\\.E\\.F\\s*:\\s*([0-9A-Za-z\\-]+)").matcher(allText)
        val wef = if (wefMatch.find()) wefMatch.group(1)?.trim() ?: "" else ""

        val toMatch = Pattern.compile("To\\s*Date\\s*:\\s*([0-9A-Za-z\\-]+)").matcher(allText)
        val toDate = if (toMatch.find()) toMatch.group(1)?.trim() ?: "" else ""

        return Metadata(
            division = division,
            program = program,
            academicYear = acadYear,
            semester = semester,
            version = version,
            wefDate = wef,
            toDate = toDate
        )
    }

    private fun extractFacultyDirectory(items: List<PdfTextPosition>): Map<String, Faculty> {
        val facultyMap = mutableMapOf<String, Faculty>()
        val page2And3 = items.filter { it.page >= 2 }
        val fullText = page2And3.joinToString(" ") { it.text }

        val pattern = Pattern.compile(
            "([A-Za-z0-9_]+)\\s+([A-Za-z0-9_]+)\\s*\\(([^)]+)\\)"
        )
        val matcher = pattern.matcher(fullText)
        while (matcher.find()) {
            val empId = matcher.group(1)?.trim() ?: ""
            val code = matcher.group(2)?.trim() ?: ""
            var rawName = matcher.group(3)?.trim() ?: ""
            // Clean up any table column spillover into parentheses
            val cleanName = rawName.replace(Regex("(?i)(Tutorial|Theory|Lab|ES\\d+|BE -|PE -|LA -|LENG -).*"), "").trim()
            val finalName = if (cleanName.isNotBlank()) cleanName else rawName

            if (code.length in 2..15 && !facultyMap.containsKey(code)) {
                facultyMap[code] = Faculty(
                    code = code,
                    fullName = finalName,
                    empId = empId
                )
            }
        }
        return facultyMap
    }

    private fun extractSessions(
        items: List<PdfTextPosition>,
        divisionName: String,
        facultyMap: Map<String, Faculty>
    ): List<ClassSession> {
        val sessions = mutableListOf<ClassSession>()

        // Dynamically detect day labels and their vertical positions across pages
        val validDayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val dayLabels = items
            .filter { item ->
                validDayNames.any { it.equals(item.text, ignoreCase = true) } && item.x < 120f
            }
            .map { item ->
                val matchedName = validDayNames.first { it.equals(item.text, ignoreCase = true) }
                Triple(item.page, matchedName, item.y)
            }
            .sortedWith(compareBy({ it.first }, { it.third }))

        val validPages = dayLabels.map { it.first }.toSet()
        val minDayY = dayLabels.minOfOrNull { it.third } ?: 100f
        val avgRowHeight = if (dayLabels.size > 1) {
            (dayLabels.last().third - dayLabels.first().third) / (dayLabels.size - 1)
        } else 75f
        val maxDayY = (dayLabels.maxOfOrNull { it.third } ?: 500f) + avgRowHeight + 10f

        val itemGrid = mutableMapOf<String, MutableList<PdfTextPosition>>()

        for (item in items) {
            // Class sessions exist ONLY on schedule pages and strictly within the day rows range (not in header or footer)
            if (item.page !in validPages || item.y < minDayY - 15f || item.y > maxDayY) continue

            val slotIndex = getSlotIndex(item.x)
            if (slotIndex !in 0 until SLOT_TIMES.size) continue

            // Determine which day this item belongs to on its page
            val pageLabels = dayLabels.filter { it.first == item.page }
            val dayName = pageLabels.filter { it.third <= item.y + 25f }.maxByOrNull { it.third }?.second
                ?: continue

            // Tag item with day & slot
            itemGrid.computeIfAbsent("$dayName#$slotIndex") { mutableListOf() }.add(item)
        }

        // Process each cell
        itemGrid.forEach { (key, cellItems) ->
            val parts = key.split("#")
            val dayName = parts[0]
            val slotIndex = parts[1].toInt()
            val dayOfWeek = when (dayName) {
                "Monday" -> Calendar.MONDAY
                "Tuesday" -> Calendar.TUESDAY
                "Wednesday" -> Calendar.WEDNESDAY
                "Thursday" -> Calendar.THURSDAY
                "Friday" -> Calendar.FRIDAY
                "Saturday" -> Calendar.SATURDAY
                else -> Calendar.SUNDAY
            }

            val (slotLabel, times) = SLOT_TIMES[slotIndex]
            val (startMin, endMin) = times
            val startTime = slotLabel.substringBefore("-")
            val endTime = slotLabel.substringAfter("-")

            val parsedCellSessions = parseCell(
                cellItems = cellItems,
                dayName = dayName,
                dayOfWeek = dayOfWeek,
                slotLabel = slotLabel,
                startTime = startTime,
                endTime = endTime,
                startMin = startMin,
                endMin = endMin,
                facultyMap = facultyMap,
                divisionName = divisionName
            )
            sessions.addAll(parsedCellSessions)
        }

        return sessions
    }

    private fun getSlotIndex(x: Float): Int {
        val relX = x - SLOT_X_START
        val idx = Math.round(relX / SLOT_X_WIDTH)
        // Allow a margin of +/- 40 points
        val expectedX = SLOT_X_START + idx * SLOT_X_WIDTH
        return if (Math.abs(x - expectedX) < 45f && idx in 0..11) idx else -1
    }

    private fun parseCell(
        cellItems: List<PdfTextPosition>,
        dayName: String,
        dayOfWeek: Int,
        slotLabel: String,
        startTime: String,
        endTime: String,
        startMin: Int,
        endMin: Int,
        facultyMap: Map<String, Faculty>,
        divisionName: String
    ): List<ClassSession> {
        val sorted = cellItems.sortedBy { it.y }
        val cellText = sorted.joinToString(" ") { it.text }
            .replace(divisionName, " ")
            .replace("FY CS-H", " ")
            .trim()

        if (cellText.isEmpty()) return emptyList()

        val results = mutableListOf<ClassSession>()

        // 1. Primary strict pattern: captures faculty, code, name, batch (with colon), loadType, room
        val primaryPattern = Pattern.compile(
            "([A-Za-z0-9_]{2,10})?\\s*([A-Z0-9]{4,10})\\s*-\\s*([^:]+):([A-Za-z0-9_\\-]+|\\-)\\s*(Lab|Theory|Tutorial)?\\s*([0-9]{4}[A-Za-z\\-]*|[A-Za-z0-9\\-]+)?"
        )
        val matcher = primaryPattern.matcher(cellText)

        val invalidCodes = setOf("SUBJECT", "TEACHER", "LOAD", "TUTORIAL", "THEORY", "EDUPLUSCAM", "AUTHORISED", "HEORY", "HEOR", "TOTAL", "SEMINAR", "PROJECT", "GENERAL")
        val validCourseCodePattern = Pattern.compile("^[A-Z]{2,4}[0-9]{4,5}[A-Za-z]?$")

        while (matcher.find()) {
            val rawFaculty = matcher.group(1)?.trim() ?: ""
            val courseCode = matcher.group(2)?.trim() ?: ""
            val courseName = matcher.group(3)?.trim() ?: ""
            val batchRaw = matcher.group(4)?.trim() ?: ""
            val loadType = matcher.group(5)?.trim() ?: "Theory"
            val classroom = matcher.group(6)?.trim() ?: ""

            if (courseCode.uppercase() in invalidCodes || !validCourseCodePattern.matcher(courseCode).matches()) continue

            val (isCombined, batch) = sanitizeBatch(batchRaw)

            val facultyCode = rawFaculty.takeIf { it.isNotBlank() } ?: ""
            val facultyName = facultyMap[facultyCode]?.fullName ?: facultyCode

            val id = "${dayName}_${slotLabel}_${courseCode}_${batch ?: "combined"}"

            results.add(
                ClassSession(
                    id = id,
                    dayOfWeek = dayOfWeek,
                    dayName = dayName,
                    slotTime = slotLabel,
                    startTime = startTime,
                    endTime = endTime,
                    startMinute = startMin,
                    endMinute = endMin,
                    subjectCode = courseCode,
                    subjectName = courseName,
                    loadType = loadType,
                    batch = batch,
                    isCombined = isCombined,
                    classroom = classroom,
                    facultyCode = facultyCode,
                    facultyName = facultyName
                )
            )
        }

        if (results.isNotEmpty()) return results

        // 2. Fallback resilient pattern: handles cells with dashes, slashes, or missing colons
        val fallbackPattern = Pattern.compile(
            "([A-Z0-9]{4,10})\\s*[-:]?\\s*([A-Za-z0-9 ]+?)(?:\\s+(B[1-9]|Combined|-))?\\s*(Lab|Theory|Tutorial)?\\s*([0-9]{4}[A-Za-z\\-]*)?",
            Pattern.CASE_INSENSITIVE
        )
        val fbMatcher = fallbackPattern.matcher(cellText)
        while (fbMatcher.find()) {
            val courseCode = fbMatcher.group(1)?.trim() ?: ""
            val courseName = fbMatcher.group(2)?.trim() ?: ""
            val batchRaw = fbMatcher.group(3)?.trim() ?: ""
            val loadType = fbMatcher.group(4)?.trim() ?: "Theory"
            val classroom = fbMatcher.group(5)?.trim() ?: ""

            if (courseCode.uppercase() in invalidCodes || !validCourseCodePattern.matcher(courseCode).matches()) continue

            val (isCombined, batch) = sanitizeBatch(batchRaw)

            val id = "${dayName}_${slotLabel}_${courseCode}_${batch ?: "combined"}"

            results.add(
                ClassSession(
                    id = id,
                    dayOfWeek = dayOfWeek,
                    dayName = dayName,
                    slotTime = slotLabel,
                    startTime = startTime,
                    endTime = endTime,
                    startMinute = startMin,
                    endMinute = endMin,
                    subjectCode = courseCode,
                    subjectName = courseName,
                    loadType = loadType,
                    batch = batch,
                    isCombined = isCombined,
                    classroom = classroom,
                    facultyCode = "",
                    facultyName = ""
                )
            )
        }

        return results
    }

    private fun sanitizeBatch(raw: String): Pair<Boolean, String?> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed == "-" || trimmed.equals("Combined", true) || trimmed.equals("ALL", true)) {
            return Pair(true, null)
        }
        if (trimmed.startsWith("V", true)) {
            return Pair(true, null)
        }
        if (trimmed.matches(Regex("(?i)^B?[1-9]$"))) {
            val normalized = if (trimmed.length == 1) "B$trimmed" else trimmed.uppercase()
            return Pair(false, normalized)
        }
        return Pair(true, null)
    }
}
