package com.vitreminder.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.vitreminder.app.data.local.NotesRepository

class ToggleNoteCallback : ActionCallback {
    companion object {
        val NoteIdKey = ActionParameters.Key<String>("note_id")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val noteId = parameters[NoteIdKey] ?: return
        val repo = NotesRepository(context)
        repo.toggleNoteCompletion(noteId)
        NotesGlanceWidget.updateAll(context)
    }
}
