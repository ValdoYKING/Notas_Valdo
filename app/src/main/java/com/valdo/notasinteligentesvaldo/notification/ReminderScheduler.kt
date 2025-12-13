package com.valdo.notasinteligentesvaldo.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Helper sencillo para programar/cancelar recordatorios de notas usando AlarmManager.
 */
object ReminderScheduler {

    private const val REQUEST_CODE_BASE = 10_000
    private const val TAG = "ReminderScheduler"

    private fun buildPendingIntent(context: Context, noteId: Int, isPersistent: Boolean): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("noteId", noteId)
            putExtra("isPersistent", isPersistent)
        }
        val requestCode = REQUEST_CODE_BASE + noteId
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun scheduleReminder(context: Context, noteId: Int, triggerAtMillis: Long, isPersistent: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = buildPendingIntent(context, noteId, isPersistent)

        val delayMs = triggerAtMillis - System.currentTimeMillis()
        Log.d(TAG, "Programando recordatorio para nota=$noteId en ${delayMs} ms")

        try {
            when {
                // Android 12-14: intentar usar setExact si el permiso está disponible
                Build.VERSION.SDK_INT in Build.VERSION_CODES.S..Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
                }
                else -> {
                    // Android 15+ (y anteriores a 12): usar alarma inexacta compatible
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
                }
            }
        } catch (e: SecurityException) {
            // Si el sistema bloquea setExact por permisos de alarmas exactas, hacemos fallback silencioso a inexacta
            Log.w(TAG, "Fallo al programar alarma exacta, usando setAndAllowWhileIdle: ${e.message}")
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    fun cancelReminder(context: Context, noteId: Int, isPersistent: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = buildPendingIntent(context, noteId, isPersistent)
        alarmManager.cancel(pi)
        Log.d(TAG, "Cancelado recordatorio para nota=$noteId")
    }
}
