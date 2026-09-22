package com.vitreminder.app.data.model

data class Faculty(
    val code: String, // e.g. "GKK"
    val fullName: String, // e.g. "Gauri Kaluram Kharat"
    val empId: String = "", // e.g. "12387"
    val loadType: String = "", // e.g. "Lab"
    val subjectSummary: String = "" // e.g. "PE - ES26104 - Programming for Engineers"
)
