package com.vitreminder.app.data.model

enum class Campus(val displayName: String, val locationName: String) {
    BIBWEWADI("Bibwewadi Campus", "Main Campus, Bibwewadi, Pune"),
    KONDHWA("Kondhwa Campus", "Kondhwa Budruk Campus, Pune")
}

data class Branch(
    val code: String, // e.g. "CS", "AI", "AIDS", "IT", "AIML", "ET", "CSCBI", "CSDS", "CSSE", "CV", "IC", "ME"
    val displayName: String,
    val campus: Campus
)

data class DivisionItem(
    val divisionCode: String, // e.g. "CS-H", "AI-F", "AIDS-A"
    val branchCode: String,   // e.g. "CS"
    val sectionLetter: String,// e.g. "H"
    val campus: Campus,
    val pdfUrl: String,
    val semester: String = "1",
    val academicYear: String = "2026-27"
) {
    val displayTitle: String
        get() = "$branchCode - Section $sectionLetter"
}
