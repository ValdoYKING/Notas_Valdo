package com.valdo.notasinteligentesvaldo.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.valdo.notasinteligentesvaldo.data.DatabaseBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getIntExtra("noteId", -1)
        val isPersistent = intent.getBooleanExtra("isPersistent", false)
        if (noteId <= 0) return

        // Usar la misma instancia/configuración (nombre + migraciones) que el resto de la app
        val noteDao = DatabaseBuilder.getInstance(context).noteDao()

        CoroutineScope(Dispatchers.IO).launch {
            val note = noteDao.getNoteById(noteId).firstOrNull()
            if (note != null) {
                NotificationHelper.showReminder(context, note, isPersistent)
                // Limpiar el estado del recordatorio tras mostrar la notificación
                noteDao.clearReminderState(noteId)
            }
        }
    }
}
