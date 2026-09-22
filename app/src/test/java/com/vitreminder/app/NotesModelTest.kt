package com.vitreminder.app

import com.vitreminder.app.data.model.ClassNote
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class NotesModelTest {

    @Test
    fun testClassNote_classBoundIdentification() {
        val classBoundNote = ClassNote(
            text = "Submit Assignment 2",
            subjectCode = "CS1001",
            subjectName = "Object Oriented Programming",
            classroom = "1203",
            timeSlot = "10:00 - 11:00",
            dayOfWeek = Calendar.MONDAY,
            dayName = "Monday"
        )

        assertTrue(classBoundNote.isClassBound)
        assertEquals("Object Oriented Programming (1203)", classBoundNote.displayTag)
        assertFalse(classBoundNote.isCompleted)

        val generalNote = ClassNote(
            text = "Pay exam fees before Friday"
        )
        assertFalse(generalNote.isClassBound)
        assertEquals("General Task", generalNote.displayTag)
    }

    @Test
    fun testClassNote_toggleCompletion() {
        val note = ClassNote(text = "Bring lab journal")
        assertFalse(note.isCompleted)

        val completedNote = note.copy(isCompleted = !note.isCompleted)
        assertTrue(completedNote.isCompleted)

        val undoneNote = completedNote.copy(isCompleted = !completedNote.isCompleted)
        assertFalse(undoneNote.isCompleted)
    }

    @Test
    fun testClassNote_filtering() {
        val notes = listOf(
            ClassNote(id = "1", text = "Note A", subjectCode = "CS1001", isCompleted = false),
            ClassNote(id = "2", text = "Note B", subjectCode = "CS1002", isCompleted = true),
            ClassNote(id = "3", text = "Note C", subjectCode = "CS1001", isCompleted = true),
            ClassNote(id = "4", text = "General Note", isCompleted = false)
        )

        val pending = notes.filterNot { it.isCompleted }
        assertEquals(2, pending.size)
        assertEquals(listOf("1", "4"), pending.map { it.id })

        val cs1001Notes = notes.filter { it.subjectCode == "CS1001" }
        assertEquals(2, cs1001Notes.size)
        assertEquals(listOf("1", "3"), cs1001Notes.map { it.id })
    }
}
