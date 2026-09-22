package com.vitreminder.app.data.local

import com.vitreminder.app.data.model.Branch
import com.vitreminder.app.data.model.Campus
import com.vitreminder.app.data.model.DivisionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

class DirectoryRepository {

    // Verified complete catalog from official VIT portal: https://www.vit.edu/DESH/undergraduate-programme/
    private val staticDivisions: List<DivisionItem> = listOf(
        // === BIBWEWADI CAMPUS ===
        // AI (Sections A to F)
        DivisionItem("AI-A", "AI", "A", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/AI-A.pdf"),
        DivisionItem("AI-B", "AI", "B", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/AI-B.pdf"),
        DivisionItem("AI-C", "AI", "C", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/AI-C.pdf"),
        DivisionItem("AI-D", "AI", "D", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/AI-D.pdf"),
        DivisionItem("AI-E", "AI", "E", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/AI-E.pdf"),
        DivisionItem("AI-F", "AI", "F", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/AI-F.pdf"),

        // CS (Computer Engineering, Sections A to L)
        DivisionItem("CS-A", "CS", "A", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/CS-A.pdf"),
        DivisionItem("CS-B", "CS", "B", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/CS-B.pdf"),
        DivisionItem("CS-C", "CS", "C", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/CS-C.pdf"),
        DivisionItem("CS-D", "CS", "D", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/CS-D.pdf"),
        DivisionItem("CS-E", "CS", "E", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/CS-E.pdf"),
        DivisionItem("CS-F", "CS", "F", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/CS-F.pdf"),
        DivisionItem("CS-G", "CS", "G", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/CS-G.pdf"),
        DivisionItem("CS-H", "CS", "H", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/CS-H.pdf"),
        DivisionItem("CS-I", "CS", "I", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/CS-I.pdf"),
        DivisionItem("CS-J", "CS", "J", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/CS-J.pdf"),
        DivisionItem("CS-K", "CS", "K", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/CS-K.pdf"),
        DivisionItem("CS-L", "CS", "L", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/CS-L.pdf"),

        // AIML (AI & Machine Learning, Sections A to F)
        DivisionItem("AIML-A", "AIML", "A", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/AIML-A.pdf"),
        DivisionItem("AIML-B", "AIML", "B", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/AIML-B.pdf"),
        DivisionItem("AIML-C", "AIML", "C", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/AIML-C.pdf"),
        DivisionItem("AIML-D", "AIML", "D", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/AIML-D.pdf"),
        DivisionItem("AIML-E", "AIML", "E", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/AIML-E.pdf"),
        DivisionItem("AIML-F", "AIML", "F", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/AIML-F.pdf"),

        // IT (Information Technology, Sections A to F)
        DivisionItem("IT-A", "IT", "A", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/IT-A.pdf"),
        DivisionItem("IT-B", "IT", "B", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/IT-B.pdf"),
        DivisionItem("IT-C", "IT", "C", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/IT-C.pdf"),
        DivisionItem("IT-D", "IT", "D", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/IT-D.pdf"),
        DivisionItem("IT-E", "IT", "E", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/IT-E.pdf"),
        DivisionItem("IT-F", "IT", "F", Campus.BIBWEWADI, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/IT-F.pdf"),

        // === KONDHWA CAMPUS ===
        // AIDS (Artificial Intelligence and Data Science, Sections A to F)
        DivisionItem("AIDS-A", "AIDS", "A", Campus.KONDHWA, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/AIDS_A.pdf"),
        DivisionItem("AIDS-B", "AIDS", "B", Campus.KONDHWA, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/AIDS_B.pdf"),
        DivisionItem("AIDS-C", "AIDS", "C", Campus.KONDHWA, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/AIDS_C.pdf"),
        DivisionItem("AIDS-D", "AIDS", "D", Campus.KONDHWA, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/AIDS_D.pdf"),
        DivisionItem("AIDS-E", "AIDS", "E", Campus.KONDHWA, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/AIDS_E.pdf"),
        DivisionItem("AIDS-F", "AIDS", "F", Campus.KONDHWA, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/AIDS_F.pdf"),

        // CSCBI (CS - Cyber Security & IoT, Sections A to C)
        DivisionItem("CSCBI-A", "CSCBI", "A", Campus.KONDHWA, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/CSCBI_A.pdf"),
        DivisionItem("CSCBI-B", "CSCBI", "B", Campus.KONDHWA, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/CSCBI_B.pdf"),
        DivisionItem("CSCBI-C", "CSCBI", "C", Campus.KONDHWA, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/CSCBI_C.pdf"),

        // CSDS (CS - Data Science, Sections A to C)
        DivisionItem("CSDS-A", "CSDS", "A", Campus.KONDHWA, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/CSDS_A-1.pdf"),
        DivisionItem("CSDS-B", "CSDS", "B", Campus.KONDHWA, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/CSDS_B-1.pdf"),
        DivisionItem("CSDS-C", "CSDS", "C", Campus.KONDHWA, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/CSDS_C-1.pdf"),

        // CSSE (CS - Software Engineering, Sections A to C)
        DivisionItem("CSSE-A", "CSSE", "A", Campus.KONDHWA, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/CSSE_A-1.pdf"),
        DivisionItem("CSSE-B", "CSSE", "B", Campus.KONDHWA, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/CSSE_B-1.pdf"),
        DivisionItem("CSSE-C", "CSSE", "C", Campus.KONDHWA, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/CSSE_C-1.pdf"),

        // ET (Electronics & Telecommunication, Sections A to F)
        DivisionItem("ET-A", "ET", "A", Campus.KONDHWA, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/ET_A-1.pdf"),
        DivisionItem("ET-B", "ET", "B", Campus.KONDHWA, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/ET_B-1.pdf"),
        DivisionItem("ET-C", "ET", "C", Campus.KONDHWA, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/ET_C-1.pdf"),
        DivisionItem("ET-D", "ET", "D", Campus.KONDHWA, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/ET_D-1.pdf"),
        DivisionItem("ET-E", "ET", "E", Campus.KONDHWA, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/ET_E-1.pdf"),
        DivisionItem("ET-F", "ET", "F", Campus.KONDHWA, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/ET_F-1.pdf"),

        // IC (Instrumentation & Control, Sections A to C)
        DivisionItem("IC-A", "IC", "A", Campus.KONDHWA, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/IC_A-1.pdf"),
        DivisionItem("IC-B", "IC", "B", Campus.KONDHWA, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/IC_B-1.pdf"),
        DivisionItem("IC-C", "IC", "C", Campus.KONDHWA, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/IC_C-1.pdf"),

        // CV (Civil Engineering, Sections A to C)
        DivisionItem("CV-A", "CV", "A", Campus.KONDHWA, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/CV_A-1.pdf"),
        DivisionItem("CV-B", "CV", "B", Campus.KONDHWA, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/CV_B-1.pdf"),
        DivisionItem("CV-C", "CV", "C", Campus.KONDHWA, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/CV_B-2.pdf"),

        // ME (Mechanical Engineering, Sections A and B)
        DivisionItem("ME-A", "ME", "A", Campus.KONDHWA, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/ME_A-1.pdf"),
        DivisionItem("ME-B", "ME", "B", Campus.KONDHWA, "https://www.vit.edu/DESH/wp-content/uploads/2026/09/ME_B-1.pdf")
    )

    private val branches = listOf(
        // Bibwewadi
        Branch("CS", "Computer Engineering", Campus.BIBWEWADI),
        Branch("AI", "Artificial Intelligence", Campus.BIBWEWADI),
        Branch("AIML", "AI & Machine Learning", Campus.BIBWEWADI),
        Branch("IT", "Information Technology", Campus.BIBWEWADI),

        // Kondhwa
        Branch("AIDS", "AI & Data Science", Campus.KONDHWA),
        Branch("CSSE", "CS - Software Engineering", Campus.KONDHWA),
        Branch("CSDS", "CS - Data Science", Campus.KONDHWA),
        Branch("CSCBI", "CS - Cyber Security & IoT", Campus.KONDHWA),
        Branch("ET", "Electronics & Telecom (ENTC)", Campus.KONDHWA),
        Branch("IC", "Instrumentation & Control", Campus.KONDHWA),
        Branch("ME", "Mechanical Engineering", Campus.KONDHWA),
        Branch("CV", "Civil Engineering", Campus.KONDHWA)
    )

    fun getCampuses(): List<Campus> = Campus.values().toList()

    fun getBranchesForCampus(campus: Campus): List<Branch> {
        return branches.filter { it.campus == campus }
    }

    fun getDivisionsForBranch(campus: Campus, branchCode: String): List<DivisionItem> {
        return staticDivisions.filter { it.campus == campus && it.branchCode.equals(branchCode, ignoreCase = true) }
    }

    fun findDivision(divisionCode: String): DivisionItem? {
        val normalized = divisionCode.trim().uppercase()
        return staticDivisions.find { it.divisionCode.equals(normalized, ignoreCase = true) }
            ?: staticDivisions.find { it.divisionCode.replace("-", "").equals(normalized.replace("-", ""), ignoreCase = true) }
    }

    suspend fun fetchLiveDirectory(): List<DivisionItem> = withContext(Dispatchers.IO) {
        val portalUrl = "https://www.vit.edu/DESH/undergraduate-programme/"
        val found = mutableListOf<DivisionItem>()
        try {
            val conn = URL(portalUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 12000
            conn.readTimeout = 12000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            val html = conn.inputStream.bufferedReader().use { it.readText() }

            val bibPos = html.indexOf("Bibwewadi Divisions")
            val kondPos = html.indexOf("Kondhwa Divisions")

            if (bibPos != -1 && kondPos != -1) {
                // Bibwewadi
                val bibHtml = html.substring(bibPos, kondPos)
                parseCampusLinks(bibHtml, Campus.BIBWEWADI, found)

                // Kondhwa
                val kondEnd = (kondPos + 25000).coerceAtMost(html.length)
                val kondHtml = html.substring(kondPos, kondEnd)
                parseCampusLinks(kondHtml, Campus.KONDHWA, found)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (found.isNotEmpty()) found else staticDivisions
    }

    private fun parseCampusLinks(html: String, campus: Campus, out: MutableList<DivisionItem>) {
        val pattern = Pattern.compile("<a[^>]+href=[\"']([^\"']+\\.pdf)[\"'][^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        val matcher = pattern.matcher(html)
        while (matcher.find()) {
            val url = matcher.group(1)?.trim() ?: ""
            val rawText = matcher.group(2)?.replace(Regex("<[^>]+>"), "")?.trim() ?: ""

            if (rawText.isBlank() || rawText.contains("syllabus", ignoreCase = true) || rawText.contains("calendar", ignoreCase = true)) {
                continue
            }

            // e.g. "AI-A", "CS-H", "AIDS_A", "ET-F"
            val cleanCode = rawText.replace("_", "-")
            val parts = cleanCode.split("-")
            val branch = if (parts.isNotEmpty()) parts[0] else ""
            val section = if (parts.size > 1) parts[1] else "A"

            if (branch.isNotBlank()) {
                out.add(
                    DivisionItem(
                        divisionCode = cleanCode,
                        branchCode = branch,
                        sectionLetter = section,
                        campus = campus,
                        pdfUrl = url
                    )
                )
            }
        }
    }
}
