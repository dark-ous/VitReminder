package com.vitreminder.app

import com.vitreminder.app.data.local.DirectoryRepository
import com.vitreminder.app.data.model.Campus
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DirectoryRepositoryTest {

    private lateinit var repository: DirectoryRepository

    @Before
    fun setUp() {
        repository = DirectoryRepository()
    }

    @Test
    fun testCampusesAndBranches() {
        val campuses = repository.getCampuses()
        assertEquals(2, campuses.size)
        assertTrue(campuses.contains(Campus.BIBWEWADI))
        assertTrue(campuses.contains(Campus.KONDHWA))

        val bibBranches = repository.getBranchesForCampus(Campus.BIBWEWADI)
        assertTrue(bibBranches.any { it.code == "CS" })
        assertTrue(bibBranches.any { it.code == "AI" })
        assertTrue(bibBranches.any { it.code == "AIML" })
        assertTrue(bibBranches.any { it.code == "IT" })

        val kondBranches = repository.getBranchesForCampus(Campus.KONDHWA)
        assertTrue(kondBranches.any { it.code == "AIDS" })
        assertTrue(kondBranches.any { it.code == "ET" })
    }

    @Test
    fun testDivisionsMapping() {
        val csDivisions = repository.getDivisionsForBranch(Campus.BIBWEWADI, "CS")
        assertFalse(csDivisions.isEmpty())
        assertTrue("Should contain CS-H", csDivisions.any { it.divisionCode == "CS-H" })
        val csh = repository.findDivision("CS-H")
        assertNotNull(csh)
        assertEquals("https://www.vit.edu/DESH/wp-content/uploads/2026/09/CS-H.pdf", csh!!.pdfUrl)

        val aidsDivisions = repository.getDivisionsForBranch(Campus.KONDHWA, "AIDS")
        assertFalse(aidsDivisions.isEmpty())
        assertTrue("Should contain AIDS-A", aidsDivisions.any { it.divisionCode == "AIDS-A" })
    }
}
