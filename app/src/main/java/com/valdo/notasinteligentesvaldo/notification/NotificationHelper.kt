package com.valdo.notasinteligentesvaldo.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.valdo.notasinteligentesvaldo.MainActivity
import com.valdo.notasinteligentesvaldo.R
import com.valdo.notasinteligentesvaldo.models.Note

object NotificationHelper {

    const val CHANNEL_ID = "note_reminders"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.app_name)
            val descriptionText = "Recordatorios de notas"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Construye y muestra una notificación para la nota.
     * Si isPersistent es true, la notificación será ongoing.
     */
    fun showReminder(context: Context, note: Note, isPersistent: Boolean): Int {
        createChannel(context)

        val notificationId = note.id

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("noteId", note.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            note.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (note.title.isNotBlank() && note.title != "Nota sin título") {
            note.title
        } else {
            // Usa los primeros 70 caracteres del contenido si no hay título
            note.content.take(70)
        }

        val contentPreview = note.content.take(70)
        val fullText = buildString {
            append(contentPreview)
            if (contentPreview.isNotEmpty()) append("\n")
            append("Toque para ver más detalles")
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            // Usar el vector notification.xml como icono pequeño
            .setSmallIcon(R.drawable.notification)
            .setContentTitle(title)
            .setContentText(contentPreview)
            .setStyle(NotificationCompat.BigTextStyle().bigText(fullText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(!isPersistent)
            .setOngoing(isPersistent)

        val manager = NotificationManagerCompat.from(context)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            manager.notify(notificationId, builder.build())
        }

        return notificationId
    }

    fun cancelReminder(context: Context, notificationId: Int) {
        val manager = NotificationManagerCompat.from(context)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            manager.cancel(notificationId)
        }
    }

    fun showGenericMissingNote(context: Context, pendingIntent: PendingIntent) {
        createChannel(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText("La nota ya no existe. Se ha abierto la lista de notas.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val manager = NotificationManagerCompat.from(context)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            manager.notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }
}
