package com.valdo.notasinteligentesvaldo.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.valdo.notasinteligentesvaldo.data.NoteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getIntExtra("noteId", -1)
        val isPersistent = intent.getBooleanExtra("isPersistent", false)
        if (noteId <= 0) return

        val db = Room.databaseBuilder(
            context.applicationContext,
            NoteDatabase::class.java,
            "notes_db"
        ).build()

        CoroutineScope(Dispatchers.IO).launch {
            val noteDao = db.noteDao()
            val note = noteDao.getNoteById(noteId).firstOrNull()
            if (note != null) {
                // Nota encontrada: mostrar la notificación normal con su contenido
                NotificationHelper.showReminder(context, note, isPersistent)
                // IMPORTANTE: limpiar el estado de recordatorio tras mostrar la notificación
                noteDao.clearReminderState(noteId)
            } else {
                // Nota ya no existe: no mostrar ninguna notificación
                // Simplemente salimos sin llamar a NotificationHelper
            }
        }
    }
}
